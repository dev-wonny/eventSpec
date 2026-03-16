# Attendance Check Spec

출석체크는 현재 프로젝트의 1차 개발 범위다. 이 디렉터리의 문서는 출석체크 기능 정의를 요구사항부터 테스트 기준까지 한 흐름으로 정리하기 위한 문서다.

## 문서 구성

- `use-cases.md`: 사용자 시나리오와 요구사항
- `business-rules.md`: 출석 정책과 검증 규칙
- `service-validation.md`: Service 레이어에서 강제할 검증 규칙
- `api-spec.md`: 확정된 외부용 API 계약 문서
- `data-spec.md`: 제공된 DDL을 반영한 데이터 문서
- `ddl-change.md`: 출석 spec 반영용 DDL 변경안
- `event-platform-schema-draft.sql`: 신규 환경 기준 전체 스키마 초안
- `exception-handling.md`: try-catch, 외부 API 실패, 트랜잭션/롤백 정책
- `test-scenarios.md`: 테스트 및 인수 조건
- `open-questions.md`: 구현 전 확정이 필요한 항목
- `traceability.md`: 요구사항과 규칙/API/데이터/테스트 연결 상태

## 현재 상태

- 요구사항/규칙: DDL 및 도메인 설명 반영
- API 계약: 외부용 2개 API 기준 반영 완료
- 데이터 구조: DDL 반영 완료, API와의 gap 확인 필요
- DDL 변경안: `event_applicant`/`event_entry` 핵심 제약 초안 작성 완료
- 전체 스키마 초안: 신규 환경 기준 SQL 초안 작성 완료
- 테스트 시나리오: DDL 기반으로 보정 완료

## 현재 핵심 해석

- 출석 단위는 `event_round`다.
- 월간 출석 이벤트는 날짜 수만큼 `event_round`를 가진다.
- `event_applicant`는 `(event_id, member_id)` 기준의 이벤트 참여 가능 대상자 풀이다.
- `event_applicant.round_id`는 선택값이며 특정 회차 대상일 때만 사용한다.
- `event_entry`는 출석 성공 및 향후 랜덤 리워드 참여 이력을 append-only로 누적한다.
- 출석 이벤트에서 `event_entry`는 일별 출석 여부 판단을 위해 `event_id`, `round_id`를 가져야 한다.
- 출석체크는 회차당 보상 매핑이 `0..1`개다. 매핑이 없으면 무보상 출석으로 처리한다.
- 랜덤 리워드는 하나의 `event_round`에 여러 `event_round_prize`를 둘 수 있고, 실제 지급 보상은 `event_win.event_round_prize_id`로 확정된다.
- `event_win`은 실제 지급된 보상과 외부 보상 API 성공 이력을 남긴다.
- 현재 출석 성공은 보상 매핑이 있는 경우 외부 point API 성공까지 포함한다.
- 이번 프로젝트는 외부용 API만 제공하고, admin API는 별도 프로젝트에서 담당한다.
- 현재 외부 API는 `POST /entries`, `GET /events/{eventId}` 두 개만 제공한다.
- 신규 환경용 schema draft에는 `event_applicant (event_id, member_id) unique`, nullable `event_applicant.round_id`, `event_entry.event_id`, `event_entry.round_id`, `event_entry (event_id, round_id, member_id)` 조회 index를 반영했다.

## 문서 사용 순서

1. `use-cases.md`로 기능 목표를 확인한다.
2. `business-rules.md`로 정책을 좁힌다.
3. `service-validation.md`로 Service 검증 책임을 고정한다.
4. `api-spec.md`와 `data-spec.md`에 입력물(DDL/API)을 연결한다.
5. `exception-handling.md`로 예외 및 rollback 흐름을 고정한다.
6. `test-scenarios.md`로 구현 완료 기준을 검증한다.
7. `traceability.md`로 연결 누락이 없는지 확인한다.
8. 합의되지 않은 내용은 `open-questions.md`에 남긴다.

## 기능 식별자 prefix

- 출석체크 관련 문서는 `ATT` prefix를 사용한다.
