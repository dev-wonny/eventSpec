# Attendance Check Spec

출석체크는 현재 프로젝트의 1차 개발 범위다. 이 디렉터리의 문서는 출석체크 기능 정의를 요구사항부터 테스트 기준까지 한 흐름으로 정리하기 위한 문서다.

## 문서 구성

- `use-cases.md`: 사용자 시나리오와 요구사항
- `business-rules.md`: 출석 정책과 검증 규칙
- `service-validation.md`: Service 레이어에서 강제할 검증 규칙
- `concurrency-control.md`: 중복 출석 방지, DB unique, point API idempotency 전략
- `api-spec.md`: 확정된 외부용 API 계약 문서
- `data-spec.md`: 제공된 DDL을 반영한 데이터 문서
- `ddl-change.md`: 출석 spec 반영용 DDL 변경안
- `event-platform-schema-draft.sql`: 신규 환경 기준 전체 스키마 초안
- `exception-handling.md`: try-catch, 외부 API 실패, post-commit 운영 대응 정책
- `test-scenarios.md`: 테스트 및 인수 조건
- `open-questions.md`: 구현 전 확정이 필요한 항목
- `traceability.md`: 요구사항과 규칙/API/데이터/테스트 연결 상태

## 현재 상태

- 요구사항/규칙: DDL 및 도메인 설명 반영
- API 계약: 외부용 2개 API 기준 반영 완료
- 데이터 구조: DDL 반영 완료, API와의 gap 확인 필요
- DDL 변경안: FK 제거 + 최소 unique 기준으로 초안 작성 완료
- 전체 스키마 초안: 신규 환경 기준 SQL 초안 작성 완료
- 테스트 시나리오: DDL 기반으로 보정 완료

## 현재 핵심 해석

- 출석 단위는 `event_round`다.
- 월간 출석 이벤트는 날짜 수만큼 `event_round`를 가진다.
- `event_applicant`는 사전 참여 가능 대상자 테이블이 아니라, 출석 요청 시 생성하는 회차별 applicant 이력이며 `(event_id, round_id, member_id)`로 한 건만 생성된다.
- 이번 출석체크에는 회원별 사전 참여 가능 대상 체크가 없고, applicant 생성과 unique 충돌로 중복 출석을 제어한다.
- 출석 이벤트에서는 1일차, 2일차, 3일차마다 각각 별도의 `event_applicant`가 출석 요청 시 생성된다.
- `event_entry`는 실제 응모권/참여 이력 테이블이며 같은 `event_id + round_id + member_id`에도 여러 건이 들어갈 수 있다.
- 추첨형 이벤트에서는 `event_entry.is_winner`가 나중에 `false -> true`로 update될 수 있다.
- 출석체크형 이벤트는 즉시 보상이므로 `event_entry.is_winner = true`로 저장한다.
- 출석체크는 회차당 보상 매핑이 `0..1`개다. 매핑이 없으면 무보상 출석으로 처리한다.
- 랜덤 리워드는 하나의 `event_round`에 여러 `event_round_prize`를 둘 수 있고, 로컬 보상 확정은 `event_win.event_round_prize_id`로 추적된다.
- 랜덤 리워드의 기본 추첨 비중은 `event_round_prize_probability.weight`를 사용하고, `weight`는 DB 기본값 `1`을 사용한다.
- 랜덤 리워드의 꽝/미지급 케이스는 `event_win` 행 없이 처리한다.
- `event_entry.event_round_prize_id`는 보조값이며 `NULL` 가능하고, 로컬 보상 확정의 SoT는 `event_win.event_round_prize_id`다.
- `event_win`은 로컬 트랜잭션 안에서 확정된 당첨/보상 정보를 남긴다.
- 현재 출석 성공은 `event_applicant`, `event_entry`, `event_win` 로컬 커밋 기준이며, 외부 point API는 커밋 후 호출한다.
- 외부 point API timeout 기준은 `connection timeout = 1초`, `read timeout = 2초`, `총 대기 시간 = 최대 3초`다.
- 외부 point API 실패나 타임아웃은 local rollback 사유가 아니며, `ERROR` 로그와 운영 알림 대상으로 남긴다.
- `EVENT_NOT_ACTIVE`는 운영 중단/급정지 상황에 사용하고, 고객 메시지는 `현재 참여가 잠시 중단되었어요.`로 안내한다.
- `EVENT_EXPIRED`는 기간 만료뿐 아니라 운영 종료로 더 이상 참여를 받지 않는 상태까지 포함하고, 고객 메시지는 `이 이벤트는 참여가 마감되었어요.`로 안내한다.
- 이번 프로젝트는 외부용 API만 제공하고, admin API는 별도 프로젝트에서 담당한다.
- 현재 외부 API는 `POST /entries`, `GET /events/{eventId}` 두 개만 제공한다.
- 신규 환경용 schema draft에는 FK를 두지 않고, `uq_event_round_event_round_no`, `uq_event_applicant_event_round_member`, `uq_event_win_entry_id`만 최소 unique로 반영했다.
- 신규 환경용 schema draft에는 `NOT NULL event_applicant.round_id`, `event_entry.event_id`, `event_entry.round_id`를 반영했다.
- soft delete된 `event_applicant`, `event_entry`는 현재 유효 레코드로 보지 않으며 같은 키로 재출석을 허용한다.
- soft delete된 `prize`, `event_round_prize`는 현재 활성 설정 조회에서는 제외하지만, 과거 지급 이력 조회와 집계에서는 참조 가능해야 한다.

## 문서 사용 순서

1. `use-cases.md`로 기능 목표를 확인한다.
2. `business-rules.md`로 정책을 좁힌다.
3. `service-validation.md`로 Service 검증 책임을 고정한다.
4. `concurrency-control.md`로 동시성/중복 방지 전략을 고정한다.
5. `api-spec.md`와 `data-spec.md`에 입력물(DDL/API)을 연결한다.
6. `exception-handling.md`로 예외 및 post-commit 실패 대응 흐름을 고정한다.
7. `test-scenarios.md`로 구현 완료 기준을 검증한다.
8. `traceability.md`로 연결 누락이 없는지 확인한다.
9. 합의되지 않은 내용은 `open-questions.md`에 남긴다.

## 기능 식별자 prefix

- 출석체크 관련 문서는 `ATT` prefix를 사용한다.
