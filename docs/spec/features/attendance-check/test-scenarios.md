# Test Scenarios

이 문서는 출석체크 구현 완료 여부를 판단하는 테스트 기준이다. 현재는 DDL과 도메인 설명, 확정된 `event_entry.event_id` 유지 및 `applicant_id -> event_applicant.round_id` 회차 파생 규칙을 반영해 작성한다.

## 기능 시나리오

| ID | 시나리오 | 기대 결과 | 연결 규칙 |
| --- | --- | --- | --- |
| ATT-TEST-001 | 보상 매핑이 있는 회차의 첫 출석 요청 | `event_applicant`, `event_entry`, `event_win`이 먼저 커밋되고 외부 point API가 후행 호출된다 | ATT-RULE-002, ATT-RULE-003, ATT-RULE-004, ATT-RULE-009, ATT-RULE-012 |
| ATT-TEST-002 | 보상 매핑이 있는 다른 날짜 회차 재출석 요청 | 다른 회차의 `event_applicant`, `event_entry`, `event_win`이 새로 저장되고 커밋 후 외부 point API가 호출된다 | ATT-RULE-001, ATT-RULE-003, ATT-RULE-004, ATT-RULE-009, ATT-RULE-012 |
| ATT-TEST-003 | 같은 `round_id + member_id`로 재응모 요청 | `uq_event_applicant_round_member_id` 기준으로 중복 출석을 막고 추가 applicant가 생기지 않는다 | ATT-RULE-005 |
| ATT-TEST-004 | 존재하지 않는 이벤트 또는 회차로 요청 | `EVENT_NOT_FOUND` 또는 `EVENT_ROUND_NOT_FOUND`를 반환한다 | ATT-RULE-006 |
| ATT-TEST-005 | 이벤트와 회차가 서로 맞지 않는 요청 | `ROUND_EVENT_MISMATCH`를 반환한다 | ATT-RULE-006 |
| ATT-TEST-006 | 비활성/삭제/기간 외 이벤트 또는 회차 요청 | 출석 불가 상태에 맞는 domain code(`EVENT_NOT_ACTIVE`, `EVENT_NOT_STARTED`, `EVENT_EXPIRED`)와 안내형 메시지를 반환한다 | ATT-RULE-002 |
| ATT-TEST-007 | 동일 `round_id + member_id` 조건의 동시 출석 요청 2건 이상 발생 | Service 중복 검증과 `uq_event_applicant_round_member_id` unique에 의해 최종 유효 applicant는 한 건만 남고, 나머지는 `ENTRY_ALREADY_APPLIED`로 정리된다 | ATT-RULE-005, ATT-RULE-007 |
| ATT-TEST-008 | `GET /events/{eventId}`를 `X-Member-Id`와 함께 호출 | 각 회차의 `ATTENDED / MISSED / TODAY / FUTURE` 상태와 보상 이력이 일관되게 반환된다 | ATT-RULE-006, ATT-RULE-008, ATT-RULE-009, ATT-RULE-014 |
| ATT-TEST-009 | 같은 회원이 여러 날짜 회차에 출석 요청 | `event_applicant`는 `round_id + member_id` 기준으로 각 날짜마다 한 건씩 생성된다 | ATT-RULE-003, ATT-RULE-006 |
| ATT-TEST-010 | 월간 이벤트의 마지막 날짜 회차 출석 요청 | 올바른 `event_round`가 선택되고 출석이 저장된다 | ATT-RULE-001, ATT-RULE-002 |
| ATT-TEST-011 | 외부 point API 실패 | 로컬 `event_applicant`, `event_entry`, `event_win`은 유지되고 오류 로그와 운영 알림이 남는다 | ATT-RULE-009, ATT-RULE-012 |
| ATT-TEST-012 | 외부 point API 무응답 또는 타임아웃 | 타임아웃도 외부 API 실패로 동일하게 처리되며, 로컬 데이터는 유지되고 운영 알림 대상이 되며 사용자 응답은 출석 성공으로 유지된다 | ATT-RULE-012, ATT-RULE-013 |
| ATT-TEST-026 | point API retry 발생 | 같은 `idempotency_key = event_id + round_id + member_id`로 중복 point 지급이 발생하지 않는다 | ATT-RULE-012, ATT-RULE-013 |
| ATT-TEST-027 | point 지급 성공 후 event 서버 타임아웃 발생 뒤 재시도 | point 시스템의 `idempotency_key`가 중복 지급을 막는다 | ATT-RULE-012, ATT-RULE-013 |
| ATT-TEST-033 | 운영 재처리로 point API를 다시 호출하는 경우 | 같은 `idempotency_key = event_id + round_id + member_id` 재호출로 중복 지급이 발생하지 않는다 | ATT-RULE-012, ATT-RULE-013 |
| ATT-TEST-034 | 외부 point API timeout 설정 확인 | `connection timeout = 1초`, `read timeout = 2초`, `총 대기 시간 = 최대 3초` 기준으로 client 설정이 반영된다 | ATT-RULE-013, ATT-SVC-008 |
| ATT-TEST-013 | 출석 성공 시 지급 point 연결 확인 | `event_win.event_round_prize_id`가 회차의 point 보상 정책을 가리킨다 | ATT-RULE-009, ATT-RULE-010 |
| ATT-TEST-023 | 출석 회차에 보상 매핑이 없는 경우 | 외부 point API를 호출하지 않고 `event_entry`만 저장되며 응답 `win`은 `null`이다 | ATT-RULE-009, ATT-RULE-010, ATT-RULE-012 |
| ATT-TEST-024 | 출석 회차에 active `event_round_prize`가 2개 이상 설정된 경우 | 운영/검증 오류로 처리하고 출석을 진행하지 않는다 | ATT-RULE-009, ATT-RULE-010 |
| ATT-TEST-022 | applicant 저장 중 예외가 발생한 출석 요청 | applicant 단계에서 요청이 중단되고 `event_entry`, `event_win`이 저장되지 않는다 | ATT-RULE-003, ATT-RULE-006 |
| ATT-TEST-020 | `GET /events/{eventId}`를 `X-Member-Id` 없이 호출 | 전체 회차 기본 정보만 반환되고 `status = null`, `win = null`이다 | ATT-RULE-014 |
| ATT-TEST-021 | `POST /entries`를 `X-Member-Id` 없이 호출 | `INVALID_REQUEST`와 헤더 오류 메시지를 반환한다 | ATT-RULE-006 |
| ATT-TEST-028 | soft delete된 `event_applicant` 또는 `event_entry`가 있는 상태에서 같은 키로 다시 요청 | soft delete 레코드는 현재 유효값에서 제외되고, 동일 applicant 키로 재출석이 허용된다 | ATT-RULE-008, ATT-SVC-004, ATT-SVC-005 |
| ATT-TEST-035 | 추첨형 이벤트의 당첨 확정 처리 | 같은 회차에 여러 `event_entry`가 존재할 수 있고, 당첨된 응모권만 `is_winner = true`로 update된다 | ATT-RULE-004, ATT-RULE-009 |
| ATT-TEST-029 | `prize`와 `event_round_prize`를 함께 생성하는 도중 `event_round_prize` 저장 실패 | 두 레코드가 함께 롤백된다 | ATT-RULE-011 |
| ATT-TEST-030 | `event_round_prize`만 soft delete | `prize`는 유지되고 현재 활성 회차 보상 조회에서만 제외된다 | ATT-RULE-011 |
| ATT-TEST-031 | soft delete된 `prize`, `event_round_prize`가 과거 `event_win`과 연결된 상태 | 현재 활성 설정 조회에서는 제외되지만, 과거 지급 이력 조회와 집계에서는 참조 가능하다 | ATT-RULE-011 |
| ATT-TEST-032 | 랜덤 리워드에서 꽝 또는 미지급 발생 | `event_entry`만 남고 `event_win`은 생성되지 않는다 | ATT-RULE-009 |

