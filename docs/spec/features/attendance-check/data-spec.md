# Data Spec

이 문서는 현재 제공된 PostgreSQL DDL을 출석체크 관점으로 해석한 결과다. 현재 도메인 설명까지 반영하면 회차별 참여 신청 정보는 `event_applicant`, 실제 참여 이력은 `event_entry`, 로컬 당첨/보상 확정 이력은 `event_win`, 출석 단위는 `event_round`로 보는 것이 맞다.

## 출석체크 핵심 흐름

1. `event`에서 출석체크 이벤트를 찾는다.
2. `event_round`에서 대상 날짜의 회차를 `id + event_id` 조건으로 찾는다.
3. `event_round.event_id`는 FK로 보호하지만, 요청 `eventId`와 `round.event_id`가 일치하는지는 애플리케이션에서 검증한다.
4. `event_applicant`를 `round_id + member_id` 기준 회차별 applicant 레코드로 생성한다.
5. 출석 회차에 연결된 단일 point 보상 매핑이 있는지 확인한다.
6. `event_entry`를 `event_id + applicant_id + member_id` 기준으로 저장하고 회차는 applicant를 통해 해석한다.
7. 보상 매핑이 있으면 `event_win`을 저장하고 로컬 트랜잭션을 커밋한다.
8. 로컬 트랜잭션 안에서 `PointGrantCommand`를 발행하고, commit 성공 후 `AFTER_COMMIT` listener가 외부 point API를 호출한다.

## 저장 예시

### 출석 3일차 이벤트

1일차:

- `event_round.round_no = 1`
- `event_applicant` 1일차 row insert
- `event_entry`는 1일차 applicant id를 참조하고 `is_winner = true`로 insert
- `event_win` 1일차 row insert

2일차:

- `event_round.round_no = 2`
- `event_applicant` 2일차 row insert
- `event_entry`는 2일차 applicant id를 참조하고 `is_winner = true`로 insert
- `event_win` 2일차 row insert

3일차:

- `event_round.round_no = 3`
- `event_applicant` 3일차 row insert
- `event_entry`는 3일차 applicant id를 참조하고 `is_winner = true`로 insert
- `event_win` 3일차 row insert

### 추후 DRAW 이벤트 예시

- `event_round.round_no = 1`
- `event_applicant`는 추첨 참여용 회차 applicant로 생성한다.
- `event_entry`는 `applicant_id`만 저장하고 최초에는 `is_winner = false`로 insert한다.
- 시간이 지난 뒤 추첨 시점에 해당 applicant가 연결된 회차 기준으로 대상을 모은다.
- 당첨되면 `event_entry.is_winner = true` update 후 `event_win`을 insert한다.

```java
if (!round.getEventId().equals(event.getId())) {
    throw new IllegalArgumentException("ROUND_EVENT_MISMATCH");
}
```

```sql
SELECT *
FROM event_round
WHERE id = :roundId
  AND event_id = :eventId;
```

## 핵심 객체 매핑

