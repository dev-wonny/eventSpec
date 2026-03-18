# Service Validation

이 문서는 출석체크 구현 시 Service 레이어에서 반드시 검증해야 하는 규칙을 정리한다. FK 없이 운영하고 최소 unique만 두는 전제를 기준으로, API 요청 시점에 판단해야 하는 업무 규칙을 중심으로 작성한다.

## 목적

- Service에서 어떤 순서로 무엇을 검증해야 하는지 고정한다.
- DB 제약만으로 막히지 않는 업무 규칙을 애플리케이션에서 보완한다.
- 출석체크와 랜덤 리워드가 같은 테이블을 공유하더라도 출석 전용 규칙을 명확히 분리한다.

## 검증 원칙

- 출석 요청은 `POST /event/v1/events/{eventId}/rounds/{roundId}/entries` 진입 시 Service에서 검증한다.
- FK 없이 운영하므로 참조 정합성은 Service에서 명시적으로 검증한다.
- DB에는 최소 unique만 두고, 업무 규칙은 Service가 우선 보장한다.
- 출석체크 전용 규칙과 공용 이벤트 규칙을 구분한다.
- 검증 실패는 저장 전에 종료하는 것을 기본으로 한다.

## 검증 순서

1. 요청 필수값 검증
2. 이벤트 존재 및 상태 검증
3. 회차 존재 및 이벤트-회차 정합성 검증
4. `event_applicant` 기준 생성 가능 여부 검증
5. 출석 회차 보상 매핑 검증
6. `event_applicant` 저장
7. `event_entry` 저장
8. 외부 point API 호출 및 결과 처리
9. `event_win` 저장 및 커밋

## 규칙

### ATT-SVC-001 요청 필수값 검증

- `eventId`, `roundId`, `X-Member-Id`는 출석 요청에서 필수다.
- `X-Member-Id` 누락이나 타입 오류는 controller `@RequestHeader` 바인딩과 전역 예외 처리기에서 `INVALID_REQUEST`로 종료한다.
- Service는 필수 요청값이 정상 바인딩되었다는 전제에서 비즈니스 검증을 수행한다.

### ATT-SVC-002 이벤트 상태 검증

- `event`가 존재해야 한다.
- `event.event_type = 'ATTENDANCE'`여야 한다.
- `is_active = TRUE`, `is_deleted = FALSE`를 만족해야 한다.
- 현재 시각이 `event.start_at ~ event.end_at` 범위 안에 있어야 한다.

### ATT-SVC-003 회차 상태 및 정합성 검증

- `event_round`가 존재해야 한다.
- 요청 `roundId`의 `event_id`가 요청 `eventId`와 일치해야 한다.
- `event_round`는 `event_id`에 대한 FK가 아니므로, `round` 조회 시 `id`와 `event_id`를 함께 조건으로 사용해야 한다.
- 회차 시간 정책을 사용하면 `round_start_at ~ round_end_at`도 함께 검증한다.

```sql
SELECT *
FROM event_round
WHERE id = :roundId
  AND event_id = :eventId;
```

- 위 조회에서 결과가 없으면 `ROUND_EVENT_MISMATCH` 또는 회차 없음으로 처리한다.

```java
if (!round.getEventId().equals(eventId)) {
    throw new IllegalArgumentException("ROUND_EVENT_MISMATCH");
}
```

### ATT-SVC-004 applicant 기준 생성 가능 여부 검증

- `event_applicant`는 `(event_id, round_id, member_id)` 기준 회차별 applicant 테이블로 사용한다.
- 이 조회는 `is_deleted = FALSE`인 현재 유효 레코드만 대상으로 한다.
- 같은 키의 applicant가 이미 있으면 중복 출석으로 종료한다.
- `event_applicant.round_id`는 `NULL`이면 안 되고 요청 `roundId`와 같아야 한다.
- `event_applicant.event_id`가 요청 `eventId`와 일치해야 한다.
- applicant 생성 성공 후에만 실제 `event_entry`를 저장한다.

### ATT-SVC-005 중복 출석 검증

- 중복 출석 기준은 `event_applicant`의 `event_id + round_id + member_id`다.
- 같은 키의 유효한 `event_applicant`가 이미 있으면 `이미 출석했습니다`로 종료한다.
- 동시 요청 상황에서는 `uq_event_applicant_event_round_member` unique 충돌도 함께 처리해야 한다.
- `event_entry`는 응모권 테이블이므로 중복 출석 제어용 unique를 갖지 않는다.

### ATT-SVC-006 출석 회차 보상 매핑 검증

