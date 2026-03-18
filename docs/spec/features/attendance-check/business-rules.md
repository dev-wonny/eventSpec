# Business Rules

이 문서는 출석체크 기능에서 구현 로직으로 옮겨질 정책을 정의한다. 아래 규칙은 현재 공유된 DDL과 확정된 API 계약, 추가 설명을 반영한 기준이다.

## 규칙

### ATT-RULE-001 출석 단위는 회차다

- 출석체크의 기본 단위는 `event_round`다.
- 월간 출석 이벤트는 같은 `event_id` 아래 날짜 수만큼 `event_round`를 가진다.
- 한 사용자는 서로 다른 회차에는 여러 번 출석할 수 있다.

### ATT-RULE-002 이벤트 상태 조건

- 출석 대상 이벤트는 `event.event_type = 'ATTENDANCE'` 여야 한다.
- 이벤트는 최소한 `is_active = TRUE`, `is_deleted = FALSE` 조건을 만족해야 한다.
- `is_active = FALSE`는 운영 중단 또는 급정지 상태로 해석하고 `EVENT_NOT_ACTIVE`를 반환한다.
- 출석 요청 시각은 `event.start_at ~ event.end_at` 범위 안에 있어야 한다.
- 이벤트가 아직 시작 전이면 `EVENT_NOT_STARTED`를 반환한다.
- `event.end_at`이 지났거나 운영 종료로 더 이상 참여를 받지 않는 상태는 `EVENT_EXPIRED`로 처리한다.
- 회차 시간이 별도로 관리된다면 `event_round.round_start_at ~ round_end_at`도 함께 만족해야 한다.
- `is_visible`은 전시 여부이며, 직접 API 출석 허용 여부를 제한하지 않는다.
- 따라서 `is_visible = FALSE`여도 다른 참여 조건을 만족하면 직접 API 출석은 허용한다.
- 고객 노출 메시지는 부정적인 표현보다 안내형 표현을 사용한다. `EVENT_NOT_ACTIVE`는 `현재 참여가 잠시 중단되었어요.`, `EVENT_NOT_STARTED`는 `이벤트 오픈 전이에요. 조금만 기다려 주세요.`, `EVENT_EXPIRED`는 `이 이벤트는 참여가 마감되었어요.`를 사용한다.

### ATT-RULE-003 회차별 applicant 생성 규칙

- `event_applicant`는 사전 참여 대상자 테이블이 아니라 회차별 applicant 기준 테이블이다.
- 이번 출석체크에서는 회원별 사전 참여 가능 대상 조회/허용 체크를 두지 않는다.
- 중복 방지 기준은 `(round_id, member_id)`이며, 같은 회차에는 한 건만 존재해야 한다.
- `event_applicant.round_id`는 `event_round.id`를 참조하는 FK로 보호한다.
- `event_applicant.event_id`는 조회용 값참조 컬럼이며 요청 `eventId`, `event_round.event_id`와 일치해야 한다.
- 출석 이벤트에서는 1일차, 2일차, 3일차마다 각각 별도의 `event_applicant`가 출석 요청 시 생성된다.
- `event_applicant.round_id`는 필수값이며 요청 회차와 일치해야 한다.
- 출석 요청 시 서버는 같은 키의 `event_applicant`가 이미 있으면 중복 출석으로 판단해야 한다.
- `event_applicant`는 별도 적재 대상이 아니라 출석 요청 처리 안에서 생성한다.

### ATT-RULE-004 실제 응모권 기록은 `event_entry`다

- 참여 이력의 SoT는 `event_entry`다.
- `event_entry`는 응모권/참여 이력 테이블이며 같은 회차에도 여러 건이 들어갈 수 있다.
- 출석 이벤트에서 `event_entry`는 `event_id`, `applicant_id`, `member_id`를 저장하고 회차는 `event_entry.applicant_id -> event_applicant.round_id`로 파생한다.
- `event_entry.event_round_prize_id`는 참여 시점의 보조값일 뿐이며 `NULL` 가능하다.
- 추첨형 이벤트에서는 최초 `event_entry.is_winner = false`로 저장한 뒤 나중에 `false -> true`로 update될 수 있다.
- 출석체크형 이벤트는 즉시 보상이므로 `event_entry.is_winner = true`로 저장한다.
- `event_applicant`는 회차별 applicant 기준이고, 실제 응모권 이력은 `event_entry`가 담당한다.

