# Business Rules

이 문서는 출석체크 기능에서 구현 로직으로 옮겨질 정책을 정의한다. 아래 규칙은 현재 공유된 DDL과 추가 설명을 반영한 기준안이다.

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

### ATT-RULE-003 참여자 앵커 생성 및 재사용

- 사용자의 첫 출석 시 이벤트 참여자 여부를 확인하고 `event_applicant`를 생성한다.
- 이후 같은 이벤트의 후속 출석에서는 기존 `event_applicant`를 재사용하고, 참여자 여부 확인을 반복하지 않는다.
- 현재 DDL의 `event_applicant`에는 `(round_id, member_id)` unique index만 존재하므로, 이벤트 단위 참여자 앵커 고유성은 애플리케이션이 추가로 관리해야 한다.

### ATT-RULE-004 실제 출석 기록은 append-only다

- 참여 이력의 SoT는 `event_entry`다.
- 출석 성공 시마다 `event_entry`에 새 레코드를 append-only로 저장한다.
- 향후 랜덤 리워드 기능에서도 참여 이력은 `event_entry`에 누적한다.
- `event_applicant`는 참여자 앵커이며, 참여 이력 자체를 대체하지 않는다.

### ATT-RULE-005 동일 회차 중복 출석 방지

- 동일 사용자는 동일 회차 또는 동일 출석일 기준으로 한 번만 유효한 `event_entry`를 가져야 한다.
- 오늘 날짜 기준으로 이미 유효한 `event_entry`가 존재하면 중복 출석으로 판단한다.
- 다른 회차의 출석은 허용된다.
- 현재 DDL에는 `event_entry` 기준 일자별 중복을 직접 막는 unique 제약이 없으므로, 서비스 로직에서 중복을 판단해야 한다.
- 중복으로 판단되면 프론트에는 `이미 출석했습니다` 메시지를 노출한다.

### ATT-RULE-006 입력값 및 참조 무결성 검증

- 서버는 이벤트, 회차, 사용자 식별값의 유효성을 검증해야 한다.
- 요청에 `event_id`와 `round_id`가 함께 들어오면 `event_round.event_id`와 일치하는지 확인해야 한다.
- `event_applicant.event_id`는 FK가 아니므로 `round_id`와의 정합성도 애플리케이션에서 검증해야 한다.
- 참여자 식별은 인증 정보 또는 요청 필드 중 하나로 확정해야 한다.

### ATT-RULE-007 동시성 제어

- 동일 사용자의 동일 회차 출석 요청이 동시에 들어와도 최종 유효 출석은 한 건이어야 한다.
- 현재 DDL만으로는 `event_entry` 중복 삽입을 막기 어려우므로, 잠금 전략, 트랜잭션 격리수준, 멱등 키, 추가 제약 중 하나 이상이 필요하다.

### ATT-RULE-008 이력 및 감사

- `event`, `event_round`, `event_applicant`, `event_entry`에는 공통 감사 컬럼이 존재한다.
- 출석 기능은 append-only 이력을 우선 사용하므로 취소/정정이 없다면 생성 이력 중심으로 운영할 수 있다.
- soft delete를 사용할 경우 재출석 허용 정책에 미치는 영향을 명확히 해야 한다.

### ATT-RULE-009 실제 지급 보상은 `event_win`에 기록한다

- 출석체크에서 실제 지급된 point 보상 이력은 `event_win`에 기록한다.
- 출석체크는 항상 point를 지급하므로 `event_win.event_round_prize_id`에는 해당 point 보상 정책이 연결되어야 한다.
- 향후 랜덤 리워드에서는 실제 지급된 보상 결과를 `event_win`으로 추적한다.
- `event_win`은 현재 구조상 외부 보상 API 요청이 성공한 경우에만 최종 커밋된다.

### ATT-RULE-010 출석 보상은 point 중심으로 설계한다

- 출석체크 이벤트의 보상은 주로 `prize.reward_type = 'POINT'`를 기준으로 설계한다.
- `event_round_prize`는 회차별 보상 연결 테이블이며, 출석체크에서는 날짜별 회차에 point 보상을 연결하는 방향을 기본값으로 본다.
- coupon, product 등 다른 보상 유형은 확장 가능하지만 현재 1차 출석체크 설계의 중심은 아니다.

### ATT-RULE-011 prize는 운영상 불변으로 취급한다

- `prize`는 CRUD 가능한 마스터 구조지만, 한 번 운영 세팅이 완료된 이후에는 수정하지 않는 것을 원칙으로 한다.
- 이미 사용 중인 보상 내용을 바꿔야 할 때는 기존 `prize`를 수정하지 않고 새 `prize`를 생성한 뒤 `event_round_prize` 연결을 바꾸는 방식으로 처리한다.
- 이 원칙을 통해 성과 집계나 추적 시 별도 snapshot 테이블 없이도 참조 시점의 의미를 안정적으로 유지한다.

### ATT-RULE-012 외부 point API 성공 시에만 출석 성공으로 확정한다

- 현재 출석체크는 외부 point API 호출 성공까지 포함해야 최종 성공이다.
- 외부 point API 호출이 실패하면 `event_entry`와 `event_win`은 모두 롤백되어야 한다.
- 외부 point API 호출이 실패한 요청은 출석 성공으로 간주하지 않는다.

### ATT-RULE-013 현재 보상 지급 연동은 동기식이다

- 현재는 외부 보상 API 응답을 기다리는 동기 처리 구조를 사용한다.
- 외부 API가 응답하지 않으면 프론트에는 현재 출석체크를 진행할 수 없다는 오류를 반환한다.
- 향후에는 AWS 기반 메시지 큐를 통한 비동기 처리로 전환할 수 있으나, 현재 spec 범위는 아니다.

## 규칙과 요구사항 연결

- `ATT-REQ-001`: ATT-RULE-001, ATT-RULE-002, ATT-RULE-006
- `ATT-REQ-002`: ATT-RULE-002, ATT-RULE-003, ATT-RULE-004, ATT-RULE-006, ATT-RULE-009, ATT-RULE-012
- `ATT-REQ-003`: ATT-RULE-005, ATT-RULE-007
- `ATT-REQ-004`: ATT-RULE-004, ATT-RULE-006, ATT-RULE-008, ATT-RULE-009
- `ATT-REQ-005`: ATT-RULE-003, ATT-RULE-004, ATT-RULE-008, ATT-RULE-009, ATT-RULE-010, ATT-RULE-011
- `ATT-REQ-006`: ATT-RULE-009, ATT-RULE-012, ATT-RULE-013

## DDL 기반 구현 시사점

- `uq_event_applicant_round_member_id`는 같은 회차 내 applicant 중복만 막는다.
- 현재 설명 기준의 일별 출석 중복 방지는 `event_entry`에 대한 별도 서비스 규칙이 필요하다.
- `event_entry`가 `round_id`를 직접 갖지 않으므로 회차 매핑 전략을 API/서비스에서 정해야 한다.
- `event_round_prize`가 `prize`를 참조하므로, 보상 이력 안정성을 확보하려면 `prize` 변경 금지 원칙이 중요하다.
- `event_win`은 외부 보상 API 성공 이력과 실제 지급 보상 추적에 사용되므로, 현재 출석 성공 판정은 외부 연동 성공에 강하게 결합된다.

세부 try-catch 및 rollback 흐름은 `exception-handling.md`를 기준으로 구현한다.
