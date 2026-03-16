# Test Scenarios

이 문서는 출석체크 구현 완료 여부를 판단하는 테스트 기준이다. 현재는 DDL과 도메인 설명을 반영해, `event_entry` append-only 구조와 `event_applicant` 재사용 전략을 기준으로 작성한다.

## 기능 시나리오

| ID | 시나리오 | 기대 결과 | 연결 규칙 |
| --- | --- | --- | --- |
| ATT-TEST-001 | 첫 출석 요청 | `event_applicant`가 생성되고 외부 point API 성공 후 `event_entry`, `event_win`이 함께 저장된다 | ATT-RULE-002, ATT-RULE-003, ATT-RULE-004, ATT-RULE-009, ATT-RULE-012 |
| ATT-TEST-002 | 다른 날짜 회차 재출석 요청 | 기존 `event_applicant`를 재사용하고 외부 point API 성공 후 `event_entry`, `event_win`이 추가 저장된다 | ATT-RULE-001, ATT-RULE-003, ATT-RULE-004, ATT-RULE-009, ATT-RULE-012 |
| ATT-TEST-003 | 오늘 날짜에 이미 `event_entry`가 있는 사용자의 재출석 요청 | `이미 출석했습니다`를 반환하고 추가 `event_entry`가 생기지 않는다 | ATT-RULE-005 |
| ATT-TEST-004 | 존재하지 않는 이벤트 또는 회차로 요청 | 유효성 오류를 반환한다 | ATT-RULE-006 |
| ATT-TEST-005 | 이벤트와 회차가 서로 맞지 않는 요청 | 정합성 오류를 반환한다 | ATT-RULE-006 |
| ATT-TEST-006 | 비활성/삭제/기간 외 이벤트 또는 회차 요청 | 출석 불가 응답을 반환한다 | ATT-RULE-002 |
| ATT-TEST-007 | 동일 조건의 동시 출석 요청 2건 이상 발생 | 최종 유효 `event_entry`는 한 건만 남는다 | ATT-RULE-005, ATT-RULE-007 |
| ATT-TEST-008 | 출석 결과 조회 요청 | 대상 회차의 출석 결과와 지급 point 이력이 일관되게 반환된다 | ATT-RULE-006, ATT-RULE-008, ATT-RULE-009 |
| ATT-TEST-009 | 이미 applicant가 있는 사용자의 첫 일자 출석 요청 | 새 applicant 없이 외부 point API 성공 후 `event_entry`, `event_win`만 추가된다 | ATT-RULE-003, ATT-RULE-004, ATT-RULE-009, ATT-RULE-012 |
| ATT-TEST-010 | 월간 이벤트의 마지막 날짜 회차 출석 요청 | 올바른 `event_round`가 선택되고 출석이 저장된다 | ATT-RULE-001, ATT-RULE-002 |
| ATT-TEST-011 | 외부 point API 실패 | `event_entry`, `event_win`이 모두 롤백되고 출석 실패 응답을 반환한다 | ATT-RULE-009, ATT-RULE-012 |
| ATT-TEST-012 | 외부 point API 무응답 또는 타임아웃 | `event_entry`, `event_win`이 모두 롤백되고 프론트에 출석체크 불가 오류를 반환한다 | ATT-RULE-012, ATT-RULE-013 |
| ATT-TEST-013 | 출석 성공 시 지급 point 연결 확인 | `event_win.event_round_prize_id`가 회차의 point 보상 정책을 가리킨다 | ATT-RULE-009, ATT-RULE-010 |

## 비기능 시나리오

### ATT-TEST-014 감사 가능성

- `event_applicant`, `event_entry`, `event_win`에 최소 생성 시각과 추적 가능한 식별 정보가 남아야 한다.

### ATT-TEST-015 시간대 경계값

- 출석 시작/종료 시각 경계에서 정책대로 처리되어야 한다.

### ATT-TEST-016 append-only 보장

- 정상적인 재출석은 기존 `event_entry`를 수정하지 않고 새 레코드로 남아야 한다.

### ATT-TEST-017 출석 보상 point 연결

- 출석체크 회차에 연결되는 기본 보상은 `reward_type = 'POINT'`인 `prize`를 우선 사용해야 한다.

### ATT-TEST-018 prize 변경 대신 신규 생성 정책

- 이미 운영에 사용된 `prize`의 보상 정보를 수정하지 않고, 변경이 필요하면 새 `prize`를 생성해 `event_round_prize` 연결을 바꾸는 방식으로 처리되어야 한다.

### ATT-TEST-019 현재 동기식 외부 연동 제약

- 외부 point API 응답 지연 중에는 요청이 성공으로 확정되지 않아야 하며, 사용자에게는 실패/불가 상태가 반환되어야 한다.

## 테스트 레벨 가이드

- 단위 테스트: 회차 판정, applicant 재사용, 중복 정책
- 통합 테스트: API 요청/응답, 외부 point API 성공/실패, 트랜잭션 롤백
- 저장소 테스트: 활성 회차 조회, 기존 applicant 조회, 오늘 날짜 기준 중복 출석 존재 여부 조회, 회차별 point prize 조회, 지급 이력 조회

## API/DDL 수신 후 추가할 항목

- 필수 요청 필드별 validation 케이스
- 실제 에러 코드와 메시지 매핑
- `roundId` 직접 입력 방식인지 자동 계산 방식인지에 따른 분기 테스트
- 현재 DDL만으로는 `event_entry` 중복 제약이 없으므로 동시성 방어 전략 테스트
- 외부 point API 타임아웃 및 재시도 정책 테스트
