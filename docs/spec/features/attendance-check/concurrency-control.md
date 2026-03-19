# Concurrency Control

이 문서는 출석체크의 중복 출석 방지, 누적 출석 수 계산, 외부 point API 중복 지급 방지 전략을 정리한다. 현재 기준은 `event_applicant` unique, `event_applicant` 집합 잠금, 외부 point 시스템의 `idempotency_key`를 함께 사용하는 구조다.

## 목적

- 같은 회차에 대한 중복 출석을 한 번만 성공시키게 한다.
- 응답에 포함되는 누적 출석 수를 동시성 환경에서도 일관되게 계산한다.
- point API 재호출 시에도 중복 지급을 막는다.
- 로컬 트랜잭션과 외부 호출이 분리된 구조에서 각 계층의 책임을 고정한다.

## 기본 원칙

- `event_applicant`에는 `UNIQUE (round_id, member_id)`가 반드시 있어야 한다.
- 출석 중복 판정의 최종 기준은 `event_applicant` insert 결과다.
- 같은 `event_id + member_id`의 applicant row 집합을 `FOR UPDATE`로 잠가 누적 출석 수 계산 구간을 직렬화한다.
- 집합 잠금은 항상 같은 조건과 같은 정렬 순서로 조회해야 한다.
- `event_entry`는 응모권 이력이므로 출석 중복 제어용 unique를 두지 않는다.
- point 지급 중복 방지는 외부 point 시스템의 `idempotency_key`로 처리한다.

## 출석 중복 제어

### 1. Database first

- Service는 `event_applicant` insert를 시도한다.
- insert가 성공하면 같은 회차 출석이 처음이라는 뜻이다.
- insert가 `uq_event_applicant_round_member_id`에 걸리면 중복 출석으로 변환한다.

```sql
CREATE UNIQUE INDEX uq_event_applicant_round_member_id
    ON promotion.event_applicant (round_id, member_id)
    WHERE is_deleted = FALSE;
```

### 2. Application handling

- applicant insert unique 충돌은 `이미 출석했습니다` 비즈니스 오류로 변환한다.
- 동시 요청 2건 이상이 들어와도 최종적으로 한 건만 성공해야 한다.
- soft delete된 applicant는 현재 중복 판정 대상에서 제외한다.

## 누적 출석 수 계산

### 1. 기존 `count + 1` 방식의 문제

- `event_entry COUNT(*) + 1`은 현재 트랜잭션 밖의 동시 요청을 반영하지 못하는 추정값이다.
- 요청이 동시에 들어오면 서로 같은 count를 읽고 같은 응답값을 반환할 수 있다.
- `COUNT(*)`는 데이터가 늘수록 비용이 커지고, 응답용 누적 수 계산에 반복적으로 사용하기엔 비효율적이다.

### 2. 현재 권장 방식

- 같은 `event_id + member_id`의 applicant 전체 row를 `PESSIMISTIC_WRITE`로 조회한다.
- 같은 회원/이벤트 요청은 항상 동일한 row 집합을 잠그므로 이후 요청은 앞선 트랜잭션이 끝날 때까지 대기한다.
- 잠금이 끝난 뒤 현재 row 개수에 `+1` 해서 이번 응답의 누적 출석 수를 계산한다.
- 조회는 항상 `ORDER BY id ASC` 같은 동일 순서로 수행해 deadlock 가능성을 줄인다.

```java
private static final long ATTENDANCE_REQUEST_INCREMENT = 1L;

List<EventApplicantEntity> applicants = eventApplicantRepository.findByEventIdAndMemberIdForUpdate(eventId, memberId);
long attendedDays = applicants.size() + ATTENDANCE_REQUEST_INCREMENT;

EventApplicantEntity applicant = eventApplicantRepository.save(
        EventApplicantEntity.create(eventId, roundId, memberId, memberId)
);
```

### 3. 설계 메모

- `event_applicant`는 여전히 회차별 row다.
- 누적 카운트 계산 시에는 같은 이벤트/회원의 applicant 집합 전체가 직렬화 기준점 역할을 맡는다.
- 응답 누적값은 `event_entry count`가 아니라 잠금된 applicant 집합 크기 기준으로 계산한다.
- 출석 수와 전체 회차 수처럼 count 성격의 값은 `int`로 줄이지 않고 `long`으로 유지한다.
- 현재 구조에서는 집합 잠금 뒤 `ATTENDANCE_REQUEST_INCREMENT`를 더해 이번 요청 반영 출석 수를 계산하고, 저장은 별도의 applicant insert로 이어진다.

## Point API idempotency

- point 시스템에는 `UNIQUE (idempotency_key)`가 있어야 한다.
- 현재 출석체크의 `idempotency_key`는 아래 업무 키를 사용한다.

```text
idempotency_key = event_id + round_id + member_id
```

- 같은 출석 건에 대해 point API를 다시 호출하더라도 point 시스템은 중복 지급 없이 처리해야 한다.
- 이 키는 자동 재시도뿐 아니라 운영 재처리, 수동 재호출에도 동일하게 사용한다.

## 시나리오별 기대 결과

### 1. concurrent request

- 같은 출석 요청이 동시에 2번 들어온다.
- 둘 다 applicant insert를 시도할 수 있다.
- 같은 회차 중복은 `uq_event_applicant_round_member_id`가 한 건만 허용한다.
- 이미 출석 이력이 있는 회원이라면 같은 applicant 집합 lock 때문에 요청이 순차적으로 진행된다.

### 2. point retry

- 운영 재처리나 수동 재호출로 point API가 다시 호출된다.
- point 시스템의 `idempotency_key`가 같은 요청을 중복 지급 없이 흡수한다.

### 3. client retry

- 프론트가 같은 출석 요청을 다시 보낸다.
- `event_applicant (round_id, member_id)` unique가 중복 출석을 막는다.

### 4. point failure

- 로컬 트랜잭션은 이미 커밋되었다.
- point API는 타임아웃을 포함한 외부 API 실패로 끝났다.
- 로컬 데이터는 유지되고, 운영 재처리 시 같은 `idempotency_key`를 사용한다.

## 구현 예시

```java
private static final long ATTENDANCE_REQUEST_INCREMENT = 1L;

try {
    List<EventApplicantEntity> applicants = eventApplicantRepository.findByEventIdAndMemberIdForUpdate(eventId, memberId);
    long attendedDays = applicants.size() + ATTENDANCE_REQUEST_INCREMENT;
    EventApplicantEntity applicant = eventApplicantRepository.save(EventApplicantEntity.create(eventId, roundId, memberId, memberId));
    saveEntryAndWin(applicant);
} catch (DataIntegrityViolationException ex) {
    throw alreadyApplied();
}

commit();
publishPointRewardEvent(pointGrantCommand);
```

## 현재 권장 흐름

1. `event` 조회
2. `round` 조회
3. `round.event_id == event.id` 검증
4. 같은 `event_id + member_id`의 applicant 집합을 `FOR UPDATE`로 조회
5. 잠금된 applicant 개수 기준으로 `attendedDays = size + ATTENDANCE_REQUEST_INCREMENT` 계산
6. `event_applicant` insert 시도
7. unique 충돌이면 중복 출석으로 종료
8. `prize mapping` 확인
9. `event_entry`, `event_win` 저장
10. 로컬 트랜잭션 커밋
11. point API 호출
12. 실패 시 로그 + 운영 알림

## 관련 문서

- `service-validation.md`
- `exception-handling.md`
- `test-scenarios.md`
- `event-platform-schema-draft.sql`
