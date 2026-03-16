-- ============================================================
-- Event Platform Schema DDL Draft
-- Target   : PostgreSQL 17
-- Schema   : event_platform
-- Encoding : UTF-8
-- Note     : 신규 환경 기준 전체 생성용 초안
-- Confirmed change:
--  - event_applicant (event_id, member_id) unique 추가
--  - event_applicant.round_id nullable 조정
--  - event_entry.event_id 추가
--  - event_entry.round_id 추가
--  - event_entry (event_id, round_id, member_id) 조회 index 추가
-- ============================================================

CREATE SCHEMA IF NOT EXISTS event_platform;
SET search_path TO event_platform, public;

-- ============================================================
-- [1] prize
-- 역할: 재사용 가능한 경품 마스터
-- ============================================================
CREATE TABLE event_platform.prize (
    id                BIGINT       GENERATED ALWAYS AS IDENTITY,
    prize_name        VARCHAR(100) NOT NULL,
    reward_type       VARCHAR(20)  NOT NULL,
    point_amount      INTEGER,
    coupon_id         BIGINT,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    prize_description TEXT,

    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        BIGINT       NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        BIGINT       NOT NULL,
    deleted_at        TIMESTAMPTZ,

    CONSTRAINT pk_prize PRIMARY KEY (id)
);

COMMENT ON TABLE  event_platform.prize                   IS '경품 마스터';
COMMENT ON COLUMN event_platform.prize.prize_name        IS '경품명';
COMMENT ON COLUMN event_platform.prize.reward_type       IS '보상 유형 (공통코드: PRIZE_TYPE[POINT, COUPON, PRODUCT, ETC])';
COMMENT ON COLUMN event_platform.prize.point_amount      IS '포인트 지급액 (POINT 전용)';
COMMENT ON COLUMN event_platform.prize.coupon_id         IS '쿠폰 식별자 (외부 참조)';
COMMENT ON COLUMN event_platform.prize.is_active         IS '활성 여부';
COMMENT ON COLUMN event_platform.prize.is_deleted        IS '논리 삭제 여부';
COMMENT ON COLUMN event_platform.prize.prize_description IS '경품 설명';
COMMENT ON COLUMN event_platform.prize.created_at        IS '등록 일시';
COMMENT ON COLUMN event_platform.prize.created_by        IS '등록자 식별자';
COMMENT ON COLUMN event_platform.prize.updated_at        IS '수정 일시';
COMMENT ON COLUMN event_platform.prize.updated_by        IS '수정자 식별자';
COMMENT ON COLUMN event_platform.prize.deleted_at        IS '삭제 일시';

-- ============================================================
-- [2] event
-- 역할: 이벤트 기본 정보 + 공통 운영 정책
-- ============================================================
CREATE TABLE event_platform.event (
    id                       BIGINT       GENERATED ALWAYS AS IDENTITY,
    event_name               VARCHAR(100) NOT NULL,
    event_type               VARCHAR(30)  NOT NULL,
    start_at                 TIMESTAMPTZ  NOT NULL,
    end_at                   TIMESTAMPTZ  NOT NULL,
    supplier_id              BIGINT       NOT NULL,
    event_url                VARCHAR(300),
    winner_selection_cycle   INTEGER,
    winner_selection_base_at TIMESTAMPTZ,
    priority                 INTEGER      NOT NULL DEFAULT 0,

    is_active                BOOLEAN      NOT NULL DEFAULT FALSE,
    is_visible               BOOLEAN      NOT NULL DEFAULT FALSE,
    is_deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    is_auto_entry            BOOLEAN      NOT NULL DEFAULT FALSE,
    is_sns_linked            BOOLEAN      NOT NULL DEFAULT FALSE,
    is_winner_announced      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_duplicate_winner      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_multiple_entry        BOOLEAN      NOT NULL DEFAULT FALSE,
    description              TEXT,

    created_at               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               BIGINT       NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by               BIGINT       NOT NULL,
    deleted_at               TIMESTAMPTZ,

    CONSTRAINT pk_event PRIMARY KEY (id)
);

