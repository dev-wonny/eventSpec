# Business Rules

이 문서는 출석체크 기능에서 구현 로직으로 옮겨질 정책을 정의한다. 아래 규칙은 현재 공유된 DDL과 확정된 API 계약, 추가 설명을 반영한 기준이다.

## 규칙

### ATT-RULE-001 출석 단위는 회차다

- 출석체크의 기본 단위는 `event_round`다.
- 월간 출석 이벤트는 같은 `event_id` 아래 날짜 수만큼 `event_round`를 가진다.
- 한 사용자는 서로 다른 회차에는 여러 번 출석할 수 있다.

### ATT-RULE-002 출석 가능 대상 조건

- 출석 대상 이벤트는 `event.event_type = 'ATTENDANCE'` 여야 한다.
- 이벤트는 최소한 `is_active = TRUE`, `is_deleted = FALSE` 조건을 만족해야 한다.
- 출석 요청 시각은 `event.start_at ~ event.end_at` 범위 안에 있어야 한다.
- 회차 시간이 별도로 관리된다면 `event_round.round_start_at ~ round_end_at`도 함께 만족해야 한다.
- `is_visible`을 출석 가능 조건으로 볼지는 별도 정책 확정이 필요하다.

### ATT-RULE-003 참여 가능 대상자(Eligibility) 확인

- `event_applicant`는 실제 참여 기록이 아니라 이벤트 참여 가능 대상자 풀이다.
- `event_applicant`는 참여 조건이나 대상자 판정 결과를 사전에 적재해두는 테이블로 사용한다.
- 기본 식별 기준은 `(event_id, member_id)`이며, 신규 환경용 schema draft에는 이 unique를 반영한다.
- `event_applicant.round_id`는 선택값이며 특정 회차에만 참여 가능하도록 제한할 때 사용한다.
- 출석 요청 시 서버는 `event_applicant`를 기준으로 참여 가능 대상 여부를 검증해야 한다.
- 이벤트가 기간 조건을 만족하고 `event_applicant`에 사용자가 존재하면, 참여 조건을 다시 계산하거나 외부 조회하지 않고 참여 가능자로 처리할 수 있다.
- 현재 프로젝트는 `event_applicant` 관리 API를 제공하지 않으므로 대상자 적재는 SQL 또는 별도 admin 프로젝트에서 관리한다.

### ATT-RULE-004 실제 출석 기록은 append-only다

- 참여 이력의 SoT는 `event_entry`다.
- 출석 성공 시마다 `event_entry`에 새 레코드를 append-only로 저장한다.
- 출석 이벤트에서 `event_entry`는 매일 출석 여부를 판단할 수 있도록 `event_id`, `round_id`를 가져야 한다.
- 향후 랜덤 리워드 기능에서도 참여 이력은 `event_entry`에 누적한다.
- `event_applicant`는 eligibility 정보이며, 참여 이력 자체를 대체하지 않는다.

### ATT-RULE-005 동일 회차 중복 출석 방지

- 동일 사용자의 출석 중복 체크 비즈니스 기준은 `event_id + round_id + member_id`다.
- 출석체크 이벤트에서는 오늘 날짜 회차가 target `roundId`가 되므로, 같은 날짜 재출석은 결국 같은 `event_id + round_id + member_id` 재요청이다.
- 다른 회차의 출석은 허용된다.
- 이를 위해 `event_entry`는 최소 `event_id`, `round_id`, `member_id`를 가져야 하고, `event_id`는 요청값 및 `event_round.event_id`와 정합해야 한다.
- 중복으로 판단되면 프론트에는 `이미 출석했습니다` 메시지를 노출한다.

### ATT-RULE-006 입력값 및 참조 무결성 검증

- 서버는 이벤트, 회차, 사용자 식별값의 유효성을 검증해야 한다.
- `POST /entries`에는 `X-Member-Id`가 필수다.
- `GET /events/{eventId}`에서는 `X-Member-Id`가 선택이다.
- 요청에 `event_id`와 `round_id`가 함께 들어오면 `event_round.event_id`와 일치하는지 확인해야 한다.
- `event_applicant.event_id`는 FK가 아니므로 `round_id`와의 정합성도 애플리케이션에서 검증해야 한다.
- `event_applicant.round_id`가 `NULL`이 아니면 요청 `round_id`와 일치해야 한다.
- 현재 참여자 식별은 `X-Member-Id`를 사용한다.

### ATT-RULE-007 동시성 제어

- 동일 사용자의 동일 회차 출석 요청이 동시에 들어와도 최종 유효 출석은 한 건이어야 한다.
- `event_applicant (event_id, member_id)` unique 제약을 대상자 데이터 중복 적재의 기본 방어선으로 둬야 한다.
- 출석 중복은 `event_id + round_id + member_id` 기준 조회와 검증으로 제어해야 한다.
- 그 위에 잠금 전략, 트랜잭션 격리수준, 멱등 키, `event_id + round_id + member_id` 조회 index를 함께 검토해야 한다.

### ATT-RULE-008 이력 및 감사

- `event`, `event_round`, `event_applicant`, `event_entry`에는 공통 감사 컬럼이 존재한다.
- 출석 기능은 append-only 이력을 우선 사용하므로 취소/정정이 없다면 생성 이력 중심으로 운영할 수 있다.
- soft delete를 사용할 경우 재출석 허용 정책에 미치는 영향을 명확히 해야 한다.

### ATT-RULE-009 실제 지급 보상은 `event_win`에 기록한다

