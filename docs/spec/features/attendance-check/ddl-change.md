# DDL Change Draft

이 문서는 출석체크 spec에서 확정된 구조를 PostgreSQL DDL에 반영하기 위한 변경안을 정리한다. 현재는 신규 환경 기준이므로 최종 형태의 전체 DDL을 새로 만드는 방식을 우선 권장한다.

## 이번 변경의 목적

- 스키마는 `promotion`을 사용한다.
- 시간 컬럼은 `TIMESTAMP`를 사용하고, 애플리케이션 해석 기준은 `Asia/Seoul`로 맞춘다.
- 전체 FK 제거가 아니라 업무상 꼭 필요한 최소 FK만 유지한다.
- 출석 이벤트는 `event_round` 기준으로 매일 출석 여부를 판정해야 한다.
- `event_applicant`는 사전 참여 가능 대상자 테이블이 아니라 회차별 applicant 기준 테이블이어야 한다.
- `event_applicant`는 `round_id + member_id` 기준으로 한 건만 생성되어야 한다.
- 출석체크는 회차당 보상 매핑이 `0..1`개여야 하고, 보상 매핑이 없으면 무보상 출석을 허용해야 한다.
- `event_entry`는 응모권 개념이므로 같은 회차에도 여러 건이 저장될 수 있어야 한다.
- `event_entry.round_id`는 제거하고 회차는 `event_entry.applicant_id -> event_applicant.round_id`로 파생한다.
- 추첨형 이벤트에서는 `event_entry.is_winner`가 나중에 update될 수 있어야 한다.

## 이번 변경 범위

- `CREATE SCHEMA IF NOT EXISTS promotion;`
- `event_round.event_id -> promotion.event.id` FK 추가
- `event_round_prize.round_id -> promotion.event_round.id` FK 추가
- `event_round_prize.prize_id -> promotion.prize.id` FK 추가
- `event_round_prize_probability.round_id -> promotion.event_round.id` FK 추가
- `event_round_prize_probability.event_round_prize_id -> promotion.event_round_prize.id` FK 추가
- `event_applicant.round_id -> promotion.event_round.id` FK 추가
- `event_applicant` unique를 `(event_id, round_id, member_id)`에서 `(round_id, member_id)`로 조정
- `event_entry.event_id`는 유지하고 `event_entry.round_id`는 제거
- `event_win.entry_id`는 unique만 유지하고 FK는 추가하지 않음

## 현재 합의 메모

- `event_applicant.event_id`는 조회용 값참조 컬럼으로 유지한다.
- 따라서 `event_applicant.event_id == event.id` 정합성은 여전히 Service에서 검증해야 한다.
- `event_entry.applicant_id`, `event_win.entry_id`도 이번 범위에서는 FK 대신 애플리케이션 검증 대상으로 둔다.
- `event_entry`의 회차는 `event_entry.applicant_id -> event_applicant.round_id`로 파생한다.
- `created_by`, `updated_by`는 `BIGINT`로 유지한다.
- system 처리도 문자열 `system`을 저장하지 않고 실행 주체로 해석하는 member id 값을 참조해 기록한다.

## 신규 환경 기준 권장안

- 신규 환경이면 `ALTER TABLE`보다 전체 스키마를 최종 형태로 한 번에 생성하는 편이 단순하다.
- 전체 SQL 초안은 `event-platform-schema-draft.sql`에 정리한다.
- 이 문서는 어떤 변경이 들어가는지 설명하는 보조 문서로 유지한다.

## 최종 목표 구조

### `event_round`