COMMENT ON TABLE  event_platform.event                           IS '이벤트 기본 정보 및 운영 정책';
COMMENT ON COLUMN event_platform.event.event_name                IS '이벤트명';
COMMENT ON COLUMN event_platform.event.event_type                IS '이벤트 유형 (공통코드: EVENT_TYPE[ATTENDANCE, RANDOM_REWARD])';
COMMENT ON COLUMN event_platform.event.start_at                  IS '이벤트 시작 일시';
COMMENT ON COLUMN event_platform.event.end_at                    IS '이벤트 종료 일시';
COMMENT ON COLUMN event_platform.event.supplier_id               IS '공급사 식별자 (외부 값참조, 현재 위드 DB 기준)';
COMMENT ON COLUMN event_platform.event.event_url                 IS '이벤트 URL';
COMMENT ON COLUMN event_platform.event.winner_selection_cycle    IS '당첨자 선정 주기 (시간)';
COMMENT ON COLUMN event_platform.event.winner_selection_base_at  IS '당첨자 선정 기준 일시';
COMMENT ON COLUMN event_platform.event.priority                  IS '전시 우선순위 (낮을수록 우선)';
COMMENT ON COLUMN event_platform.event.is_active                 IS '활성 여부';
COMMENT ON COLUMN event_platform.event.is_visible                IS '전시 여부';
COMMENT ON COLUMN event_platform.event.is_deleted                IS '논리 삭제 여부';
COMMENT ON COLUMN event_platform.event.is_auto_entry             IS '자동 응모 여부';
COMMENT ON COLUMN event_platform.event.is_sns_linked             IS 'SNS 연동 여부';
COMMENT ON COLUMN event_platform.event.is_winner_announced       IS '당첨자 발표 여부';
COMMENT ON COLUMN event_platform.event.is_duplicate_winner       IS '중복 당첨 허용 여부';
COMMENT ON COLUMN event_platform.event.is_multiple_entry         IS '복수 응모 허용 여부';
COMMENT ON COLUMN event_platform.event.description               IS '이벤트 설명';
COMMENT ON COLUMN event_platform.event.created_at                IS '등록 일시';
COMMENT ON COLUMN event_platform.event.created_by                IS '등록자 식별자';
COMMENT ON COLUMN event_platform.event.updated_at                IS '수정 일시';
COMMENT ON COLUMN event_platform.event.updated_by                IS '수정자 식별자';
COMMENT ON COLUMN event_platform.event.deleted_at                IS '삭제 일시';

-- ============================================================
-- [3] event_round
-- 역할: 이벤트 회차 기준
-- 관계: event (1) -> event_round (N)
-- ============================================================
CREATE TABLE event_platform.event_round (
    id             BIGINT      GENERATED ALWAYS AS IDENTITY,
    event_id       BIGINT      NOT NULL,
    round_no       INTEGER     NOT NULL,
    round_start_at TIMESTAMPTZ,
    round_end_at   TIMESTAMPTZ,
    draw_at        TIMESTAMPTZ,
    is_confirmed   BOOLEAN     NOT NULL DEFAULT FALSE,
    is_deleted     BOOLEAN     NOT NULL DEFAULT FALSE,

    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     BIGINT      NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     BIGINT      NOT NULL,
    deleted_at     TIMESTAMPTZ,

    CONSTRAINT pk_event_round       PRIMARY KEY (id),
    CONSTRAINT fk_event_round_event FOREIGN KEY (event_id)
        REFERENCES event_platform.event(id)
);

