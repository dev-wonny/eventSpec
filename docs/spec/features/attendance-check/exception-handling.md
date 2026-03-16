# Exception Handling

이 문서는 출석체크 기능의 try-catch 경계, 외부 API 예외 처리, 트랜잭션/rollback 기준을 정의한다. 현재 범위는 동기식 point 지급 API 연동을 전제로 한다.

## 목적

- 어디서 예외를 잡고 어디서 다시 던질지 정한다.
- 어떤 실패가 `event_entry`, `event_win` rollback으로 이어지는지 고정한다.
- 프론트에 어떤 종류의 실패를 노출할지 일관되게 관리한다.
- 향후 AWS 기반 비동기 구조로 전환할 때 변경 포인트를 분리한다.
- FK 없이 운영하는 대신 Service 검증과 최소 unique가 어떻게 보완하는지도 함께 정리한다.

## 현재 처리 원칙

1. 출석 회차에 보상 매핑이 있으면 출석 성공은 외부 point API 성공까지 포함한다.
2. 보상 매핑이 있는 출석은 `event_entry` 저장 후 외부 point API를 호출하고, 성공 시 `event_win`을 저장한다.
3. 출석 회차에 보상 매핑이 없으면 외부 point API를 호출하지 않는다.
4. 외부 point API 실패 또는 무응답이면 출석은 실패다.
5. 출석 실패 시 `event_entry`, `event_win`은 남지 않아야 한다.
6. 예외를 catch한 뒤 삼키지 말고 도메인 예외로 변환해서 다시 던진다.
7. 현재는 동기식 구조이므로 프론트는 즉시 성공 또는 실패를 받는다.

## 예외 분류

| 분류 | 예시 | 외부 API 호출 여부 | rollback 대상 | 프론트 응답 방향 |
| --- | --- | --- | --- | --- |
| Validation 예외 | 이벤트 없음, 회차 없음, 이벤트-회차 불일치, `X-Member-Id` 누락 | 호출 안 함 | 현재 트랜잭션 내 로컬 변경 | 요청 오류/비즈니스 오류 |
| Business 예외 | 같은 `event_id + round_id + member_id` 중복 응모, 출석 불가 시간 | 호출 안 함 | 현재 트랜잭션 내 로컬 변경 | 요청 거절 |
| External 실패 | point API 실패 응답 | 보상 매핑이 있을 때만 호출 | `event_entry`, `event_win` | 출석 실패 |
| External timeout | point API 무응답, 타임아웃 | 보상 매핑이 있을 때만 호출 | `event_entry`, `event_win` | 현재 출석체크 불가 |
| Persistence 예외 | 최소 unique 충돌, DB 저장 실패 | 상황에 따라 다름 | 현재 트랜잭션 내 로컬 변경 | 서버 오류 또는 충돌 |
| Unexpected 예외 | NPE, 매핑 오류, 기타 런타임 예외 | 상황에 따라 다름 | 현재 트랜잭션 내 로컬 변경 | 서버 오류 |

## try-catch 경계

### Controller

- Controller에서는 넓은 `try-catch`를 두지 않는다.
- 서비스가 던진 도메인 예외를 전역 예외 처리기에서 HTTP 응답으로 변환한다.

### Service

- 출석 처리의 핵심 `try-catch` 경계는 Service에 둔다.
- 외부 point API 관련 예외는 Service에서 catch 후 출석 도메인 예외로 변환한다.
- DB 예외는 필요한 경우 의미 있는 도메인 예외로 감싼 뒤 다시 던진다.
- rollback이 필요한 예외는 반드시 Runtime 예외 계열로 전파한다.

### Repository / Client

- Repository는 원칙적으로 예외를 잡지 않는다.
- 외부 API Client는 HTTP/네트워크 예외를 기술 예외로 올리고, Service가 이를 도메인 의미로 변환한다.

## 권장 흐름

```text
1. 요청 검증
2. 이벤트/회차 정합성 확인
3. applicant eligibility 조회 및 검증
4. 중복 출석 확인
5. 출석 회차의 보상 매핑 확인
6. event_entry 저장
7. 보상 매핑이 있으면 외부 point API 호출
8. 성공 시 event_win 저장
9. 트랜잭션 커밋
```

외부 API 실패 또는 무응답이면 6~9 단계는 성공으로 확정되면 안 된다. 보상 매핑이 없으면 7과 8은 건너뛴다.

이 단계에서 가장 중요한 검증 중 하나는 FK가 없기 때문에 `round.event_id == event.id`를 직접 확인하는 것이다.

```sql
SELECT *
FROM event_round
WHERE id = :roundId
  AND event_id = :eventId;
```

```java
if (!round.getEventId().equals(event.getId())) {
    throw new IllegalArgumentException("ROUND_EVENT_MISMATCH");
}
```

## 권장 try-catch 구조

