# Concurrency Control

이 문서는 출석체크의 중복 출석 방지와 외부 point API 중복 지급 방지 전략을 정리한다. 현재 기준은 `event_applicant` unique와 외부 point 시스템의 `idempotency_key`를 함께 사용하는 구조다.

## 목적

- 같은 회차에 대한 중복 출석을 한 번만 성공시키게 한다.
- point API 재호출 시에도 중복 지급을 막는다.
- 로컬 트랜잭션과 외부 호출이 분리된 구조에서 각 계층의 책임을 고정한다.

## 기본 원칙

- `event_applicant`에는 `UNIQUE (round_id, member_id)`가 반드시 있어야 한다.
- 출석 중복 판정의 최종 기준은 `event_applicant` insert 결과다.
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
- 둘 다 insert를 시도할 수 있다.
- 최종적으로는 `uq_event_applicant_round_member_id`가 한 건만 허용한다.

### 2. point retry

- 운영 재처리나 수동 재호출로 point API가 다시 호출된다.
- point 시스템의 `idempotency_key`가 같은 요청을 중복 지급 없이 흡수한다.

### 3. client retry

- 프론트가 같은 출석 요청을 다시 보낸다.
- `event_applicant (round_id, member_id)` unique가 중복 출석을 막는다.

### 4. point timeout

- 로컬 트랜잭션은 이미 커밋되었다.
- point API는 타임아웃 또는 실패로 끝났다.
- 로컬 데이터는 유지되고, 운영 재처리 시 같은 `idempotency_key`를 사용한다.

## 구현 예시

```java
try {
    eventApplicantRepository.save(applicant);
} catch (DataIntegrityViolationException ex) {
    throw alreadyApplied();
}

saveEntryAndWin();
commit();
callPointApi(idempotencyKey);
```

## 현재 권장 흐름

1. `event` 조회
2. `round` 조회
3. `round.event_id == event.id` 검증
4. `event_applicant` insert 시도
5. unique 충돌이면 중복 출석으로 종료
6. `prize mapping` 확인
7. `event_entry`, `event_win` 저장
8. 로컬 트랜잭션 커밋
9. point API 호출
10. 실패 시 로그 + 운영 알림

## 관련 문서

- `service-validation.md`
- `exception-handling.md`
- `test-scenarios.md`
- `event-platform-schema-draft.sql`