### ATT-RULE-005 동일 회차 중복 출석 방지

- 동일 사용자의 출석 중복 체크 비즈니스 기준은 `event_applicant`의 `round_id + member_id`다.
- 출석체크 이벤트에서는 오늘 날짜 회차가 target `roundId`가 되므로, 같은 날짜 재출석은 결국 같은 `round_id + member_id` 재요청이다.
- 다른 회차의 출석은 허용된다.
- 이를 위해 `event_applicant`는 최소 `event_id`, `round_id`, `member_id`를 가져야 하고, `event_id`는 요청값 및 `event_round.event_id`와 정합해야 한다.
- 중복으로 판단되면 프론트에는 `이미 출석했습니다` 메시지를 노출한다.

### ATT-RULE-006 입력값 및 참조 무결성 검증

- 서버는 이벤트, 회차, 사용자 식별값의 유효성을 검증해야 한다.
- `POST /entries`에는 `X-Member-Id`가 필수다.
- `GET /events/{eventId}`에서는 `X-Member-Id`가 선택이다.
- 요청에 `event_id`와 `round_id`가 함께 들어오면 `event_round.event_id`와 일치하는지 확인해야 한다.
- `event_round.event_id`, `event_round_prize.round_id`, `event_round_prize.prize_id`, `event_round_prize_probability.round_id`, `event_round_prize_probability.event_round_prize_id`, `event_applicant.round_id`는 DB FK로 보호한다.
- 다만 `event_applicant.event_id == event.id`, `event_entry.applicant_id`, `event_win.entry_id == event_entry.id` 같은 값참조 정합성은 애플리케이션에서 계속 검증해야 한다.
- `event_applicant.round_id`는 `NULL`이 아니어야 하며, 요청 `roundId`와 같아야 한다.
- 현재 참여자 식별은 `X-Member-Id`를 사용한다.

### ATT-RULE-007 동시성 제어

- 동일 사용자의 동일 회차 출석 요청이 동시에 들어와도 최종 유효 출석은 한 건이어야 한다.
- 최소 FK와 최소 unique를 함께 유지한다.
- 최소 unique 대상은 `event_round (event_id, round_no)`, `event_applicant (round_id, member_id)`, `event_win (entry_id)`다.
- 출석 중복은 `event_applicant` insert 시도와 unique 충돌 변환으로 제어해야 한다.
- 동시 요청에서는 `uq_event_applicant_round_member_id` unique 충돌도 중복 출석으로 변환해야 한다.
- 그 위에 잠금 전략, 트랜잭션 격리수준, 멱등 키, unique 충돌 처리 전략을 함께 검토해야 한다.

### ATT-RULE-008 이력 및 감사

- `event`, `event_round`, `event_applicant`, `event_entry`에는 공통 감사 컬럼이 존재한다.
- `event_applicant`는 회차별 생성 이력이고, `event_entry`는 응모권 이력이다.
- 시간 컬럼은 DB `TIMESTAMP`를 사용하고, 애플리케이션 해석 기준은 `Asia/Seoul`이다.
- `created_by`, `updated_by`는 `BIGINT`로 유지한다.
- system 처리도 문자열 `system`이 아니라 실행 주체로 해석하는 member id 값을 참조해 기록한다.
- soft delete된 `event_applicant`, `event_entry`는 현재 유효 레코드로 보지 않는다.
- 따라서 soft delete된 applicant/entry가 있어도 같은 키로 재출석은 허용한다.
- partial unique도 `is_deleted = FALSE` 조건만 묶으므로 soft delete 이후 같은 applicant 키의 새 레코드 생성이 가능해야 한다.

### ATT-RULE-009 로컬 당첨/보상 확정은 `event_win`에 기록한다

