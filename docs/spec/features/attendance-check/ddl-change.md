# DDL Change Draft

이 문서는 출석체크 spec에서 확정된 구조를 PostgreSQL DDL에 반영하기 위한 변경안을 정리한다. 현재는 신규 환경 기준이므로 최종 형태의 전체 DDL을 새로 만드는 방식을 우선 권장한다.

## 이번 변경의 목적

- FK는 두지 않고 Service 검증으로 참조 정합성을 보장해야 한다.
- 출석 이벤트는 `event_round` 기준으로 매일 출석 여부를 판정해야 한다.
- `event_applicant`는 eligibility 테이블이 아니라 회차별 applicant 기준 테이블이어야 한다.
- `event_applicant`는 `(event_id, round_id, member_id)` 기준으로 한 건만 생성되어야 한다.
- 출석체크는 회차당 보상 매핑이 `0..1`개여야 하고, 보상 매핑이 없으면 무보상 출석을 허용해야 한다.
- 따라서 `event_entry`가 어떤 회차에 대한 참여 이력인지 직접 알아야 한다.
- `event_entry`는 응모권 개념이므로 같은 회차에도 여러 건이 저장될 수 있어야 한다.
- 추첨형 이벤트에서는 `event_entry.is_winner`가 나중에 update될 수 있어야 한다.
- 동시에 회차 번호, 대상자, 출석 이력, 지급 결과 중복은 최소 unique로 막아야 한다.

## 이번 변경 범위

- 모든 FK 제거
- 최소 unique만 유지
- `event_applicant (event_id, round_id, member_id)` unique 추가
- `event_entry`에 `event_id` 추가
- `event_entry`에 `round_id` 추가
- `event_entry (event_id, round_id, member_id)` unique 제거
- `event_round_prize_probability.weight` DB 기본값 `1` 적용
- `event_round (event_id, round_no)` unique 유지
- `event_win (entry_id)` unique 유지

## 신규 환경 기준 권장안

- 신규 환경이면 `ALTER TABLE`보다 전체 스키마를 최종 형태로 한 번에 생성하는 편이 단순하다.
- 전체 SQL 초안은 `event-platform-schema-draft.sql`에 정리한다.
- 이 문서는 어떤 변경이 들어가는지 설명하는 보조 문서로 유지한다.

## 최종 목표 구조

### `event_applicant` 최종 정의

```sql
CREATE TABLE event.event_applicant (
    id         BIGINT      GENERATED ALWAYS AS IDENTITY,
    event_id   BIGINT      NOT NULL,
    round_id   BIGINT      NOT NULL,
    member_id  BIGINT      NOT NULL,
    is_deleted BOOLEAN     NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT      NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT      NOT NULL,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT pk_event_applicant PRIMARY KEY (id)
);

COMMENT ON TABLE  event.event_applicant IS '회차별 applicant 기준';
COMMENT ON COLUMN event.event_applicant.event_id IS '이벤트 식별자';
COMMENT ON COLUMN event.event_applicant.round_id IS '회차 식별자';
COMMENT ON COLUMN event.event_applicant.member_id IS '회원 식별자';
```

### `event_entry` 최종 정의

```sql
CREATE TABLE event.event_entry (
    id                   BIGINT      GENERATED ALWAYS AS IDENTITY,
    applicant_id         BIGINT      NOT NULL,
    event_id             BIGINT      NOT NULL,
    round_id             BIGINT      NOT NULL,
    member_id            BIGINT      NOT NULL,
    applied_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_round_prize_id BIGINT,
    is_winner            BOOLEAN     NOT NULL DEFAULT FALSE,
    is_deleted           BOOLEAN     NOT NULL DEFAULT FALSE,

    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           BIGINT      NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by           BIGINT      NOT NULL,
    deleted_at           TIMESTAMPTZ,

    CONSTRAINT pk_event_entry PRIMARY KEY (id)
);

COMMENT ON TABLE  event.event_entry IS '응모권/참여 이력';
COMMENT ON COLUMN event.event_entry.applicant_id IS 'applicant 식별자';
COMMENT ON COLUMN event.event_entry.event_id IS '이벤트 식별자';
COMMENT ON COLUMN event.event_entry.round_id IS '응모 회차 식별자';
COMMENT ON COLUMN event.event_entry.member_id IS '회원 식별자';
COMMENT ON COLUMN event.event_entry.applied_at IS '응모 일시';
COMMENT ON COLUMN event.event_entry.event_round_prize_id IS '당첨 경품 식별자 보조값 (NULL 가능, SoT 아님)';
COMMENT ON COLUMN event.event_entry.is_winner IS '당첨 여부 (추첨형 이벤트에서 update 가능)';
COMMENT ON COLUMN event.event_entry.is_deleted IS '논리 삭제 여부';
COMMENT ON COLUMN event.event_entry.created_at IS '등록 일시';
COMMENT ON COLUMN event.event_entry.created_by IS '등록자 식별자';
COMMENT ON COLUMN event.event_entry.updated_at IS '수정 일시';
COMMENT ON COLUMN event.event_entry.updated_by IS '수정자 식별자';
COMMENT ON COLUMN event.event_entry.deleted_at IS '삭제 일시';

```

