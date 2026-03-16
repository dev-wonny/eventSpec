# Data Spec

이 문서는 현재 제공된 PostgreSQL DDL을 출석체크 관점으로 해석한 결과다. 현재 도메인 설명까지 반영하면 참여 이력은 `event_entry`, 참여자 앵커는 `event_applicant`, 실제 지급 보상 이력은 `event_win`, 출석 단위는 `event_round`로 보는 것이 맞다.

## 출석체크 핵심 흐름

1. `event`에서 출석체크 이벤트를 찾는다.
2. `event_round`에서 대상 날짜의 회차를 찾는다.
3. `event_applicant`에서 기존 참여자 앵커를 조회하거나 첫 참여 시 생성한다.
4. 회차에 연결된 point 보상 정책을 `event_round_prize`에서 찾는다.
5. 외부 point API 호출 성공 후 `event_entry`, `event_win`을 함께 커밋한다.

## 핵심 객체 매핑

| ID | 논리 개념 | 물리 객체 | 핵심 컬럼 | 관련 요구사항 | 확인 상태 | 메모 |
| --- | --- | --- | --- | --- | --- | --- |
| ATT-DATA-001 | 출석 이벤트 마스터 | `event_platform.event` | `id`, `event_type`, `start_at`, `end_at`, `is_active`, `is_visible`, `is_deleted`, `is_multiple_entry`, `is_auto_entry` | ATT-REQ-001 | DDL 반영 | 출석체크 이벤트는 `event_type = 'ATTENDANCE'`로 식별 |
| ATT-DATA-002 | 출석 회차 | `event_platform.event_round` | `id`, `event_id`, `round_no`, `round_start_at`, `round_end_at`, `is_deleted` | ATT-REQ-001, ATT-REQ-003 | DDL 반영 | 월간 출석 이벤트는 날짜 수만큼 회차 생성 |
| ATT-DATA-003 | 참여자 앵커 | `event_platform.event_applicant` | `id`, `event_id`, `round_id`, `member_id`, `is_deleted` | ATT-REQ-002, ATT-REQ-005 | DDL 반영(해석 주의) | 첫 참여 시 생성 후 재사용하는 레코드로 해석 |
| ATT-DATA-004 | 참여 이력 | `event_platform.event_entry` | `id`, `applicant_id`, `member_id`, `applied_at`, `event_round_prize_id`, `is_winner`, `is_deleted` | ATT-REQ-002, ATT-REQ-004, ATT-REQ-005 | DDL 반영 | 출석 성공 및 향후 랜덤 리워드 참여 이력을 누적 |
| ATT-DATA-005 | 회차 번호 유일성 | `uq_event_round_event_round_no` | `(event_id, round_no)` | ATT-REQ-001 | DDL 반영 | 이벤트 내 회차 번호 중복 방지 |
| ATT-DATA-006 | applicant 중복 방지 | `uq_event_applicant_round_member_id` | `(round_id, member_id)` | ATT-REQ-002 | DDL 반영(용도 불일치 가능) | 같은 회차 내 applicant 중복만 방지, 이벤트 단위 앵커 고유성은 보장하지 않음 |
| ATT-DATA-007 | 일별 출석 중복 판정 기준 | `event_platform.event_entry` | `applicant_id`, `applied_at`, `is_deleted` | ATT-REQ-003 | DDL 반영(애플리케이션 판정) | 오늘 날짜 기준 `event_entry` 존재 여부로 이미 출석 판단 |
| ATT-DATA-008 | 보상 마스터 | `event_platform.prize` | `id`, `prize_name`, `reward_type`, `point_amount`, `coupon_id`, `is_active`, `is_deleted` | ATT-REQ-005 | DDL 반영 | 출석체크는 주로 `POINT` 유형 사용 |
| ATT-DATA-009 | 회차별 보상 연결 | `event_platform.event_round_prize` | `id`, `round_id`, `prize_id`, `priority`, `daily_limit`, `total_limit`, `is_active`, `is_deleted` | ATT-REQ-005 | DDL 반영 | 회차별로 어떤 prize를 적용할지 연결 |
| ATT-DATA-010 | 보상 결과 이력 | `event_platform.event_win` | `id`, `entry_id`, `round_id`, `event_id`, `member_id`, `event_round_prize_id`, `is_deleted` | ATT-REQ-002, ATT-REQ-004, ATT-REQ-005, ATT-REQ-006 | DDL 반영 | point 지급 성공 및 향후 실제 보상 결과 이력 |
| ATT-DATA-011 | 확률 기반 보상 확장 경로 | `event_platform.event_round_prize_probability` | `id`, `round_id`, `event_round_prize_id`, `probability`, `weight` | ATT-REQ-005 | DDL 반영 | 랜덤 보상 확장 시 사용 |
| ATT-DATA-012 | 감사/삭제 정보 | `event`, `event_round`, `event_applicant`, `event_entry`, `event_win`, `prize`, `event_round_prize` | `created_at`, `created_by`, `updated_at`, `updated_by`, `deleted_at`, `is_deleted` | ATT-REQ-005, ATT-REQ-006 | DDL 반영 | 전 테이블 공통 감사 패턴 |

## 현재 해석에서 중요한 포인트

- `event_round`는 일별 출석 단위다.
- `event_entry`는 참여 이력 테이블이며, 출석 성공과 향후 랜덤 리워드 참여 기록이 누적된다.
- `event_win`은 실제 지급된 보상과 외부 보상 API 성공 이력을 저장한다.
- `event_entry`가 `round_id`를 직접 저장하지 않으므로, 참여 엔트리와 회차의 연결은 요청값 또는 `applied_at` 기준 계산으로 보완해야 한다.
- 출석 중복 여부는 현재 업무 기준으로 오늘 날짜의 `event_entry` 존재 여부로 판단한다.
- `event_applicant.event_id`는 값참조이며 FK가 아니므로 `round_id`와 `event_id` 정합성은 애플리케이션에서 보장해야 한다.
- `event_applicant`를 이벤트 단위 참여자 앵커로 재사용하려면 `(event_id, member_id)` 기준 조회 전략이 필요하다.
- 출석 보상은 `event_round_prize -> prize` 연결로 관리하며, 현재 업무 기준으로는 point 보상이 중심이다.
- `prize`를 불변 마스터로 운영하면 보상 snapshot 없이도 통계와 추적의 의미를 유지하기 쉽다.
- 현재 출석체크는 외부 point API 성공 시에만 `event_entry`, `event_win`이 함께 커밋된다.

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
- 조회 전략: 현재 활성 회차 조회, 이벤트별 기존 applicant 조회, 오늘 날짜 entry 존재 여부 조회, 회차별 point prize 조회
- QueryDSL 필요 지점: 활성 이벤트/회차 탐색, 오늘 날짜 기준 중복 출석 판정, 출석 결과 조회, 지급된 point 이력 조회
- 동시성 위험 구간: 같은 회원의 같은 회차 출석 동시 요청, applicant 중복 생성 가능성, 외부 API 재시도 중 중복 지급 위험
- 스키마 리스크: 현재 DDL만으로는 이벤트 단위 applicant 고유성과 회차 단위 entry 고유성이 직접 강제되지 않는다.
- 운영 리스크: `prize`를 수정 가능하게 사용하면 집계/추적 시점 의미가 흔들리므로 운영상 불변 정책이 필요하다.
- 연동 리스크: 현재는 외부 point API가 실패하거나 무응답이면 출석 전체가 실패한다.