| ID | 논리 개념 | 물리 객체 | 핵심 컬럼 | 관련 요구사항 | 확인 상태 | 메모 |
| --- | --- | --- | --- | --- | --- | --- |
| ATT-DATA-001 | 출석 이벤트 마스터 | `promotion.event` | `id`, `event_type`, `supplier_id`, `start_at`, `end_at`, `is_active`, `is_visible`, `is_deleted`, `is_multiple_entry`, `is_auto_entry` | ATT-REQ-001 | DDL 반영 | 출석체크 이벤트는 `event_type = 'ATTENDANCE'`로 식별 |
| ATT-DATA-002 | 출석 회차 | `promotion.event_round` | `id`, `event_id`, `round_no`, `round_start_at`, `round_end_at`, `is_deleted` | ATT-REQ-001, ATT-REQ-003 | DDL 반영 | `event_id -> event.id` FK, 월간 출석 이벤트는 날짜 수만큼 회차 생성 |
| ATT-DATA-003 | 회차별 applicant 기준 | `promotion.event_applicant` | `id`, `event_id`, `round_id`, `member_id`, `is_deleted` | ATT-REQ-002, ATT-REQ-005 | 업무 확정, schema draft 반영 | `round_id -> event_round.id` FK, `(round_id, member_id)` 최소 unique, `event_id + member_id` 집합 잠금 조회에 사용 |
| ATT-DATA-004 | 응모권/참여 이력 | `promotion.event_entry` | `id`, `applicant_id`, `event_id`, `member_id`, `applied_at`, `event_round_prize_id`, `is_winner`, `is_deleted` | ATT-REQ-002, ATT-REQ-004, ATT-REQ-005 | 업무 확정, schema draft 반영 | 회차는 `applicant_id -> event_applicant.round_id`로 파생, 같은 회차/회원에도 여러 건 저장 가능 |
| ATT-DATA-005 | 회차 번호 유일성 | `uq_event_round_event_round_no` | `(event_id, round_no)` | ATT-REQ-001 | DDL 반영 | 이벤트 내 회차 번호 중복 방지 |
| ATT-DATA-006 | applicant 고유성 요구사항 | 출석 spec 기준 | `(round_id, member_id)` | ATT-REQ-002 | 업무 확정, schema draft 반영 | 회차별 applicant 최소 unique 기준 |
| ATT-DATA-007 | 출석 중복 판정 기준 | 출석 spec 기준 | `(round_id, member_id)` | ATT-REQ-003 | 업무 확정, schema draft 반영 | 출석체크에서는 `event_applicant` 기준으로 중복 판정 |
| ATT-DATA-008 | 보상 마스터 | `promotion.prize` | `id`, `prize_name`, `reward_type`, `point_amount`, `coupon_id`, `is_active`, `is_deleted` | ATT-REQ-005 | DDL 반영 | 출석체크는 주로 `POINT` 유형 사용 |
| ATT-DATA-009 | 회차별 보상 연결 | `promotion.event_round_prize` | `id`, `round_id`, `prize_id`, `priority`, `daily_limit`, `total_limit`, `is_active`, `is_deleted` | ATT-REQ-005 | DDL 반영 | `round_id`, `prize_id` FK 보유, 출석체크는 회차당 `0..1` |
| ATT-DATA-010 | 보상 결과 이력 | `promotion.event_win` | `id`, `entry_id`, `round_id`, `event_id`, `member_id`, `event_round_prize_id`, `is_deleted` | ATT-REQ-002, ATT-REQ-004, ATT-REQ-005, ATT-REQ-006 | DDL 반영 | 로컬 당첨/보상 확정 시 생성 |
| ATT-DATA-011 | 확률 기반 보상 확장 경로 | `promotion.event_round_prize_probability` | `id`, `round_id`, `event_round_prize_id`, `probability`, `weight` | ATT-REQ-005 | DDL 반영 | `round_id`, `event_round_prize_id` FK 보유, DB 기본값은 `1`, Java 코드는 `DEFAULT_WEIGHT` 상수 사용 |
| ATT-DATA-012 | 감사/삭제 정보 | `event`, `event_round`, `event_applicant`, `event_entry`, `event_win`, `prize`, `event_round_prize` | `created_at`, `created_by`, `updated_at`, `updated_by`, `deleted_at`, `is_deleted` | ATT-REQ-005, ATT-REQ-006 | DDL 반영 | 전 테이블 공통 감사 패턴, `created_by/updated_by`는 `BIGINT` |

## 현재 해석에서 중요한 포인트