COMMENT ON TABLE  event_platform.event_round                IS '이벤트 회차 기준';
COMMENT ON COLUMN event_platform.event_round.event_id       IS '이벤트 식별자';
COMMENT ON COLUMN event_platform.event_round.round_no       IS '회차 번호';
COMMENT ON COLUMN event_platform.event_round.round_start_at IS '회차 시작 일시';
COMMENT ON COLUMN event_platform.event_round.round_end_at   IS '회차 종료 일시';
COMMENT ON COLUMN event_platform.event_round.draw_at        IS '실제 추첨 실행 일시';
COMMENT ON COLUMN event_platform.event_round.is_confirmed   IS '회차 결과 확정 여부';
COMMENT ON COLUMN event_platform.event_round.is_deleted     IS '논리 삭제 여부';
COMMENT ON COLUMN event_platform.event_round.created_at     IS '등록 일시';
COMMENT ON COLUMN event_platform.event_round.created_by     IS '등록자 식별자';
COMMENT ON COLUMN event_platform.event_round.updated_at     IS '수정 일시';
COMMENT ON COLUMN event_platform.event_round.updated_by     IS '수정자 식별자';
COMMENT ON COLUMN event_platform.event_round.deleted_at     IS '삭제 일시';

-- ============================================================
-- [4] event_round_prize
-- 역할: 회차별 경품 정책
-- 관계: event_round (1) -> event_round_prize (N)
--       prize (1) -> event_round_prize (N)
-- ============================================================
CREATE TABLE event_platform.event_round_prize (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY,
    round_id    BIGINT      NOT NULL,
    prize_id    BIGINT      NOT NULL,
    priority    INTEGER     NOT NULL DEFAULT 0,
    daily_limit INTEGER,
    total_limit INTEGER,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  BIGINT      NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  BIGINT      NOT NULL,
    deleted_at  TIMESTAMPTZ,

    CONSTRAINT pk_event_round_prize       PRIMARY KEY (id),
    CONSTRAINT fk_event_round_prize_round FOREIGN KEY (round_id)
        REFERENCES event_platform.event_round(id),
    CONSTRAINT fk_event_round_prize_prize FOREIGN KEY (prize_id)
        REFERENCES event_platform.prize(id)
);

COMMENT ON TABLE  event_platform.event_round_prize             IS '회차별 경품 정책';
COMMENT ON COLUMN event_platform.event_round_prize.round_id    IS '회차 식별자';
COMMENT ON COLUMN event_platform.event_round_prize.prize_id    IS '경품 식별자';
COMMENT ON COLUMN event_platform.event_round_prize.priority    IS '우선순위 (낮을수록 우선)';
COMMENT ON COLUMN event_platform.event_round_prize.daily_limit IS '일별 지급 상한 (NULL=무제한)';
COMMENT ON COLUMN event_platform.event_round_prize.total_limit IS '총 지급 상한 (NULL=무제한)';
COMMENT ON COLUMN event_platform.event_round_prize.is_active   IS '정책 활성 여부';
COMMENT ON COLUMN event_platform.event_round_prize.is_deleted  IS '논리 삭제 여부';
COMMENT ON COLUMN event_platform.event_round_prize.created_at  IS '등록 일시';
COMMENT ON COLUMN event_platform.event_round_prize.created_by  IS '등록자 식별자';
COMMENT ON COLUMN event_platform.event_round_prize.updated_at  IS '수정 일시';
COMMENT ON COLUMN event_platform.event_round_prize.updated_by  IS '수정자 식별자';
COMMENT ON COLUMN event_platform.event_round_prize.deleted_at  IS '삭제 일시';

-- ============================================================
-- [5] event_round_prize_probability
-- 역할: 회차-경품 확률 정책
-- ============================================================
CREATE TABLE event_platform.event_round_prize_probability (
    id                   BIGINT       GENERATED ALWAYS AS IDENTITY,
    round_id             BIGINT       NOT NULL,
    event_round_prize_id BIGINT       NOT NULL,
    probability          NUMERIC(5,2) NOT NULL,
    weight               INTEGER,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           BIGINT       NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by           BIGINT       NOT NULL,
    deleted_at           TIMESTAMPTZ,

    CONSTRAINT pk_event_round_prize_probability PRIMARY KEY (id),
    CONSTRAINT fk_event_round_prize_probability_round FOREIGN KEY (round_id)
        REFERENCES event_platform.event_round(id),
    CONSTRAINT fk_event_round_prize_probability_round_prize FOREIGN KEY (event_round_prize_id)
        REFERENCES event_platform.event_round_prize(id)
);

