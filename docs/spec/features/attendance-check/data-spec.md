# Data Spec

이 문서는 현재 제공된 PostgreSQL DDL을 출석체크 관점으로 해석한 결과다. 현재 도메인 설명까지 반영하면 참여 가능 대상 정보는 `event_applicant`, 실제 참여 이력은 `event_entry`, 실제 지급 보상 이력은 `event_win`, 출석 단위는 `event_round`로 보는 것이 맞다.

## 출석체크 핵심 흐름

1. `event`에서 출석체크 이벤트를 찾는다.
2. `event_round`에서 대상 날짜의 회차를 `id + event_id` 조건으로 찾는다.
3. FK가 없으므로 `round.event_id == event.id`인지 반드시 검증한다.
4. `event_applicant`에서 해당 사용자의 eligibility를 조회하고, `round_id`가 있으면 요청 회차와도 맞는지 확인한다.
5. 출석 회차에 연결된 단일 point 보상 매핑이 있는지 확인한다.
6. `event_entry`를 저장한다.
7. 보상 매핑이 있으면 외부 point API 호출 성공 후 `event_win`을 저장하고 함께 커밋한다.
8. 보상 매핑이 없으면 `event_entry`만 커밋한다.

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
| ATT-DATA-001 | 출석 이벤트 마스터 | `event_platform.event` | `id`, `event_type`, `supplier_id`, `start_at`, `end_at`, `is_active`, `is_visible`, `is_deleted`, `is_multiple_entry`, `is_auto_entry` | ATT-REQ-001 | DDL 반영 | 출석체크 이벤트는 `event_type = 'ATTENDANCE'`로 식별 |
| ATT-DATA-002 | 출석 회차 | `event_platform.event_round` | `id`, `event_id`, `round_no`, `round_start_at`, `round_end_at`, `is_deleted` | ATT-REQ-001, ATT-REQ-003 | DDL 반영 | 월간 출석 이벤트는 날짜 수만큼 회차 생성 |
| ATT-DATA-003 | 참여 가능 대상자 풀 | `event_platform.event_applicant` | `id`, `event_id`, `round_id`, `member_id`, `is_deleted` | ATT-REQ-002, ATT-REQ-005 | 업무 확정, schema draft 반영 | `(event_id, member_id)` 최소 unique, `round_id`는 선택값 |
| ATT-DATA-004 | 참여 이력 | `event_platform.event_entry` | `id`, `applicant_id`, `event_id`, `round_id`, `member_id`, `applied_at`, `event_round_prize_id`, `is_winner`, `is_deleted` | ATT-REQ-002, ATT-REQ-004, ATT-REQ-005 | 업무 확정, schema draft 반영 | 출석 이벤트에서 일별 출석 여부 판단을 위해 `event_id`, `round_id`가 필요 |
| ATT-DATA-005 | 회차 번호 유일성 | `uq_event_round_event_round_no` | `(event_id, round_no)` | ATT-REQ-001 | DDL 반영 | 이벤트 내 회차 번호 중복 방지 |
| ATT-DATA-006 | applicant 고유성 요구사항 | 출석 spec 기준 | `(event_id, member_id)` | ATT-REQ-002 | 업무 확정, schema draft 반영 | 최소 unique 기준 |
| ATT-DATA-007 | 출석 중복 판정 기준 | 출석 spec 기준 | `(event_id, round_id, member_id)` | ATT-REQ-003 | 업무 확정, schema draft 반영 | 비즈니스 기준이자 최소 DB unique 기준 |
| ATT-DATA-008 | 보상 마스터 | `event_platform.prize` | `id`, `prize_name`, `reward_type`, `point_amount`, `coupon_id`, `is_active`, `is_deleted` | ATT-REQ-005 | DDL 반영 | 출석체크는 주로 `POINT` 유형 사용 |
| ATT-DATA-009 | 회차별 보상 연결 | `event_platform.event_round_prize` | `id`, `round_id`, `prize_id`, `priority`, `daily_limit`, `total_limit`, `is_active`, `is_deleted` | ATT-REQ-005 | DDL 반영 | 출석체크는 회차당 `0..1`, 랜덤 리워드는 회차당 여러 prize 정책 가능 |
| ATT-DATA-010 | 보상 결과 이력 | `event_platform.event_win` | `id`, `entry_id`, `round_id`, `event_id`, `member_id`, `event_round_prize_id`, `is_deleted` | ATT-REQ-002, ATT-REQ-004, ATT-REQ-005, ATT-REQ-006 | DDL 반영 | 실제 보상이 지급된 경우에만 생성 |
| ATT-DATA-011 | 확률 기반 보상 확장 경로 | `event_platform.event_round_prize_probability` | `id`, `round_id`, `event_round_prize_id`, `probability`, `weight` | ATT-REQ-005 | DDL 반영 | 랜덤 리워드에서 여러 보상 중 하나를 계산할 때 사용 |
| ATT-DATA-012 | 감사/삭제 정보 | `event`, `event_round`, `event_applicant`, `event_entry`, `event_win`, `prize`, `event_round_prize` | `created_at`, `created_by`, `updated_at`, `updated_by`, `deleted_at`, `is_deleted` | ATT-REQ-005, ATT-REQ-006 | DDL 반영 | 전 테이블 공통 감사 패턴 |

