# Exception Handling

이 문서는 출석체크 기능의 try-catch 경계와 외부 point API 실패 처리 기준을 정의한다. 현재 기준은 로컬 DB 트랜잭션과 외부 point API 호출을 분리하는 구조다.

## 목적

- 어떤 예외가 로컬 트랜잭션 rollback 대상인지 고정한다.
- 외부 point API 실패가 사용자 응답이 아니라 운영 대응 이슈가 되도록 기준을 맞춘다.
- 동기 호출 구조를 유지하되 transaction 경계는 명확히 분리한다.

## 현재 처리 원칙

1. 출석 성공 기준은 `event_applicant`, `event_entry`, `event_win` 로컬 커밋 성공이다.
2. 보상 매핑이 있으면 `event_win`까지 저장한 뒤 로컬 트랜잭션을 종료한다.
3. 외부 point API는 로컬 트랜잭션 커밋 후 호출한다.
4. 외부 point API 실패나 타임아웃은 로컬 rollback 사유가 아니다.
5. 외부 point API 실패는 `ERROR` 로그와 운영 알림으로 처리한다.
6. 중복 출석, 이벤트 상태 오류, 회차 정합성 오류는 로컬 저장 전에 비즈니스 예외로 종료한다.

## 예외 분류

| 분류 | 예시 | 로컬 rollback 대상 | 프론트 응답 방향 |
| --- | --- | --- | --- |
| Validation 예외 | `X-Member-Id` 누락, 타입 오류 | 없음 | `INVALID_REQUEST` |
| Business 예외 | 이벤트 없음, 회차 없음, 이벤트-회차 불일치, 중복 출석 | 현재 트랜잭션 내 로컬 변경 | 해당 domain code (`EVENT_NOT_FOUND`, `ROUND_EVENT_MISMATCH`, `ENTRY_ALREADY_APPLIED` 등) |
| Persistence 예외 | applicant insert unique 충돌, DB 저장 실패 | 현재 트랜잭션 내 로컬 변경 | `CONFLICT` 또는 `INTERNAL_ERROR` |
| External 실패 | point API 실패 응답 | 없음 | 출석 성공 유지, 운영 대응 |
| External timeout | point API 무응답, 타임아웃 | 없음 | 출석 성공 유지, 운영 대응 |
| Unexpected 예외 | point 호출 단계의 예상치 못한 런타임 예외 | 없음 | 출석 성공 유지, 운영 대응 |

## try-catch 경계

### Controller

- Controller는 넓은 `try-catch`를 두지 않는다.
- 비즈니스 예외와 validation 예외는 전역 예외 처리기에서 HTTP 응답으로 변환한다.

### Transaction Service

- 로컬 저장 단계는 `@Transactional` 경계 안에서 처리한다.
- 이벤트/회차 검증, applicant insert, reward 조회, `event_entry`, `event_win` 저장을 담당한다.
- 이 단계에서 발생한 예외만 rollback 대상으로 본다.

### Orchestrator Service

- 로컬 트랜잭션이 끝난 뒤 외부 point API를 호출한다.
- point API 예외는 catch 후 로그와 운영 알림으로 전환한다.
- 외부 API 예외를 다시 던져 사용자 응답을 실패로 바꾸지 않는다.

### Repository / Client

- Repository는 예외를 잡지 않는다.
- point API client는 HTTP/네트워크 예외를 기술 예외로 올린다.

## 권장 흐름

```text
1. 요청 검증
2. 이벤트/회차 조회
3. round.event_id == event.id 검증
4. event_applicant insert 시도
5. unique 충돌이면 이미 출석으로 종료
6. reward mapping 조회
7. event_entry 저장
8. reward가 있으면 event_win 저장
9. 로컬 트랜잭션 커밋
10. point API 호출
11. 실패 시 로그 + 운영 알림
12. 성공 응답 반환
```

## rollback 기준

### 반드시 rollback

- `event_applicant` 저장 실패
- `event_entry` 저장 실패
- `event_win` 저장 실패
- 이벤트/회차 정합성 오류
- 중복 출석 판정 전후의 로컬 Runtime 예외

### rollback 하지 않음

- 외부 point API 실패 응답
- 외부 point API 타임아웃 또는 무응답
- point API 호출 단계의 예기치 않은 Runtime 예외

## 외부 point API 타임아웃 정책

- `connection timeout = 1초`
- `read timeout = 2초`
- `총 대기 시간 = 최대 3초`
- 타임아웃은 외부 시스템 장애로 간주한다.
- 타임아웃이 발생해도 로컬 출석 데이터는 유지한다.
- 타임아웃은 운영 알림과 재처리 대상으로 남긴다.
- 재호출 시 `idempotency_key = event_id + round_id + member_id`를 그대로 사용한다.

## 예외별 응답 가이드

| 예외 | 내부 의미 | 사용자 응답 방향 |
| --- | --- | --- |
| Validation 예외 | 잘못된 요청 | `INVALID_REQUEST` |
| `ENTRY_ALREADY_APPLIED` | 같은 `event_id + round_id + member_id` 기준 이미 출석 완료 | `code = ENTRY_ALREADY_APPLIED`, `message = 이미 출석했습니다.` |
| 이벤트/회차 관련 예외 | 출석 불가 상태 | `EVENT_NOT_FOUND`, `EVENT_ROUND_NOT_FOUND`, `ROUND_EVENT_MISMATCH`, `EVENT_NOT_ACTIVE`, `EVENT_NOT_STARTED`, `EVENT_EXPIRED` |
| point API 실패/타임아웃 | 외부 지급 후처리 실패 | 출석 성공 유지, 운영 대응 |

- Business 예외는 `commonCode`가 아니라 domain code 자체를 응답 body의 `code`에 사용한다.
- 고객 메시지는 안내형 표현을 사용한다. `EVENT_NOT_ACTIVE`는 `현재 참여가 잠시 중단되었어요.`, `EVENT_NOT_STARTED`는 `이벤트 오픈 전이에요. 조금만 기다려 주세요.`, `EVENT_EXPIRED`는 `이 이벤트는 참여가 마감되었어요.`를 반환한다.

## 로깅 원칙

- 필수 식별값: `eventId`, `roundId`, `memberId`
- point API 실패 로그에는 `idempotency_key`를 반드시 포함한다.
- timeout 로그에는 `commonCode=INTERNAL_ERROR`, `domainCode=POINT_API_TIMEOUT`를 함께 남긴다.
- point API 실패 로그에는 `operationsAlertRequired=true`를 포함한다.
- 민감한 개인정보나 전체 payload는 로그에 남기지 않는다.

## 운영 대응 메모

- point API 실패 후 `event_win`은 이미 저장된 상태일 수 있다.
- 따라서 `event_win`은 외부 지급 성공 이력이라기보다 로컬 보상 확정 이력으로 해석해야 한다.
- 운영 재처리나 수동 재호출 시에는 같은 `idempotency_key`를 사용해야 한다.
- 재처리 설계가 필요해지면 별도 queue 또는 배치 복구 정책으로 확장한다.