```sql
CREATE TABLE promotion.event_round
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    event_id       BIGINT      NOT NULL,
    round_no       INTEGER     NOT NULL,
    round_start_at TIMESTAMP,
    round_end_at   TIMESTAMP,
    draw_at        TIMESTAMP,
    is_confirmed   BOOLEAN     NOT NULL DEFAULT FALSE,
    is_deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     BIGINT      NOT NULL,
    updated_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     BIGINT      NOT NULL,
    deleted_at     TIMESTAMP,
    CONSTRAINT pk_event_round PRIMARY KEY (id),
    CONSTRAINT fk_event_round_event FOREIGN KEY (event_id)
        REFERENCES promotion.event (id)
);
COMMENT ON TABLE  promotion.event_round                IS '이벤트 회차 기준';
COMMENT ON COLUMN promotion.event_round.event_id       IS '이벤트 식별자';
COMMENT ON COLUMN promotion.event_round.round_no       IS '회차 번호';
COMMENT ON COLUMN promotion.event_round.round_start_at IS '회차 시작 일시';
COMMENT ON COLUMN promotion.event_round.round_end_at   IS '회차 종료 일시';
COMMENT ON COLUMN promotion.event_round.draw_at        IS '실제 추첨 실행 일시';
COMMENT ON COLUMN promotion.event_round.is_confirmed   IS '회차 결과 확정 여부';
COMMENT ON COLUMN promotion.event_round.is_deleted     IS '논리 삭제 여부';
COMMENT ON COLUMN promotion.event_round.created_at     IS '등록 일시';
COMMENT ON COLUMN promotion.event_round.created_by     IS '등록자 식별자';
COMMENT ON COLUMN promotion.event_round.updated_at     IS '수정 일시';
COMMENT ON COLUMN promotion.event_round.updated_by     IS '수정자 식별자';
COMMENT ON COLUMN promotion.event_round.deleted_at     IS '삭제 일시';
```

### `event_round_prize`

```sql
CREATE TABLE promotion.event_round_prize
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY,
    round_id    BIGINT      NOT NULL,
    prize_id    BIGINT      NOT NULL,
    priority    INTEGER     NOT NULL DEFAULT 0,
    daily_limit INTEGER,
    total_limit INTEGER,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  BIGINT      NOT NULL,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  BIGINT      NOT NULL,
    deleted_at  TIMESTAMP,
    CONSTRAINT pk_event_round_prize PRIMARY KEY (id),
    CONSTRAINT fk_event_round_prize_round FOREIGN KEY (round_id)
        REFERENCES promotion.event_round (id),
    CONSTRAINT fk_event_round_prize_prize FOREIGN KEY (prize_id)
        REFERENCES promotion.prize (id)
);
COMMENT ON TABLE  promotion.event_round_prize             IS '회차별 경품 정책';
COMMENT ON COLUMN promotion.event_round_prize.round_id    IS '회차 식별자';
COMMENT ON COLUMN promotion.event_round_prize.prize_id    IS '경품 식별자';
COMMENT ON COLUMN promotion.event_round_prize.priority    IS '우선순위 (낮을수록 우선)';
COMMENT ON COLUMN promotion.event_round_prize.daily_limit IS '일별 지급 상한 (NULL=무제한)';
COMMENT ON COLUMN promotion.event_round_prize.total_limit IS '총 지급 상한 (NULL=무제한)';
COMMENT ON COLUMN promotion.event_round_prize.is_active   IS '정책 활성 여부';
COMMENT ON COLUMN promotion.event_round_prize.is_deleted  IS '논리 삭제 여부';
COMMENT ON COLUMN promotion.event_round_prize.created_at  IS '등록 일시';
COMMENT ON COLUMN promotion.event_round_prize.created_by  IS '등록자 식별자';
COMMENT ON COLUMN promotion.event_round_prize.updated_at  IS '수정 일시';
COMMENT ON COLUMN promotion.event_round_prize.updated_by  IS '수정자 식별자';
COMMENT ON COLUMN promotion.event_round_prize.deleted_at  IS '삭제 일시';
```

### `event_round_prize_probability`

