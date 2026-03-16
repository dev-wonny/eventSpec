# Test Scenarios

이 문서는 출석체크 구현 완료 여부를 판단하는 테스트 기준이다. 현재는 DDL과 도메인 설명, 확정된 `event_entry.event_id`, `event_entry.round_id` 규칙을 반영해 작성한다.

## 기능 시나리오

| ID | 시나리오 | 기대 결과 | 연결 규칙 |
| --- | --- | --- | --- |
| ATT-TEST-001 | 보상 매핑이 있는 회차의 첫 출석 요청 | eligibility 검증 후 `event_entry` 저장, 외부 point API 성공, `event_win` 저장 순서로 커밋된다 | ATT-RULE-002, ATT-RULE-003, ATT-RULE-004, ATT-RULE-009, ATT-RULE-012 |
| ATT-TEST-002 | 보상 매핑이 있는 다른 날짜 회차 재출석 요청 | 같은 `event_applicant` eligibility를 기준으로 `event_entry` 저장 후 외부 point API 성공 시 `event_win`이 추가 저장된다 | ATT-RULE-001, ATT-RULE-003, ATT-RULE-004, ATT-RULE-009, ATT-RULE-012 |
| ATT-TEST-003 | 같은 `event_id + round_id + member_id`로 재응모 요청 | `이미 출석했습니다`를 반환하고 추가 `event_entry`가 생기지 않는다 | ATT-RULE-005 |
| ATT-TEST-004 | 존재하지 않는 이벤트 또는 회차로 요청 | 유효성 오류를 반환한다 | ATT-RULE-006 |
| ATT-TEST-005 | 이벤트와 회차가 서로 맞지 않는 요청 | 정합성 오류를 반환한다 | ATT-RULE-006 |
| ATT-TEST-006 | 비활성/삭제/기간 외 이벤트 또는 회차 요청 | 출석 불가 응답을 반환한다 | ATT-RULE-002 |
| ATT-TEST-007 | 동일 `event_id + round_id + member_id` 조건의 동시 출석 요청 2건 이상 발생 | Service 중복 검증과 `uq_event_entry_event_round_member` unique에 의해 최종 유효 출석은 한 건만 남고, 나머지는 이미 출석 오류로 정리된다 | ATT-RULE-005, ATT-RULE-007 |
| ATT-TEST-008 | `GET /events/{eventId}`를 `X-Member-Id`와 함께 호출 | 각 회차의 `ATTENDED / MISSED / TODAY / FUTURE` 상태와 보상 이력이 일관되게 반환된다 | ATT-RULE-006, ATT-RULE-008, ATT-RULE-009, ATT-RULE-014 |
| ATT-TEST-009 | `event_applicant.round_id`가 지정된 사용자의 출석 요청 | 요청 `roundId`가 일치할 때만 출석이 허용되고, 불일치하면 거절된다 | ATT-RULE-003, ATT-RULE-006 |
| ATT-TEST-010 | 월간 이벤트의 마지막 날짜 회차 출석 요청 | 올바른 `event_round`가 선택되고 출석이 저장된다 | ATT-RULE-001, ATT-RULE-002 |
| ATT-TEST-011 | 외부 point API 실패 | `event_entry`, `event_win`이 모두 롤백되고 출석 실패 응답을 반환한다 | ATT-RULE-009, ATT-RULE-012 |
| ATT-TEST-012 | 외부 point API 무응답 또는 타임아웃 | `event_entry`, `event_win`이 모두 롤백되고 프론트에 출석체크 불가 오류를 반환한다 | ATT-RULE-012, ATT-RULE-013 |
| ATT-TEST-026 | point API retry 발생 | 같은 `idempotency_key = event_id + round_id + member_id`로 중복 point 지급이 발생하지 않는다 | ATT-RULE-012, ATT-RULE-013 |
| ATT-TEST-027 | point 지급 성공 후 event 서버 타임아웃 발생 뒤 재시도 | point 시스템의 `idempotency_key`가 중복 지급을 막는다 | ATT-RULE-012, ATT-RULE-013 |
| ATT-TEST-013 | 출석 성공 시 지급 point 연결 확인 | `event_win.event_round_prize_id`가 회차의 point 보상 정책을 가리킨다 | ATT-RULE-009, ATT-RULE-010 |
| ATT-TEST-023 | 출석 회차에 보상 매핑이 없는 경우 | 외부 point API를 호출하지 않고 `event_entry`만 저장되며 응답 `win`은 `null`이다 | ATT-RULE-009, ATT-RULE-010, ATT-RULE-012 |
| ATT-TEST-024 | 출석 회차에 active `event_round_prize`가 2개 이상 설정된 경우 | 운영/검증 오류로 처리하고 출석을 진행하지 않는다 | ATT-RULE-009, ATT-RULE-010 |
| ATT-TEST-022 | 대상자 풀이 적용된 이벤트에서 applicant가 없는 사용자의 출석 요청 | eligibility 오류를 반환하고 `event_entry`, `event_win`이 저장되지 않는다 | ATT-RULE-003, ATT-RULE-006 |
| ATT-TEST-020 | `GET /events/{eventId}`를 `X-Member-Id` 없이 호출 | 전체 회차 기본 정보만 반환되고 `status = null`, `win = null`이다 | ATT-RULE-014 |
| ATT-TEST-021 | `POST /entries`를 `X-Member-Id` 없이 호출 | validation 오류를 반환한다 | ATT-RULE-006 |