- 출석체크에서 로컬 트랜잭션 안에서 확정된 point 보상 이력은 `event_win`에 기록한다.
- 로컬 보상 확정의 SoT는 `event_win.event_round_prize_id`다.
- 출석체크는 회차당 active `event_round_prize`를 최대 1개만 둔다.
- 출석체크 회차에 보상 매핑이 없으면 point를 지급하지 않고 `event_win`도 생성하지 않는다.
- 출석 성공 1건에 대해 로컬로 확정된 보상 1건이 있다면 `event_win.event_round_prize_id`에는 그 보상 정책이 연결되어야 한다.
- 출석체크형 이벤트는 즉시 보상이므로 `event_entry.is_winner = true`와 `event_win` 생성이 같은 트랜잭션 안에서 완료된다.
- 랜덤 리워드에서는 응모 시 `event_entry.is_winner = false`로 저장하고, 추후 당첨 확정 시 `true`로 update한다.
- 향후 랜덤 리워드에서는 하나의 `event_round`에 여러 `event_round_prize`를 둘 수 있고, 로컬로 확정된 보상 결과를 `event_win`으로 추적한다.
- 랜덤 리워드에서 꽝 또는 미지급 케이스는 `event_win` 행 없이 처리한다.
- `event_win`은 현재 구조상 외부 보상 API 호출보다 먼저 로컬 트랜잭션 안에서 커밋된다.

### ATT-RULE-010 출석 보상은 point 중심으로 설계한다

- 출석체크 이벤트의 보상은 주로 `prize.reward_type = 'POINT'`를 기준으로 설계한다.
- `event_round_prize`는 회차별 보상 연결 테이블이다.
- 출석체크에서는 날짜별 회차에 point 보상을 `0..1`개 연결하는 방향을 기본값으로 본다.
- 현재 출석체크 범위에서는 재고/수량 제한을 적용하지 않는다.
- point 보상 매핑이 있으면 별도 재고 검증 없이 지급한다.
- 회차에 보상 매핑이 없으면 무보상 출석으로 처리한다.
- 랜덤 리워드에서는 같은 회차에 여러 보상 정책을 연결하고, 기본적으로 `weight` 기반으로 실제 지급 대상을 계산한다.
- 랜덤 리워드의 `weight` 기본값은 `1`이며, 운영 정책에 따라 확률 계산 보조값으로 사용한다.
- coupon, product 등 다른 보상 유형은 확장 가능하지만 현재 1차 출석체크 설계의 중심은 아니다.

### ATT-RULE-011 prize는 운영상 불변으로 취급한다

- `prize`는 immutable 정책으로 운영한다.
- `prize`는 CRUD 가능한 마스터 구조지만, 한 번 운영 세팅이 완료된 이후에는 수정하지 않는 것을 원칙으로 한다.
- 이미 사용 중인 보상 내용을 바꿔야 할 때는 기존 `prize`를 수정하지 않고 새 `prize`를 생성한 뒤 `event_round_prize` 연결을 바꾸는 방식으로 처리한다.
- 이 원칙을 통해 성과 집계나 추적 시 별도 snapshot 테이블 없이도 참조 시점의 의미를 안정적으로 유지한다.
- `prize`와 `event_round_prize`를 함께 생성하는 운영 흐름에서는 두 레코드를 하나의 트랜잭션으로 보고, 하나라도 실패하면 함께 롤백해야 한다.
- `event_round_prize`만 soft delete해도 `prize`는 그대로 유지한다.
- `prize`, `event_round_prize`를 함께 제거하려는 경우에는 두 테이블 모두 각각 soft delete한다.
- 현재 활성 보상 정책 조회에서는 soft delete된 `prize`, `event_round_prize`를 제외한다.
- 다만 과거 지급 이력 추적과 집계에서는 soft delete된 `prize`, `event_round_prize`도 참조 가능해야 한다.

### ATT-RULE-012 외부 point API는 트랜잭션 커밋 후 호출한다

- 출석 회차에 point 보상 매핑이 있는 경우에만 외부 point API를 호출한다.
- 보상 매핑이 있는 출석은 `event_applicant`, `event_entry.is_winner = true`, `event_win`을 먼저 저장하고 로컬 트랜잭션을 커밋한다.
- 외부 point API는 로컬 커밋 이후에 호출한다.
- 외부 point API 호출이 실패하거나 무응답이어도 `event_applicant`, `event_entry`, `event_win`은 롤백하지 않는다.
- 외부 point API 호출 실패는 `ERROR` 로그와 운영 알림으로 처리한다.
- 회차에 보상 매핑이 없으면 외부 point API를 호출하지 않고 `event_entry`만 저장한다.
- 외부 point API 호출 시 `idempotency_key = event_id + round_id + member_id`를 사용한다.
- 운영 재처리나 수동 재호출 시에도 같은 `idempotency_key`를 사용해 중복 지급을 방지한다.

