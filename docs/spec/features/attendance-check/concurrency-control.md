# Concurrency Control

이 문서는 출석체크의 중복 출석 방지와 외부 point API 중복 지급 방지 전략을 정리한다. 현재 설계는 FK 없이 운영하므로, 중복 제어는 Application 검증, DB unique, 외부 idempotency를 함께 사용한다.

## 목적

- 동시 요청이 들어와도 출석은 한 번만 성공하게 한다.
- point API 재시도나 네트워크 타임아웃 상황에서도 보상이 중복 지급되지 않게 한다.
- 구현 시 어느 계층이 무엇을 막는지 분리해서 고정한다.

## 기본 원칙

- `event_entry`에는 `UNIQUE (event_id, round_id, member_id)`가 반드시 있어야 한다.
- 출석 중복 방지는 Application 검증과 DB unique 두 계층에서 처리한다.
- point 지급 중복 방지는 외부 point 시스템의 `idempotency_key`로 처리한다.
- 현재 point API의 `idempotency_key`는 `event_id + round_id + member_id` 조합을 사용한다.

## 두 계층 제어

### 1. Application validation

- Service는 출석 저장 전에 `(event_id, round_id, member_id)` 기준으로 기존 `event_entry`를 조회한다.
- 이미 유효한 `event_entry`가 있으면 `이미 출석했습니다`로 종료한다.
- 이 검증은 사용자 경험과 빠른 중복 응답을 위한 1차 방어선이다.

### 2. Database constraint

- `event_entry`에는 아래 unique가 반드시 있어야 한다.

```sql
CREATE UNIQUE INDEX uq_event_entry_event_round_member
    ON event.event_entry (event_id, round_id, member_id)
    WHERE is_deleted = FALSE;
```

- 이 unique는 동시 요청에서 최종적으로 한 건만 성공시키는 2차 방어선이다.
- Application 조회를 통과한 뒤라도 동시성 상황에서는 DB unique가 최종 충돌을 감지해야 한다.
- unique가 `WHERE is_deleted = FALSE` 조건을 가지므로, soft delete된 과거 레코드는 현재 중복 판정 대상에 포함되지 않는다.

## Point API idempotency

- point 시스템에는 `UNIQUE (idempotency_key)`가 있어야 한다.
- 현재 출석체크의 `idempotency_key`는 아래 업무 키를 사용한다.

```text
idempotency_key = event_id + round_id + member_id
```

- 같은 출석 요청이 재시도되더라도 point 시스템은 같은 `idempotency_key`를 같은 지급 요청으로 간주해야 한다.
- 따라서 event 서버가 point API를 재호출하더라도 중복 point 지급은 발생하지 않아야 한다.

## 계층별 역할 정리

### 출석 시스템

```text
UNIQUE(event_id, round_id, member_id)
```

- 출석 중복 방지
- 프론트 재요청 방지
- 동시 출석 요청 충돌 방지

### Point 시스템

```text
UNIQUE(idempotency_key)
```

- 보상 중복 지급 방지
- 네트워크 타임아웃 후 재시도 방지
- 외부 API retry 방지

## 시나리오별 기대 결과

### 1. concurrent request

- 같은 출석 요청이 동시에 2번 들어온다.
- Application 검증은 둘 다 통과할 수 있다.
- 최종적으로는 `uq_event_entry_event_round_member`가 한 건만 허용한다.

### 2. retry

- point API가 재시도된다.
- point 시스템의 `idempotency_key`가 같은 요청을 중복 지급 없이 흡수한다.

### 3. client retry

- 프론트가 같은 출석 요청을 다시 보낸다.
- `event_entry` unique가 중복 출석을 막는다.

### 4. network timeout

- point 지급은 이미 성공했다.
- event 서버는 타임아웃 또는 실패로 인식했다.
- 이후 재시도 시 point 시스템은 같은 `idempotency_key`로 중복 지급을 막는다.

### 5. point success + DB commit failure

- 외부 point API는 성공했다.
- event 서버의 DB 커밋이 실패했다.
- 이 경우 point 보정 차감은 하지 않는다.
- 사용자가 재시도하면 같은 `idempotency_key`로 point API를 다시 호출한다.
- point 시스템은 이미 처리된 요청으로 보고 중복 지급 없이 응답한다.
- event 서버는 그 재시도 시점에 local `event_entry`, `event_win`을 복구한다.

## DB 충돌 동작

- 트랜잭션이 commit 되기 전이라도 DB는 unique 충돌을 관리한다.

```text
A insert
-> 성공

B insert
-> 대기

A commit

B
-> duplicate key error
```

- 즉 commit 전에라도 DB가 충돌을 감지하고 최종적으로 하나만 성공시킨다.

## 구현 예시

```java
try {
    insertEntry();
} catch (DuplicateKeyException e) {
    return "이미 출석했습니다";
}

callPointApi(idempotencyKey);
```

```text
INSERT event_entry
   UNIQUE(event_id, round_id, member_id)
        ↓
성공 -> 출석 계속 진행
실패 -> 이미 출석
```

## 현재 권장 흐름

1. `event` 조회
2. `round` 조회
3. `round.event_id == event.id` 검증
4. `applicant eligibility` 확인
5. `prize mapping` 확인
6. `event_entry` insert 시도
7. 성공 시 point API 호출
8. point API 성공 시 `event_win` 저장 및 커밋
9. point API 실패 시 전체 rollback

DB 커밋 실패가 발생하면 보정 차감보다 `idempotency_key` 기반 local recovery를 우선한다.

## 관련 문서

- `service-validation.md`
- `exception-handling.md`
- `test-scenarios.md`
- `event-platform-schema-draft.sql`