- 출석체크는 회차당 active `event_round_prize`가 `0..1`개여야 한다.
- active 보상 매핑이 2개 이상이면 운영/설정 오류로 종료한다.
- 보상 매핑이 1개면 연결된 `prize.reward_type = 'POINT'`인지 검증한다.
- 보상 매핑이 0개면 무보상 출석으로 처리한다.

### ATT-SVC-007 외부 point API 호출 결정

- 보상 매핑이 있는 경우에만 외부 point API를 호출한다.
- 보상 매핑이 없는 경우 외부 point API를 호출하지 않는다.
- 출석체크형 이벤트는 `event_applicant` 저장 후 `event_entry`를 저장한다.
- 보상 매핑이 있는 경우 출석체크형 이벤트의 `event_entry.is_winner`는 저장 시점에 `true`다.
- 보상 매핑이 없는 경우 `event_entry`만 저장하고 `event_win`은 생성하지 않는다.

### ATT-SVC-008 외부 연동 성공 후 저장 검증

- 외부 point API가 성공하면 `event_win`을 저장하고 최종 커밋해야 한다.
- 외부 point API가 실패하거나 무응답이면 `event_applicant`, `event_entry`, `event_win`은 모두 롤백해야 한다.
- 외부 point API가 호출되지 않은 무보상 출석은 `event_entry`만 저장한다.
- 외부 point API client는 `connection timeout = 1초`, `read timeout = 2초`, `총 대기 시간 = 최대 3초`를 사용해야 한다.
- 외부 point API 타임아웃은 외부 시스템 장애로 간주하고, 출석 자체를 실패 처리해야 한다.
- 타임아웃 응답의 공통 응답 코드는 `INTERNAL_ERROR`를 사용하고, 사용자 메시지는 `일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.`를 사용한다.
- 타임아웃 발생 시 Service는 `requestId`, `commonCode=INTERNAL_ERROR`, `domainCode=POINT_API_TIMEOUT`, `eventId`, `roundId`, `memberId`를 구조화 로그로 남겨야 한다.
- 외부 point API 호출 시 `idempotency_key = event_id + round_id + member_id`를 함께 전달해야 한다.
- 같은 출석 요청의 point API retry는 외부 point 시스템의 `UNIQUE(idempotency_key)`로 중복 지급을 막아야 한다.
- 현재 출석체크에서는 `date`, `rewardId`를 별도 멱등 키 구성값으로 추가하지 않고 `event_id + round_id + member_id`만 사용한다.
- 외부 point API 성공 후 DB 커밋이 실패하면 point 보정 차감은 수행하지 않는다.
- 이후 재시도 시 같은 `idempotency_key`로 외부 point API를 다시 호출하고, 중복 지급 없이 local `event_entry`, `event_win`을 복구하는 것을 기본 정책으로 한다.

### ATT-SVC-009 조회 API 상태 계산 검증

- `GET /events/{eventId}`에서 회원 기준 상태 계산 시 `event_entry` 존재 여부로 `ATTENDED / MISSED / TODAY / FUTURE`를 판정한다.
- 출석은 완료됐지만 보상 매핑이 없었던 회차는 `status = ATTENDED`, `win = null`이다.
- 지급된 보상이 있을 때만 `event_win` 정보를 `rounds[].win`에 연결한다.

## DB만으로 보장되지 않는 항목

- `round.event_id == event.id`
- `event_applicant.event_id == event.id`
- `event_applicant.round_id`가 비어 있지 않고 같은 `event_id`의 회차를 가리키는지
- `event_win.entry_id == event_entry.id`
- 출석체크 회차당 active `event_round_prize = 0..1`
- 출석 회차 보상은 `POINT`만 허용
- `event_applicant` 생성 가능 여부에 따른 출석 가능 판정
- 무보상 출석 시 외부 API 생략 및 `event_win` 미생성

## 구현 메모

- 최소 unique는 `uq_event_round_event_round_no`, `uq_event_applicant_event_round_member`, `uq_event_win_entry_id`만 유지한다.
- 출석체크와 랜덤 리워드는 `event_round_prize`를 공유하므로, 출석 전용 제약은 Service에서 `event_type = ATTENDANCE`일 때만 적용한다.
- 관리 API가 아직 없으므로 운영/SQL 적재 데이터의 이상 여부를 Service가 한 번 더 방어해야 한다.
- 동시성 제어는 조회만으로 끝내지 말고 락, 최소 unique 충돌 처리, 재검증 전략을 함께 고려한다.
- 자세한 동시성 전략은 `concurrency-control.md`를 기준으로 구현한다.
