# API Spec

이 문서는 제공 예정인 출석체크 최소 API 목록을 반영하는 계약 문서다. 현재는 문서 틀과 연결 기준만 먼저 정의한다.

## 입력 대기 항목

- 출석체크 최소 API 목록
- 요청/응답 샘플
- 인증 방식 또는 사용자 식별 방법

## 작성 규칙

- 하나의 엔드포인트는 최소 하나 이상의 요구사항 ID를 연결한다.
- 오류 응답은 비즈니스 규칙 ID와 함께 적는다.
- 요청/응답 스키마가 바뀌면 `test-scenarios.md`도 함께 갱신한다.

## 엔드포인트 목록 템플릿

| ID | Method | Path | 목적 | 연결 요구사항 | 연결 규칙 | 상태 |
| --- | --- | --- | --- | --- | --- | --- |
| ATT-API-001 | TBD | TBD | 출석 처리 | ATT-REQ-002, ATT-REQ-003 | ATT-RULE-001, ATT-RULE-005, ATT-RULE-006 | API 목록 대기 |
| ATT-API-002 | TBD | TBD | 출석 결과 조회 | ATT-REQ-004 | ATT-RULE-004, ATT-RULE-007 | API 목록 대기 |
| ATT-API-003 | TBD | TBD | 출석 가능 여부 확인 또는 대상 조회 | ATT-REQ-001 | ATT-RULE-002, ATT-RULE-003, ATT-RULE-004 | API 목록 대기 |

운영자 API가 필요 없다면 `ATT-API-002` 또는 `ATT-API-003`의 범위를 조정한다.

## 엔드포인트 상세 템플릿

### ATT-API-001

- 목적:
- 인증/인가:
- Request Path/Query:
- Request Body:
- Success Response:
- Error Response:
- 선행 규칙:
- 비고:

### ATT-API-002

- 목적:
- 인증/인가:
- Request Path/Query:
- Request Body:
- Success Response:
- Error Response:
- 선행 규칙:
- 비고:

### ATT-API-003

- 목적:
- 인증/인가:
- Request Path/Query:
- Request Body:
- Success Response:
- Error Response:
- 선행 규칙:
- 비고:

## 확인 포인트

- 중복 출석은 HTTP 에러로 처리할지, 성공이지만 동일 결과를 반환할지
- 출석 결과 조회가 필요한지, 출석 처리 응답만으로 충분한지
- 관리자 전용 API가 필요한지