COMMENT ON TABLE  event_platform.event_round_prize_probability IS '회차-경품 확률 정책';
COMMENT ON COLUMN event_platform.event_round_prize_probability.round_id IS '적용 회차 식별자';
COMMENT ON COLUMN event_platform.event_round_prize_probability.event_round_prize_id IS '경품 정책 식별자';
COMMENT ON COLUMN event_platform.event_round_prize_probability.probability IS '당첨 확률 (0.00~100.00)';
COMMENT ON COLUMN event_platform.event_round_prize_probability.weight IS '가중치 (선택)';
COMMENT ON COLUMN event_platform.event_round_prize_probability.is_active IS '정책 활성 여부';
COMMENT ON COLUMN event_platform.event_round_prize_probability.is_deleted IS '논리 삭제 여부';
COMMENT ON COLUMN event_platform.event_round_prize_probability.created_at IS '등록 일시';
COMMENT ON COLUMN event_platform.event_round_prize_probability.created_by IS '등록자 식별자';
COMMENT ON COLUMN event_platform.event_round_prize_probability.updated_at IS '수정 일시';
COMMENT ON COLUMN event_platform.event_round_prize_probability.updated_by IS '수정자 식별자';
COMMENT ON COLUMN event_platform.event_round_prize_probability.deleted_at IS '삭제 일시';

-- ============================================================
-- [6] event_applicant
-- 역할: 참여 가능 대상자 기준
-- Confirmed change: 이벤트 단위 eligibility unique 및 nullable round_id 반영
-- ============================================================
CREATE TABLE event_platform.event_applicant (
    id         BIGINT      GENERATED ALWAYS AS IDENTITY,
    event_id   BIGINT      NOT NULL,
    round_id   BIGINT,
    member_id  BIGINT      NOT NULL,
    is_deleted BOOLEAN     NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT      NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT      NOT NULL,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT pk_event_applicant       PRIMARY KEY (id),
    CONSTRAINT fk_event_applicant_round FOREIGN KEY (round_id)
        REFERENCES event_platform.event_round(id)
);

COMMENT ON TABLE  event_platform.event_applicant            IS '참여 가능 대상자 기준';
COMMENT ON COLUMN event_platform.event_applicant.event_id   IS '이벤트 식별자 (eligibility 기준값)';
COMMENT ON COLUMN event_platform.event_applicant.round_id   IS '회차 식별자 (선택, 특정 회차 대상일 때만 사용)';
COMMENT ON COLUMN event_platform.event_applicant.member_id  IS '회원 식별자';
COMMENT ON COLUMN event_platform.event_applicant.is_deleted IS '논리 삭제 여부';
COMMENT ON COLUMN event_platform.event_applicant.created_at IS '등록 일시';
COMMENT ON COLUMN event_platform.event_applicant.created_by IS '등록자 식별자';
COMMENT ON COLUMN event_platform.event_applicant.updated_at IS '수정 일시';
COMMENT ON COLUMN event_platform.event_applicant.updated_by IS '수정자 식별자';
COMMENT ON COLUMN event_platform.event_applicant.deleted_at IS '삭제 일시';

-- ============================================================
-- [7] event_entry
-- 역할: 응모 행위 이력
-- Confirmed change: round_id 직접 저장
-- ============================================================
CREATE TABLE event_platform.event_entry (
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

    CONSTRAINT pk_event_entry           PRIMARY KEY (id),
    CONSTRAINT fk_event_entry_applicant FOREIGN KEY (applicant_id)
        REFERENCES event_platform.event_applicant(id),
    CONSTRAINT fk_event_entry_round FOREIGN KEY (round_id)
        REFERENCES event_platform.event_round(id)
);