```java
@Transactional
public AttendanceResult attend(AttendanceCommand command) {
    validate(command);

    Event event = loadEvent(command);
    EventRound round = loadRound(command.roundId());
    ensureRoundMatchesEvent(round, event.getId());
    EventApplicant applicant = loadEligibleApplicant(command, event, round);
    ensureApplicantMatchesEvent(applicant, event.getId(), round.getId());
    Optional<EventRoundPrize> prize = loadAttendancePrize(round);

    ensureNoDuplicateAttendance(applicant, round);

    try {
        EventEntry entry = eventEntryRepository.save(...);

        if (prize.isEmpty()) {
            return AttendanceResult.success(entry, null, null);
        }

        PointGrantResult pointResult = pointApiClient.grantPoint(...);
        EventWin win = eventWinRepository.save(...);

        return AttendanceResult.success(entry, win, pointResult);
    } catch (PointApiTimeoutException e) {
        throw new AttendanceUnavailableException(e);
    } catch (PointApiFailException e) {
        throw new AttendanceRewardFailedException(e);
    } catch (DataIntegrityViolationException e) {
        throw new AttendanceConflictException(e);
    } catch (Exception e) {
        throw new AttendanceUnexpectedException(e);
    }
}
```

위 흐름의 기준은 `event_entry` 저장 후 외부 point API 호출, 성공 시 `event_win` 저장이다. 외부 API 실패나 무응답이 발생하면 트랜잭션 전체가 롤백되어 `event_entry`도 남지 않아야 한다.

## rollback 기준

### 반드시 rollback

- 외부 point API 실패 응답
- 외부 point API 타임아웃 또는 무응답
- `event_entry` 저장 실패
- `event_win` 저장 실패
- 중복 출석 판정 이후 발견된 정합성 오류
- 기타 Runtime 예외

### 현재 보장 대상

- `event_entry`
- `event_win` 생성이 있는 경우의 `event_win`

### 추가 확인 필요

- `event_applicant.round_id`가 있는 대상자와 없는 대상자가 함께 존재할 때 판정 우선순위
- 외부 API 성공 후 DB 커밋 실패 시 보상 보정 또는 재처리 방식

## 예외별 응답 가이드

| 예외 | 내부 의미 | 사용자 응답 방향 |
| --- | --- | --- |
| `AttendanceValidationException` | 잘못된 요청 | 잘못된 요청 |
| `AttendanceDuplicateException` | 같은 `event_id + round_id + member_id` 기준 이미 출석 완료 | 이미 출석했습니다 |
| `AttendanceUnavailableException` | 외부 API 무응답 또는 타임아웃 | 현재 출석체크를 진행할 수 없음 |
| `AttendanceRewardFailedException` | 외부 API 실패 응답 | 출석 처리 실패 |
| `AttendanceConflictException` | 동시성/DB 충돌 | 잠시 후 다시 시도 |
| `AttendanceUnexpectedException` | 예상치 못한 오류 | 일시적 오류 |

구체적인 HTTP status, 에러 코드, 메시지는 `api-spec.md`에서 확정한다.

## 로깅 원칙

- 요청 식별값: `eventId`, `roundId`, `memberId`
- 외부 API 결과: 응답 코드, 타임아웃 여부, 추적 가능한 요청 ID
- rollback 원인: 예외 타입, 실패 단계
- 민감한 개인정보나 보상 원문 payload 전체는 로그에 남기지 않는다.

## 현재 구조의 숨은 리스크

### 외부 API 성공 후 DB 실패

- 외부 point API는 이미 성공했는데 `event_entry` 또는 `event_win` 커밋이 실패할 수 있다.
- 이 경우 DB rollback만으로는 외부 지급을 되돌릴 수 없다.
- 따라서 보정 API, 재처리 작업, 운영 수동 대응 중 어떤 방식으로 정합성을 맞출지 별도 정책이 필요하다.

### 외부 API 재시도

- 타임아웃 후 실제 외부 시스템에서는 이미 point가 적립되었을 가능성이 있다.
- 현재 출석체크는 `idempotency_key = event_id + round_id + member_id`를 사용한다.
- 재시도 시 point 시스템의 `UNIQUE(idempotency_key)`가 중복 지급을 막아야 한다.

## 동시성 제어 연결

- 출석 중복은 Application 조회와 `uq_event_entry_event_round_member` unique 두 계층에서 막는다.
- point 중복 지급은 외부 point 시스템의 `idempotency_key`로 막는다.
- 자세한 흐름은 `concurrency-control.md`를 기준으로 구현한다.

## 향후 비동기 전환 메모

- AWS 기반 메시지 큐로 전환되면 "출석 성공"과 "point 지급 성공"의 시점이 분리될 수 있다.
- 그 시점에는 현재 문서의 동기식 rollback 규칙을 다시 작성해야 한다.
- 비동기 전환 후에는 `event_win` 생성 시점, 실패 재처리, 사용자 응답 정책이 함께 바뀐다.
