# API Spec

이 문서는 제공 예정인 출석체크 최소 API 목록을 반영하는 계약 문서다. 아직 API 목록은 없지만, 현재 DDL과 도메인 설명으로부터 도출되는 계약 포인트를 먼저 정리한다.

## 입력 대기 항목

- 출석체크 최소 API 목록
- 요청/응답 샘플
- 인증 방식 또는 사용자 식별 방법

## 작성 규칙

- 하나의 엔드포인트는 최소 하나 이상의 요구사항 ID를 연결한다.
- 오류 응답은 비즈니스 규칙 ID와 함께 적는다.
- 요청/응답 스키마가 바뀌면 `test-scenarios.md`도 함께 갱신한다.

## DDL 반영 메모

- 출석 성공 시 참여 이력은 `event_entry`에 저장된다.
- `event_applicant`는 첫 출석 시 생성하고 이후 재사용하는 참여자 앵커다.
- point 지급 성공 이력은 `event_win`에 저장된다.
- 회차 식별은 `event_round` 기준이지만, `event_entry`가 `round_id`를 직접 저장하지 않으므로 API에서 회차를 어떻게 다룰지 중요하다.
- 최소한 `eventId`, 사용자 식별값, 출석 대상 회차를 식별할 수 있는 정보가 필요하다.
- 현재 출석 성공은 외부 point API 성공까지 포함하며, 실패 시 `event_entry`, `event_win`은 함께 롤백된다.

## 엔드포인트 목록 템플릿

| ID | Method | Path | 목적 | 연결 요구사항 | 연결 규칙 | 상태 |
| --- | --- | --- | --- | --- | --- | --- |
| ATT-API-001 | TBD | TBD | 출석 처리 | ATT-REQ-002, ATT-REQ-003, ATT-REQ-006 | ATT-RULE-002, ATT-RULE-003, ATT-RULE-004, ATT-RULE-005, ATT-RULE-006, ATT-RULE-007, ATT-RULE-009, ATT-RULE-012, ATT-RULE-013 | API 목록 대기 |
| ATT-API-002 | TBD | TBD | 출석 결과 조회 | ATT-REQ-004, ATT-REQ-005 | ATT-RULE-004, ATT-RULE-006, ATT-RULE-008, ATT-RULE-009 | API 목록 대기 |
| ATT-API-003 | TBD | TBD | 출석 가능 여부 확인 또는 대상 조회 | ATT-REQ-001 | ATT-RULE-001, ATT-RULE-002, ATT-RULE-006 | API 목록 대기 |

운영자 API가 필요 없다면 `ATT-API-002` 또는 `ATT-API-003`의 범위를 조정한다.

## 엔드포인트 상세 템플릿

### ATT-API-001

- 목적: 특정 이벤트/회차에 출석한다.
- 인증/인가: 회원 인증 필요 여부 확정 필요
- Request Path/Query: `eventId`, `roundId` 직접 수신 여부 미정
- Request Body: 사용자 식별 정보 직접 수신 여부 미정
- Success Response: `applicantId`, `entryId`, `winId`, `eventId`, `roundId 또는 출석일`, `appliedAt`, `rewardType`, `pointAmount` 후보
- Error Response: 이벤트 없음, 회차 없음, 이벤트-회차 불일치, 오늘 날짜 기준 이미 출석함, 출석 가능 시간 아님, 외부 point API 실패, 외부 point API 무응답
- 선행 규칙: ATT-RULE-001, ATT-RULE-002, ATT-RULE-003, ATT-RULE-005, ATT-RULE-006, ATT-RULE-007, ATT-RULE-009, ATT-RULE-012, ATT-RULE-013
- 비고: 첫 출석이면 `event_applicant` 생성, 이후에는 재사용. 오늘 날짜의 `event_entry`가 이미 있으면 프론트에는 `이미 출석했습니다`를 반환한다. 현재는 외부 point API 성공 후에만 최종 성공 응답

### ATT-API-002

- 목적: 특정 이벤트/회차 기준 출석 결과를 확인한다.
- 인증/인가: 본인 조회 또는 운영자 조회 범위 확정 필요
- Request Path/Query: `eventId`, `roundId 또는 날짜`, `memberId` 취급 방식 미정
- Request Body:
- Success Response: 대상 회차의 출석 여부, `entryId`, `winId`, `appliedAt`, `eventRoundPrizeId`, `pointAmount` 후보
- Error Response: 조회 대상 없음, 권한 없음
- 선행 규칙: ATT-RULE-001, ATT-RULE-004, ATT-RULE-006, ATT-RULE-008, ATT-RULE-009
- 비고: `event_entry`가 회차를 직접 저장하지 않으면 조회 기준을 명확히 해야 한다. 조회 시 `event_win` 조인이 필요할 수 있다.

### ATT-API-003

- 목적: 출석 가능한 이벤트/회차 또는 현재 활성 회차를 확인한다.
- 인증/인가: 공개 여부 정책에 따라 달라질 수 있음
- Request Path/Query: `eventId`
- Request Body:
- Success Response: 활성 이벤트 상태, 활성 회차 정보, 이미 출석했는지 여부 후보
- Error Response: 이벤트 없음, 출석 불가 상태
- 선행 규칙: ATT-RULE-001, ATT-RULE-002, ATT-RULE-006
- 비고: 클라이언트가 `roundId`를 직접 알아야 하는 구조인지 확인 필요

## 확인 포인트

- `roundId`를 클라이언트가 넘기는지, 서버가 현재 시각으로 계산하는지
- 출석 결과 조회가 필요한지, 출석 처리 응답만으로 충분한지
- 관리자 전용 API가 필요한지
- 외부 point API 실패/무응답 시 어떤 에러 코드와 메시지를 반환할지
- 향후 AWS 기반 비동기 큐 전환 시 현재 API 계약을 어떻게 유지할지
