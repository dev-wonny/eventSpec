# Traceability

이 문서는 출석체크 요구사항이 규칙, API, 데이터, 테스트와 모두 연결되었는지 확인하기 위한 추적 매트릭스다.

## 추적 매트릭스

| Requirement | 요약 | Rules | APIs | Data | Tests | 상태 |
| --- | --- | --- | --- | --- | --- | --- |
| ATT-REQ-001 | 출석 대상 식별 | ATT-RULE-002, ATT-RULE-003, ATT-RULE-004 | ATT-API-003 | ATT-DATA-001 | ATT-TEST-001, ATT-TEST-004, ATT-TEST-005 | 초안 |
| ATT-REQ-002 | 출석 요청 처리 | ATT-RULE-002, ATT-RULE-003, ATT-RULE-004 | ATT-API-001 | ATT-DATA-002, ATT-DATA-003 | ATT-TEST-001 | 초안 |
| ATT-REQ-003 | 중복 출석 통제 | ATT-RULE-001, ATT-RULE-005, ATT-RULE-006 | ATT-API-001 | ATT-DATA-004 | ATT-TEST-002, ATT-TEST-006, ATT-TEST-008 | 초안 |
| ATT-REQ-004 | 출석 결과 확인 | ATT-RULE-004, ATT-RULE-007 | ATT-API-002 | ATT-DATA-003, ATT-DATA-005 | ATT-TEST-007 | 초안 |
| ATT-REQ-005 | 후속 기능이 사용할 출석 기록 보존 | ATT-RULE-001, ATT-RULE-007 | ATT-API-001, ATT-API-002 | ATT-DATA-003, ATT-DATA-005 | ATT-TEST-009 | 초안 |

## 사용 규칙

- 새로운 요구사항이 생기면 이 문서에 먼저 ID를 추가한다.
- API 또는 DDL이 변경되면 연결된 행을 함께 갱신한다.
- 빈 칸이 남아 있으면 구현에 들어가지 않는다.