### 최소 unique 목록

```sql
CREATE UNIQUE INDEX uq_event_round_event_round_no
    ON event.event_round (event_id, round_no)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uq_event_applicant_event_round_member
    ON event.event_applicant (event_id, round_id, member_id)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uq_event_win_entry_id
    ON event.event_win (entry_id)
    WHERE is_deleted = FALSE;
```

## 운영 중 스키마 변경용 참고 ALTER 안

신규 환경에서는 필요 없지만, 기존 운영 DB에 반영해야 할 경우를 위한 참고안이다.

### 1. 컬럼 추가

```sql
ALTER TABLE event.event_entry
    ADD COLUMN event_id BIGINT;

COMMENT ON COLUMN event.event_entry.event_id IS '이벤트 식별자 (조회/중복체크용 값참조)';

ALTER TABLE event.event_entry
    ADD COLUMN round_id BIGINT;

COMMENT ON COLUMN event.event_entry.round_id IS '출석/응모 회차 식별자';

```

### 2. 기존 데이터 backfill

```sql
-- 기존 데이터가 있다면 event_id, round_id를 채운 뒤 다음 단계로 진행한다.
-- 현재 출석 spec 기준으로는 기존 entry가 어떤 event/round에 속하는지 확정 가능한
-- backfill 규칙이 먼저 준비되어야 한다.
```

### 3. 필수값과 최소 unique 반영

```sql
ALTER TABLE event.event_entry
    ALTER COLUMN event_id SET NOT NULL;

ALTER TABLE event.event_entry
    ALTER COLUMN round_id SET NOT NULL;

DROP INDEX IF EXISTS event.uq_event_applicant_event_member_id;
DROP INDEX IF EXISTS event.uq_event_entry_event_round_member;

CREATE UNIQUE INDEX uq_event_applicant_event_round_member
    ON event.event_applicant (event_id, round_id, member_id)
    WHERE is_deleted = FALSE;
```

## 적용 메모

- 현재 조건은 신규 환경이므로 전체 스키마를 최종 목표 구조로 바로 생성하면 된다.
- 기존 데이터가 이미 있다면 `event_id`, `round_id` backfill 전략 없이 `NOT NULL`을 바로 추가하면 안 된다.
- `event_applicant`는 회차별 applicant 기준이며 출석 중복 제어의 기준이 된다.
- `event_entry`는 응모권/참여 이력이므로 같은 회차에도 여러 건이 저장될 수 있다.
- 추첨형 이벤트에서는 `event_entry.is_winner`를 나중에 update할 수 있다.
- `event_entry.event_round_prize_id`는 보조값이며 실제 지급 보상의 SoT는 `event_win.event_round_prize_id`다.
- FK가 없으므로 `round.event_id == event.id`, `event_applicant.event_id == event.id`, `event_win.entry_id == event_entry.id` 같은 참조 정합성은 Service에서 검증해야 한다.
- 출석 중복의 비즈니스 판정은 `event_applicant (event_id, round_id, member_id)` 기준으로 애플리케이션과 unique에서 함께 방어한다.
- 출석체크의 회차당 active `event_round_prize = 0..1` 제약은 현재 스키마만으로 직접 강제되지 않으므로 애플리케이션 또는 운영 검증이 필요하다.
- 랜덤 리워드는 같은 회차에 여러 `event_round_prize`와 확률 정책을 둘 수 있다.