### ATT-RULE-013 현재 보상 지급 연동은 동기식이다

- 현재는 외부 보상 API 응답을 기다리는 동기 처리 구조를 사용한다.
- 외부 point API의 timeout 기준은 `connection timeout = 1초`, `read timeout = 2초`, `총 대기 시간 = 최대 3초`다.
- 외부 point API 타임아웃은 외부 시스템 장애로 간주한다.
- 타임아웃이 발생해도 로컬 출석 데이터는 유지하고 운영 보정 대상으로 남긴다.
- 사용자 응답은 로컬 출석 성공을 유지한다.
- 외부 API가 응답하지 않으면 운영 알림을 남기고 후속 재처리 대상으로 관리한다.
- 향후에는 AWS 기반 메시지 큐를 통한 비동기 처리로 전환할 수 있으나, 현재 spec 범위는 아니다.

### ATT-RULE-014 조회 API 상태 계산 규칙

- `GET /events/{eventId}`는 출석 이벤트 전체 회차 목록을 반환한다.
- `X-Member-Id`가 있으면 각 회차의 `status`를 `ATTENDED / MISSED / TODAY / FUTURE`로 계산한다.
- 회차 상태 계산의 기준 시간대는 한국 시간(`Asia/Seoul`)이다.
- `X-Member-Id`가 없으면 각 회차의 `status = null`, `win = null`로 반환한다.
- 출석 완료 회차에 로컬로 확정된 보상이 있으면 `win`에 point 보상 정보를 반환한다.
- 출석은 완료됐지만 보상 매핑이 없었던 회차는 `status = ATTENDED`, `win = null`이다.

## 규칙과 요구사항 연결

- `ATT-REQ-001`: ATT-RULE-001, ATT-RULE-002, ATT-RULE-006
- `ATT-REQ-002`: ATT-RULE-002, ATT-RULE-003, ATT-RULE-004, ATT-RULE-006, ATT-RULE-009, ATT-RULE-012
- `ATT-REQ-003`: ATT-RULE-005, ATT-RULE-007
- `ATT-REQ-004`: ATT-RULE-004, ATT-RULE-006, ATT-RULE-008, ATT-RULE-009, ATT-RULE-014
- `ATT-REQ-005`: ATT-RULE-003, ATT-RULE-004, ATT-RULE-008, ATT-RULE-009, ATT-RULE-010, ATT-RULE-011
- `ATT-REQ-006`: ATT-RULE-009, ATT-RULE-012, ATT-RULE-013

## DDL 기반 구현 시사점

- 출석 spec 기준으로는 `event_applicant`에 `(round_id, member_id)` 최소 unique와 `event_entry`에 `event_id`, `member_id`, `applicant_id` 기반 조회 경로가 필요하다.
- 신규 환경용 schema draft에는 `event_round`, `event_round_prize`, `event_round_prize_probability`, `event_applicant` 최소 FK와 `uq_event_round_event_round_no`, `uq_event_applicant_round_member_id`, `uq_event_win_entry_id`를 반영한다.
- 현재 DDL만으로는 `ATTENDANCE` 이벤트의 회차당 active `event_round_prize`를 1개로 강제하기 어렵기 때문에 애플리케이션 또는 운영 검증이 필요하다.
- `event_round_prize`가 `prize`를 참조하므로, 보상 이력 안정성을 확보하려면 `prize` 변경 금지 원칙이 중요하다.
- `event_win`은 로컬 당첨/보상 확정 추적에 사용되므로, 외부 지급 성공 여부를 별도 상태나 운영 로그로 보완할 필요가 있다.

세부 Service 검증 책임은 `service-validation.md`, try-catch 및 rollback 흐름은 `exception-handling.md`를 기준으로 구현한다.