- `event_round`는 일별 출석 단위다.
- `event.supplier_id`는 외부 값참조 식별자이며 현재는 위드 DB 값을 사용한다.
- 이후 버터가 쇼핑몰 외주 개발을 완료하면 `supplier_id`도 위드 데이터를 버터 DB로 마이그레이션해 사용할 예정이다.
- 현재 `supplier_id` 용도는 돌쇠네 자체 서비스 범위다.
- `event_applicant`는 이벤트 단위 사전 참여 가능 대상 풀이 아니라, 출석 요청 시 생성하는 회차별 applicant 기준 레코드다.
- `event_applicant`는 `(round_id, member_id)` 기준으로 식별하며, 같은 회차에는 한 건만 존재해야 한다.
- 출석 이벤트에서는 1일차, 2일차, 3일차마다 각각 별도의 `event_applicant`가 생성된다.
- 같은 `event_id + member_id`의 applicant 집합은 누적 출석 수 계산 시 잠금 대상이다.
- 누적 출석 수는 잠금된 applicant 집합 크기에 `+1` 해서 계산한다.
- 이번 출석체크에서는 회원별 사전 참여 가능 대상 체크를 두지 않는다.
- `event_applicant.round_id`는 FK로 보호하고, `event_id`는 조회용 값참조로 유지한다.
- soft delete된 `event_applicant`는 현재 유효 applicant로 보지 않으며, 동일 `(round_id, member_id)`로 새 레코드를 다시 생성할 수 있다.
- `event_entry`는 실제 응모권/참여 이력 테이블이다.
- `event_entry.applicant_id`는 어떤 applicant 레코드를 기준으로 생성된 응모권인지 연결한다.
- `event_entry`는 `round_id`를 직접 가지지 않고 `applicant_id -> event_applicant.round_id`로 회차를 해석한다.
- 출석 이벤트에서 `event_entry`는 회차별 출석 이력을 나타내고, 추첨형 이벤트에서는 같은 회차에도 여러 응모권이 들어갈 수 있다.
- `event_entry.is_winner`는 추첨형 이벤트에서 최초 `false`로 저장됐다가 나중에 update될 수 있다.
- 출석체크형 이벤트는 즉시 보상이므로 `event_entry.is_winner = true`로 저장한다.
- `event_entry.event_round_prize_id`는 보조값이며 `NULL` 가능하다.
- `event_win`은 로컬 트랜잭션 안에서 확정된 당첨/보상 정보를 저장한다.
- 로컬 보상 확정의 SoT는 `event_win.event_round_prize_id`다.
- 출석 중복 여부의 비즈니스 기준은 `(round_id, member_id)` applicant 생성 여부다.
- `event_entry`는 출석 중복 제어용 unique를 갖지 않는다.
- 신규 환경용 schema draft는 `promotion` 스키마와 `TIMESTAMP` 컬럼을 사용하고, 애플리케이션 해석 기준은 `Asia/Seoul`이다.
- 최소 FK 대상은 `event_round`, `event_round_prize`, `event_round_prize_probability`, `event_applicant`다.
- 최소 unique 대상은 `uq_event_round_event_round_no`, `uq_event_applicant_round_member_id`, `uq_event_win_entry_id`다.
- 감사 컬럼 `created_by`, `updated_by`는 `BIGINT`로 유지한다.
- system 처리도 문자열 계정명이 아니라 실행 주체로 해석하는 member id 값을 참조해 기록한다.
- 제공된 원본 DDL은 `event_entry.event_id`가 없어 API 계약을 그대로 구현하기 어려웠고, 현재 정리에서는 `round_id`는 applicant join으로 대체한다.
- `event_applicant.round_id`는 FK로 보호되지만, `event_applicant.event_id`는 값참조이므로 기준 이벤트 정합성은 애플리케이션에서 보장해야 한다.
- `event_applicant`는 별도 적재 API 없이 출석 요청 시 생성한다.
- 출석 보상은 `event_round_prize -> prize` 연결로 관리하며, 출석체크에서는 회차당 보상 매핑을 `0..1`개만 둔다.
- `prize`와 `event_round_prize`를 함께 생성하는 경우에는 하나의 운영 트랜잭션으로 본다.
- 이 생성 흐름에서 `event_round_prize` 생성이 실패하면 `prize`도 함께 롤백해야 한다.
- `event_round_prize`만 삭제해도 `prize`는 삭제되지 않는다.
- `prize`, `event_round_prize`를 함께 없애려면 두 레코드를 각각 soft delete한다.
- 출석 회차에 보상 매핑이 없으면 point를 지급하지 않으며 `event_win`도 생성되지 않는다.
- 랜덤 리워드는 같은 회차에 여러 보상 정책을 세팅할 수 있다.
- 랜덤 리워드의 `weight` DB 기본값은 `1`이며, Java 코드에서는 `DEFAULT_WEIGHT` 같은 상수로 관리한다. 운영 정책에 따라 확률 계산 보조값으로 사용한다.
- 랜덤 리워드의 꽝/미지급 케이스는 `event_win` 행 없이 처리한다.
- 실제 출석 성공 1건에서 어떤 보상이 로컬 확정됐는지는 `event_win.event_round_prize_id`로 추적한다.
- 보상 추적과 집계는 `event_entry.event_round_prize_id`보다 `event_win.event_round_prize_id`를 기준으로 본다.
- `prize`는 immutable 정책으로 운영한다.
- `prize`를 불변 마스터로 운영하면 보상 snapshot 없이도 통계와 추적의 의미를 유지하기 쉽다.
- soft delete된 `prize`, `event_round_prize`는 현재 활성 설정 조회에서는 제외한다.
- 다만 과거 지급 이력 조회나 집계에서는 필요 시 soft delete된 row도 함께 참조할 수 있어야 한다.
- 현재 출석체크는 보상 매핑이 있으면 `event_applicant`, `event_entry`, `event_win`을 먼저 커밋하고, 외부 point API는 그 이후 호출한다.
- 조회 API는 `X-Member-Id` 유무에 따라 회차별 status/win 포함 여부가 달라진다.

## 관계와 사용 관점

### 현재 출석체크에서 직접 사용하는 테이블

- `event`
- `event_round`
- `event_applicant`
- `event_entry`
- `event_win`
- `event_round_prize`
- `prize`

### 현재 범위 밖이지만 나중에 연결될 테이블

- `event_round_prize_probability`

## 구현 연결 포인트

- Entity 후보: `Event`, `EventRound`, `EventApplicant`, `EventEntry`, `EventWin`, `EventRoundPrize`, `Prize`
- 조회 전략: 회차별 applicant 조회, event/member 기준 entry 조회, 요청의 `event_id`와 회차 정합성 검증, 출석 회차의 단일 prize 매핑 조회, 회원별 회차 상태 집계
- QueryDSL 필요 지점: 출석 이벤트 상세 조회, 회차별 상태 계산, entry 조회, win 조회, prize 조회, 지급된 point 이력 조회
- 동시성 위험 구간: 같은 회원의 같은 회차 applicant 동시 생성, 외부 API 재시도 중 중복 지급 위험, 추첨형 이벤트의 `is_winner` update 경쟁
- 스키마 리스크: 최소 FK를 적용하더라도 `event_applicant.event_id`, `event_entry.applicant_id`, `event_win.entry_id` 같은 값참조 정합성은 Service에서 검증해야 한다.
- 운영 리스크: `prize`를 수정 가능하게 사용하면 집계/추적 시점 의미가 흔들리므로 운영상 불변 정책이 필요하다.
- 연동 리스크: 현재는 외부 point API가 실패하거나 무응답이어도 로컬 출석과 `event_win`은 이미 커밋되므로 운영 보정 체계가 필요하다.