```sql
CREATE TABLE promotion.event_round_prize_probability
(
    id                   BIGINT GENERATED ALWAYS AS IDENTITY,
    round_id             BIGINT        NOT NULL,
    event_round_prize_id BIGINT        NOT NULL,
    probability          NUMERIC(5, 2) NOT NULL,
    weight               INTEGER        NOT NULL DEFAULT 1,
    is_active            BOOLEAN       NOT NULL DEFAULT TRUE,
    is_deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           BIGINT        NOT NULL,
    updated_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by           BIGINT        NOT NULL,
    deleted_at           TIMESTAMP,
    CONSTRAINT pk_event_round_prize_probability PRIMARY KEY (id),
    CONSTRAINT fk_event_round_prize_probability_round FOREIGN KEY (round_id)
        REFERENCES promotion.event_round (id),
    CONSTRAINT fk_event_round_prize_probability_round_prize FOREIGN KEY (event_round_prize_id)
        REFERENCES promotion.event_round_prize (id)
);
COMMENT ON TABLE  promotion.event_round_prize_probability                 IS '회차-경품 확률 정책';
COMMENT ON COLUMN promotion.event_round_prize_probability.round_id        IS '적용 회차 식별자';
COMMENT ON COLUMN promotion.event_round_prize_probability.event_round_prize_id IS '경품 정책 식별자';
COMMENT ON COLUMN promotion.event_round_prize_probability.probability     IS '당첨 확률 (0.00~100.00)';
COMMENT ON COLUMN promotion.event_round_prize_probability.weight          IS '가중치 (기본값 1)';
COMMENT ON COLUMN promotion.event_round_prize_probability.is_active       IS '정책 활성 여부';
COMMENT ON COLUMN promotion.event_round_prize_probability.is_deleted      IS '논리 삭제 여부';
COMMENT ON COLUMN promotion.event_round_prize_probability.created_at      IS '등록 일시';
COMMENT ON COLUMN promotion.event_round_prize_probability.created_by      IS '등록자 식별자';
COMMENT ON COLUMN promotion.event_round_prize_probability.updated_at      IS '수정 일시';
COMMENT ON COLUMN promotion.event_round_prize_probability.updated_by      IS '수정자 식별자';
COMMENT ON COLUMN promotion.event_round_prize_probability.deleted_at      IS '삭제 일시';
```

### `event_applicant`

```sql
CREATE TABLE promotion.event_applicant
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY,
    event_id   BIGINT      NOT NULL,
    round_id   BIGINT      NOT NULL,
    member_id  BIGINT      NOT NULL,
    is_deleted BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT      NOT NULL,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT      NOT NULL,
    deleted_at TIMESTAMP,
    CONSTRAINT pk_event_applicant PRIMARY KEY (id),
    CONSTRAINT fk_event_applicant_round FOREIGN KEY (round_id)
        REFERENCES promotion.event_round (id)
);
COMMENT ON TABLE  promotion.event_applicant            IS '참여자 기준';
COMMENT ON COLUMN promotion.event_applicant.event_id   IS '이벤트 식별자 (조회용 값참조)';
COMMENT ON COLUMN promotion.event_applicant.round_id   IS '회차 식별자';
COMMENT ON COLUMN promotion.event_applicant.member_id  IS '회원 식별자';
COMMENT ON COLUMN promotion.event_applicant.is_deleted IS '논리 삭제 여부';
COMMENT ON COLUMN promotion.event_applicant.created_at IS '등록 일시';
COMMENT ON COLUMN promotion.event_applicant.created_by IS '등록자 식별자';
COMMENT ON COLUMN promotion.event_applicant.updated_at IS '수정 일시';
COMMENT ON COLUMN promotion.event_applicant.updated_by IS '수정자 식별자';
COMMENT ON COLUMN promotion.event_applicant.deleted_at IS '삭제 일시';
```

### `event_entry`

```sql
CREATE TABLE promotion.event_entry
(
    id                   BIGINT GENERATED ALWAYS AS IDENTITY,
    applicant_id         BIGINT      NOT NULL,
    event_id             BIGINT      NOT NULL,
    member_id            BIGINT      NOT NULL,
    applied_at           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_round_prize_id BIGINT,
    is_winner            BOOLEAN     NOT NULL DEFAULT FALSE,
    is_deleted           BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           BIGINT      NOT NULL,
    updated_at           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by           BIGINT      NOT NULL,
    deleted_at           TIMESTAMP,
    CONSTRAINT pk_event_entry PRIMARY KEY (id)
);
COMMENT ON TABLE  promotion.event_entry                     IS '응모권/참여 이력';
COMMENT ON COLUMN promotion.event_entry.applicant_id        IS 'applicant 식별자';
COMMENT ON COLUMN promotion.event_entry.event_id            IS '이벤트 식별자';
COMMENT ON COLUMN promotion.event_entry.member_id           IS '회원 식별자';
COMMENT ON COLUMN promotion.event_entry.applied_at          IS '응모 일시';
COMMENT ON COLUMN promotion.event_entry.event_round_prize_id IS '당첨 경품 식별자 보조값 (NULL 가능, SoT 아님)';
COMMENT ON COLUMN promotion.event_entry.is_winner           IS '당첨 여부 (추첨형 이벤트에서 update 가능)';
COMMENT ON COLUMN promotion.event_entry.is_deleted          IS '논리 삭제 여부';
COMMENT ON COLUMN promotion.event_entry.created_at          IS '등록 일시';
COMMENT ON COLUMN promotion.event_entry.created_by          IS '등록자 식별자';
COMMENT ON COLUMN promotion.event_entry.updated_at          IS '수정 일시';
COMMENT ON COLUMN promotion.event_entry.updated_by          IS '수정자 식별자';
COMMENT ON COLUMN promotion.event_entry.deleted_at          IS '삭제 일시';
```