## 현재 해석에서 중요한 포인트

- `event_round`는 일별 출석 단위다.
- `event.supplier_id`는 외부 값참조 식별자이며 현재는 위드 DB 값을 사용한다.
- 이후 버터가 쇼핑몰 외주 개발을 완료하면 `supplier_id`도 위드 데이터를 버터 DB로 마이그레이션해 사용할 예정이다.
- 현재 `supplier_id` 용도는 돌쇠네 자체 서비스 범위다.
- `event_applicant`는 이벤트 참여 가능 대상자 풀이며 `(event_id, member_id)` 기준으로 식별한다.
- 참여 조건이나 대상자 판정 결과를 사전에 적재해두고, 이벤트 기간 조건을 만족하는 상태에서 `event_applicant` 존재 여부로 참여 가능 여부를 빠르게 판정한다.
- `event_applicant.round_id`는 선택값이며 특정 회차에만 참여 가능하도록 제한할 때 사용한다.
- `event_entry`는 실제 참여 이력 테이블이며, 출석 성공과 향후 랜덤 리워드 참여 기록이 누적된다.
- `event_entry.applicant_id`는 어떤 eligibility 레코드를 기준으로 참여했는지 연결한다.
- 출석 이벤트에서 `event_entry`는 매일 출석 여부를 판정하기 위해 `event_id`, `round_id`를 가져야 한다.
- `event_win`은 실제 지급된 보상과 외부 보상 API 성공 이력을 저장한다.
- 출석 중복 여부의 비즈니스 기준은 `(event_id, round_id, member_id)`다.
- 신규 환경용 schema draft는 FK 없이 최소 unique만 둔다.
- 최소 unique 대상은 `uq_event_round_event_round_no`, `uq_event_applicant_event_member_id`, `uq_event_entry_event_round_member`, `uq_event_win_entry_id`다.
- 제공된 원본 DDL은 `event_entry.event_id`, `event_entry.round_id`가 없어 API 계약을 그대로 구현하기 어려웠다.
- `event_applicant.event_id`는 값참조이며 FK가 아니므로 `round_id`와 `event_id` 정합성은 애플리케이션에서 보장해야 한다.
- 현재 프로젝트는 `event_applicant` 관리 API를 제공하지 않으므로 대상자 데이터는 SQL 또는 별도 admin 프로젝트에서 적재한다.
- 출석 보상은 `event_round_prize -> prize` 연결로 관리하며, 출석체크에서는 회차당 보상 매핑을 `0..1`개만 둔다.
- 출석 회차에 보상 매핑이 없으면 point를 지급하지 않으며 `event_win`도 생성되지 않는다.
- 랜덤 리워드는 같은 회차에 여러 보상 정책을 세팅할 수 있다.
- 실제 출석 성공 1건에서 어떤 보상이 지급됐는지는 `event_win.event_round_prize_id`로 추적한다.
- `prize`는 immutable 정책으로 운영한다.
- `prize`를 불변 마스터로 운영하면 보상 snapshot 없이도 통계와 추적의 의미를 유지하기 쉽다.
- 현재 출석체크는 보상 매핑이 있으면 `event_entry` 저장 후 외부 point API를 호출하고, 성공 시 `event_win`까지 함께 커밋된다.
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
- 조회 전략: 이벤트별 eligibility applicant 조회, `(event_id, round_id, member_id)` 기준 entry 조회, 요청의 `event_id`와 회차 정합성 검증, 출석 회차의 단일 prize 매핑 조회, 회원별 회차 상태 집계
- QueryDSL 필요 지점: 출석 이벤트 상세 조회, 회차별 상태 계산, entry/win 조인 조회, 지급된 point 이력 조회
- 동시성 위험 구간: 같은 회원의 같은 회차 출석 동시 요청, eligibility 데이터 중복 적재 가능성, 외부 API 재시도 중 중복 지급 위험
- 스키마 리스크: FK 없이 운영하므로 `event_id`, `round_id`, `entry_id` 사이의 참조 정합성은 모두 Service에서 검증해야 한다.
- 운영 리스크: `prize`를 수정 가능하게 사용하면 집계/추적 시점 의미가 흔들리므로 운영상 불변 정책이 필요하다.
- 연동 리스크: 현재는 보상 매핑이 있는 출석에서 외부 point API가 실패하거나 무응답이면 출석 전체가 실패한다.