## 비기능 시나리오

### ATT-TEST-014 감사 가능성

- `event_applicant`, `event_entry`, `event_win`에 최소 생성 시각과 추적 가능한 식별 정보가 남아야 한다.

### ATT-TEST-015 시간대 경계값

- 출석 시작/종료 시각 경계에서 정책대로 처리되어야 한다.

### ATT-TEST-016 append-only 보장

- 정상적인 재출석은 기존 `event_entry`를 수정하지 않고 새 레코드로 남아야 한다.

### ATT-TEST-017 출석 보상 point 연결

- 출석체크 회차에 보상 매핑이 있다면 `reward_type = 'POINT'`인 `prize`를 우선 사용해야 한다.

### ATT-TEST-025 랜덤 리워드 다중 보상 확장성

- 랜덤 리워드 회차에는 여러 `event_round_prize`와 각 확률 정책이 연결될 수 있어야 한다.

### ATT-TEST-018 prize 변경 대신 신규 생성 정책

- 이미 운영에 사용된 `prize`의 보상 정보를 수정하지 않고, 변경이 필요하면 새 `prize`를 생성해 `event_round_prize` 연결을 바꾸는 방식으로 처리되어야 한다.

### ATT-TEST-019 현재 동기식 외부 연동 제약

- 외부 point API 응답 지연 중에는 요청이 성공으로 확정되지 않아야 하며, 사용자에게는 실패/불가 상태가 반환되어야 한다.

## 테스트 레벨 가이드

- 단위 테스트: 회차 판정, eligibility 판정, 중복 정책, 상태 계산
- 통합 테스트: API 요청/응답, 외부 point API 성공/실패, 트랜잭션 롤백
- 통합 테스트: API 요청/응답, 동시 출석 요청, point API idempotency, 외부 point API 성공/실패, 트랜잭션 롤백
- 저장소 테스트: eligibility applicant 조회, `event_id + round_id + member_id` 기준 출석 이력 조회, 이벤트-회차 정합성 검증, 출석 회차의 단일 prize 매핑 조회, 지급 이력 조회

## 추가 확정 시 보강 항목

- 필수 요청 필드별 validation 케이스
- 실제 에러 코드와 메시지 매핑
- DDL에 FK가 제거되고, `uq_event_round_event_round_no`, `uq_event_applicant_event_member_id`, `uq_event_entry_event_round_member`, `uq_event_win_entry_id`만 최소 unique로 반영되는지 테스트
- 외부 point API 타임아웃 및 재시도 정책 테스트
- `idempotency_key = event_id + round_id + member_id` 전달 및 중복 지급 방지 테스트