### 최소 unique 및 조회 index 목록

```sql
CREATE UNIQUE INDEX uq_event_round_event_round_no
    ON promotion.event_round (event_id, round_no)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uq_event_applicant_round_member_id
    ON promotion.event_applicant (round_id, member_id)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uq_event_win_entry_id
    ON promotion.event_win (entry_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_event_entry_event_member
    ON promotion.event_entry (event_id, member_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_event_entry_applicant_id
    ON promotion.event_entry (applicant_id)
    WHERE is_deleted = FALSE;
```

## 운영 중 스키마 변경용 참고 ALTER 안

신규 환경에서는 필요 없지만, 기존 운영 DB에 반영해야 할 경우를 위한 참고안이다.

### 1. 스키마 및 FK 정리

```sql
CREATE SCHEMA IF NOT EXISTS promotion;

ALTER TABLE promotion.event_round
    ADD CONSTRAINT fk_event_round_event
        FOREIGN KEY (event_id) REFERENCES promotion.event (id);

ALTER TABLE promotion.event_round_prize
    ADD CONSTRAINT fk_event_round_prize_round
        FOREIGN KEY (round_id) REFERENCES promotion.event_round (id);

ALTER TABLE promotion.event_round_prize
    ADD CONSTRAINT fk_event_round_prize_prize
        FOREIGN KEY (prize_id) REFERENCES promotion.prize (id);

ALTER TABLE promotion.event_round_prize_probability
    ADD CONSTRAINT fk_event_round_prize_probability_round
        FOREIGN KEY (round_id) REFERENCES promotion.event_round (id);

ALTER TABLE promotion.event_round_prize_probability
    ADD CONSTRAINT fk_event_round_prize_probability_round_prize
        FOREIGN KEY (event_round_prize_id) REFERENCES promotion.event_round_prize (id);

ALTER TABLE promotion.event_applicant
    ADD CONSTRAINT fk_event_applicant_round
        FOREIGN KEY (round_id) REFERENCES promotion.event_round (id);
```

### 2. applicant unique 조정

```sql
DROP INDEX IF EXISTS promotion.uq_event_applicant_event_round_member;

CREATE UNIQUE INDEX uq_event_applicant_round_member_id
    ON promotion.event_applicant (round_id, member_id)
    WHERE is_deleted = FALSE;
```

## 적용 메모

- 현재 조건은 신규 환경이므로 전체 스키마를 최종 목표 구조로 바로 생성하면 된다.
- `event_applicant`는 회차별 applicant 기준이며 출석 중복 제어의 기준이 된다.
- 출석 중복의 비즈니스 판정은 `event_applicant (round_id, member_id)` 기준으로 애플리케이션과 unique에서 함께 방어한다.
- `event_entry`의 회차는 `event_entry.applicant_id -> event_applicant.round_id`로 파생한다.
- `event_entry.event_round_prize_id`는 보조값이며 로컬 보상 확정의 SoT는 `event_win.event_round_prize_id`다.
- `event_applicant.event_id`, `event_entry.applicant_id`, `event_win.entry_id` 같은 값참조 정합성은 Service에서 계속 검증해야 한다.
- 출석체크의 회차당 active `event_round_prize = 0..1` 제약은 현재 스키마만으로 직접 강제되지 않으므로 애플리케이션 또는 운영 검증이 필요하다.