- 출석체크에서 실제 지급된 point 보상 이력은 `event_win`에 기록한다.
- 출석체크는 회차당 active `event_round_prize`를 최대 1개만 둔다.
- 출석체크 회차에 보상 매핑이 없으면 point를 지급하지 않고 `event_win`도 생성하지 않는다.
- 출석 성공 1건에 대해 실제로 지급된 보상 1건이 있다면 `event_win.event_round_prize_id`에는 그 보상 정책이 연결되어야 한다.
- 향후 랜덤 리워드에서는 하나의 `event_round`에 여러 `event_round_prize`를 둘 수 있고, 실제 지급된 보상 결과를 `event_win`으로 추적한다.
- `event_win`은 현재 구조상 외부 보상 API 요청이 성공한 경우에만 최종 커밋된다.

### ATT-RULE-010 출석 보상은 point 중심으로 설계한다

- 출석체크 이벤트의 보상은 주로 `prize.reward_type = 'POINT'`를 기준으로 설계한다.
- `event_round_prize`는 회차별 보상 연결 테이블이다.
- 출석체크에서는 날짜별 회차에 point 보상을 `0..1`개 연결하는 방향을 기본값으로 본다.
- 회차에 보상 매핑이 없으면 무보상 출석으로 처리한다.
- 랜덤 리워드에서는 같은 회차에 여러 보상 정책을 연결하고, 확률 정책으로 실제 지급 대상을 계산할 수 있다.
- coupon, product 등 다른 보상 유형은 확장 가능하지만 현재 1차 출석체크 설계의 중심은 아니다.

### ATT-RULE-011 prize는 운영상 불변으로 취급한다

- `prize`는 CRUD 가능한 마스터 구조지만, 한 번 운영 세팅이 완료된 이후에는 수정하지 않는 것을 원칙으로 한다.
- 이미 사용 중인 보상 내용을 바꿔야 할 때는 기존 `prize`를 수정하지 않고 새 `prize`를 생성한 뒤 `event_round_prize` 연결을 바꾸는 방식으로 처리한다.
- 이 원칙을 통해 성과 집계나 추적 시 별도 snapshot 테이블 없이도 참조 시점의 의미를 안정적으로 유지한다.

### ATT-RULE-012 외부 point API 성공 시에만 출석 성공으로 확정한다

- 출석 회차에 point 보상 매핑이 있는 경우에만 외부 point API를 호출한다.
- 외부 point API 호출이 실패하면 `event_entry`와 `event_win`은 모두 롤백되어야 한다.
- 외부 point API 호출이 실패한 요청은 출석 성공으로 간주하지 않는다.
- 회차에 보상 매핑이 없으면 외부 point API를 호출하지 않고 `event_entry`만 저장한다.

### ATT-RULE-013 현재 보상 지급 연동은 동기식이다

- 현재는 외부 보상 API 응답을 기다리는 동기 처리 구조를 사용한다.
- 외부 API가 응답하지 않으면 프론트에는 현재 출석체크를 진행할 수 없다는 오류를 반환한다.
- 향후에는 AWS 기반 메시지 큐를 통한 비동기 처리로 전환할 수 있으나, 현재 spec 범위는 아니다.

### ATT-RULE-014 조회 API 상태 계산 규칙

- `GET /events/{eventId}`는 출석 이벤트 전체 회차 목록을 반환한다.
- `X-Member-Id`가 있으면 각 회차의 `status`를 `ATTENDED / MISSED / TODAY / FUTURE`로 계산한다.
- `X-Member-Id`가 없으면 각 회차의 `status = null`, `win = null`로 반환한다.
- 출석 완료 회차에 실제 지급된 보상이 있으면 `win`에 point 보상 정보를 반환한다.
- 출석은 완료됐지만 보상 매핑이 없었던 회차는 `status = ATTENDED`, `win = null`이다.

## 규칙과 요구사항 연결

- `ATT-REQ-001`: ATT-RULE-001, ATT-RULE-002, ATT-RULE-006
- `ATT-REQ-002`: ATT-RULE-002, ATT-RULE-003, ATT-RULE-004, ATT-RULE-006, ATT-RULE-009, ATT-RULE-012
- `ATT-REQ-003`: ATT-RULE-005, ATT-RULE-007
- `ATT-REQ-004`: ATT-RULE-004, ATT-RULE-006, ATT-RULE-008, ATT-RULE-009, ATT-RULE-014
- `ATT-REQ-005`: ATT-RULE-003, ATT-RULE-004, ATT-RULE-008, ATT-RULE-009, ATT-RULE-010, ATT-RULE-011
- `ATT-REQ-006`: ATT-RULE-009, ATT-RULE-012, ATT-RULE-013

## DDL 기반 구현 시사점

- 출석 spec 기준으로는 `event_applicant`에 `(event_id, member_id)` unique와 nullable `round_id`, `event_entry`에 `event_id`, `round_id`, `member_id` 기반 조회 경로가 필요하다.
- 신규 환경용 schema draft에는 위 구조와 조회 index를 반영한다.
- 현재 DDL만으로는 `ATTENDANCE` 이벤트의 회차당 active `event_round_prize`를 1개로 강제하기 어렵기 때문에 애플리케이션 또는 운영 검증이 필요하다.
- `event_round_prize`가 `prize`를 참조하므로, 보상 이력 안정성을 확보하려면 `prize` 변경 금지 원칙이 중요하다.
- `event_win`은 외부 보상 API 성공 이력과 실제 지급 보상 추적에 사용되므로, 보상 매핑이 있는 출석 성공 판정은 외부 연동 성공에 강하게 결합된다.

세부 Service 검증 책임은 `service-validation.md`, try-catch 및 rollback 흐름은 `exception-handling.md`를 기준으로 구현한다.