## 비기능 시나리오

### ATT-TEST-014 감사 가능성

- `event_applicant`, `event_entry`, `event_win`에 최소 생성 시각과 추적 가능한 식별 정보가 남아야 한다.

### ATT-TEST-015 시간대 경계값

- 출석 시작/종료 시각 경계에서 정책대로 처리되어야 한다.

### ATT-TEST-016 응모권 상태 변경 가능성

- 추첨형 이벤트에서는 기존 `event_entry`를 유지한 채 `is_winner`만 update할 수 있어야 한다.

### ATT-TEST-017 출석 보상 point 연결

- 출석체크 회차에 보상 매핑이 있다면 `reward_type = 'POINT'`인 `prize`를 우선 사용해야 한다.

### ATT-TEST-025 랜덤 리워드 다중 보상 확장성

- 랜덤 리워드 회차에는 여러 `event_round_prize`와 각 확률 정책이 연결될 수 있어야 한다.

### ATT-TEST-018 prize 변경 대신 신규 생성 정책

- 이미 운영에 사용된 `prize`의 보상 정보를 수정하지 않고, 변경이 필요하면 새 `prize`를 생성해 `event_round_prize` 연결을 바꾸는 방식으로 처리되어야 한다.

### ATT-TEST-019 현재 동기식 외부 연동 제약

- 외부 point API 응답 지연 중에도 로컬 트랜잭션은 이미 커밋되어 있어야 하며, 실패 시 운영 보정 대상으로 남아야 한다.
- 외부 point API 타임아웃은 최대 3초 안에 종료되어야 하며, 로그와 알림은 일반 외부 API 실패 기준으로 남겨야 한다.

## 테스트 레벨 가이드

- 단위 테스트: 회차 판정, applicant 생성 판정, 중복 정책, 상태 계산
- 통합 테스트: API 요청/응답, 외부 point API 성공/실패, 로컬 커밋 유지 여부
- 통합 테스트: API 요청/응답, 동시 출석 요청, point API idempotency, 외부 point API 성공/실패, 운영 알림 처리
- 저장소 테스트: 회차별 applicant 조회, `round_id + member_id` 기준 applicant 중복 검증, 이벤트-회차 정합성 검증, 출석 회차의 단일 prize 매핑 조회, 지급 이력 조회

## 추가 확정 시 보강 항목

- 필수 요청 필드별 validation 케이스
- 실제 에러 코드와 메시지 매핑
- DDL에 최소 FK와 `uq_event_round_event_round_no`, `uq_event_applicant_round_member_id`, `uq_event_win_entry_id`가 반영되는지 테스트
- 외부 point API 타임아웃 및 재시도 정책 테스트
- point API client의 `connection timeout = 1초`, `read timeout = 2초`, `총 대기 시간 = 최대 3초` 설정 테스트
- `idempotency_key = event_id + round_id + member_id` 전달 및 중복 지급 방지 테스트