COMMENT ON TABLE  event_platform.event_entry                    IS '응모 행위 이력';
COMMENT ON COLUMN event_platform.event_entry.applicant_id       IS '참여자 식별자';
COMMENT ON COLUMN event_platform.event_entry.event_id           IS '이벤트 식별자 (조회/중복체크용 값참조)';
COMMENT ON COLUMN event_platform.event_entry.round_id           IS '출석/응모 회차 식별자';
COMMENT ON COLUMN event_platform.event_entry.member_id          IS '회원 식별자';
COMMENT ON COLUMN event_platform.event_entry.applied_at         IS '응모 일시';
COMMENT ON COLUMN event_platform.event_entry.event_round_prize_id IS '당첨 경품 식별자 (즉시당첨 시)';
COMMENT ON COLUMN event_platform.event_entry.is_winner          IS '당첨 여부 보조값 (SoT: event_win)';
COMMENT ON COLUMN event_platform.event_entry.is_deleted         IS '논리 삭제 여부';
COMMENT ON COLUMN event_platform.event_entry.created_at         IS '등록 일시';
COMMENT ON COLUMN event_platform.event_entry.created_by         IS '등록자 식별자';
COMMENT ON COLUMN event_platform.event_entry.updated_at         IS '수정 일시';
COMMENT ON COLUMN event_platform.event_entry.updated_by         IS '수정자 식별자';
COMMENT ON COLUMN event_platform.event_entry.deleted_at         IS '삭제 일시';

-- ============================================================
-- [8] event_win
-- 역할: 당첨 결과 SoT
-- ============================================================
CREATE TABLE event_platform.event_win (
    id                   BIGINT      GENERATED ALWAYS AS IDENTITY,
    entry_id             BIGINT      NOT NULL,
    round_id             BIGINT      NOT NULL,
    event_id             BIGINT      NOT NULL,
    member_id            BIGINT      NOT NULL,
    event_round_prize_id BIGINT,
    is_deleted           BOOLEAN     NOT NULL DEFAULT FALSE,

    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           BIGINT      NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by           BIGINT      NOT NULL,
    deleted_at           TIMESTAMPTZ,

    CONSTRAINT pk_event_win       PRIMARY KEY (id),
    CONSTRAINT fk_event_win_entry FOREIGN KEY (entry_id)
        REFERENCES event_platform.event_entry(id),
    CONSTRAINT fk_event_win_round FOREIGN KEY (round_id)
        REFERENCES event_platform.event_round(id)
);

COMMENT ON TABLE  event_platform.event_win                    IS '당첨 결과 SoT';
COMMENT ON COLUMN event_platform.event_win.entry_id           IS '응모 식별자 (1응모 최대 1당첨)';
COMMENT ON COLUMN event_platform.event_win.round_id           IS '회차 식별자';
COMMENT ON COLUMN event_platform.event_win.event_id           IS '이벤트 식별자 (조회용 값참조)';
COMMENT ON COLUMN event_platform.event_win.member_id          IS '당첨 회원 식별자 (외부 시스템)';
COMMENT ON COLUMN event_platform.event_win.event_round_prize_id IS '당첨 경품 식별자';
COMMENT ON COLUMN event_platform.event_win.is_deleted         IS '논리 삭제 여부';
COMMENT ON COLUMN event_platform.event_win.created_at         IS '등록 일시';
COMMENT ON COLUMN event_platform.event_win.created_by         IS '등록자 식별자';
COMMENT ON COLUMN event_platform.event_win.updated_at         IS '수정 일시';
COMMENT ON COLUMN event_platform.event_win.updated_by         IS '수정자 식별자';
COMMENT ON COLUMN event_platform.event_win.deleted_at         IS '삭제 일시';

-- ============================================================
-- UNIQUE INDEX (논리 삭제 제외)
-- ============================================================
CREATE UNIQUE INDEX uq_event_round_event_round_no
    ON event_platform.event_round (event_id, round_no)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uq_event_applicant_event_member_id
    ON event_platform.event_applicant (event_id, member_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_event_entry_event_round_member_id
    ON event_platform.event_entry (event_id, round_id, member_id)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uq_event_win_entry_id
    ON event_platform.event_win (entry_id)
    WHERE is_deleted = FALSE;
