-- ============================================================
-- Event Platform Schema DDL Draft
-- Target   : PostgreSQL 17
-- Schema   : promotion
-- Encoding : UTF-8
-- Note     : 신규 환경 기준 전체 생성용 초안
--            시간 컬럼은 TIMESTAMP를 사용하고
--            애플리케이션 해석 기준은 Asia/Seoul이다.
--            업무상 꼭 필요한 최소 FK와 unique만 유지한다.
-- ============================================================

CREATE SCHEMA IF NOT EXISTS promotion;
SET search_path TO promotion, public;

-- ============================================================
-- [1] prize
-- 역할: 재사용 가능한 경품 마스터
-- ============================================================
CREATE TABLE promotion.prize
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY,
    prize_name        VARCHAR(100) NOT NULL,
    reward_type       VARCHAR(50)  NOT NULL,
    point_amount      INTEGER,
    coupon_id         BIGINT,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    prize_description TEXT,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        BIGINT       NOT NULL,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        BIGINT       NOT NULL,
    deleted_at        TIMESTAMP,
    CONSTRAINT pk_prize PRIMARY KEY (id)
);
COMMENT ON TABLE  promotion.prize                   IS '경품 마스터';
COMMENT ON COLUMN promotion.prize.prize_name        IS '경품명';
COMMENT ON COLUMN promotion.prize.reward_type       IS '보상 유형 (공통코드: PRIZE_TYPE[POINT, COUPON, PRODUCT, ETC])';
COMMENT ON COLUMN promotion.prize.point_amount      IS '포인트 지급액 (POINT 전용)';
COMMENT ON COLUMN promotion.prize.coupon_id         IS '쿠폰 식별자 (외부 참조)';
COMMENT ON COLUMN promotion.prize.is_active         IS '활성 여부';
COMMENT ON COLUMN promotion.prize.is_deleted        IS '논리 삭제 여부';
COMMENT ON COLUMN promotion.prize.prize_description IS '경품 설명';
COMMENT ON COLUMN promotion.prize.created_at        IS '등록 일시';
COMMENT ON COLUMN promotion.prize.created_by        IS '등록자 식별자';
COMMENT ON COLUMN promotion.prize.updated_at        IS '수정 일시';
COMMENT ON COLUMN promotion.prize.updated_by        IS '수정자 식별자';
COMMENT ON COLUMN promotion.prize.deleted_at        IS '삭제 일시';

-- ============================================================
-- [2] event
-- 역할: 이벤트 기본 정보 + 공통 운영 정책
-- ============================================================
CREATE TABLE promotion.event
(
    id                       BIGINT GENERATED ALWAYS AS IDENTITY,
    event_name               VARCHAR(100) NOT NULL,
    event_type               VARCHAR(50)  NOT NULL,
    start_at                 TIMESTAMP    NOT NULL,
    end_at                   TIMESTAMP    NOT NULL,
    supplier_id              BIGINT       NOT NULL,
    event_url                VARCHAR(300),
    winner_selection_cycle   INTEGER,
    winner_selection_base_at TIMESTAMP,
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
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               BIGINT       NOT NULL,
    updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by               BIGINT       NOT NULL,
    deleted_at               TIMESTAMP,
    CONSTRAINT pk_event PRIMARY KEY (id)
);
COMMENT ON TABLE  promotion.event                          IS '이벤트 기본 정보 및 운영 정책';
COMMENT ON COLUMN promotion.event.event_name               IS '이벤트명';
COMMENT ON COLUMN promotion.event.event_type               IS '이벤트 유형 (공통코드: EVENT_TYPE[ATTENDANCE, RANDOM_REWARD])';
COMMENT ON COLUMN promotion.event.start_at                 IS '이벤트 시작 일시';
COMMENT ON COLUMN promotion.event.end_at                   IS '이벤트 종료 일시';
COMMENT ON COLUMN promotion.event.supplier_id              IS '공급사 식별자 (외부 값참조, 현재 위드 DB 기준)';
COMMENT ON COLUMN promotion.event.event_url                IS '이벤트 URL';
COMMENT ON COLUMN promotion.event.winner_selection_cycle   IS '당첨자 선정 주기 (시간)';
COMMENT ON COLUMN promotion.event.winner_selection_base_at IS '당첨자 선정 기준 일시';
COMMENT ON COLUMN promotion.event.priority                 IS '전시 우선순위 (낮을수록 우선)';
COMMENT ON COLUMN promotion.event.is_active                IS '활성 여부';
COMMENT ON COLUMN promotion.event.is_visible               IS '전시 여부';
COMMENT ON COLUMN promotion.event.is_deleted               IS '논리 삭제 여부';
COMMENT ON COLUMN promotion.event.is_auto_entry            IS '자동 응모 여부';
COMMENT ON COLUMN promotion.event.is_sns_linked            IS 'SNS 연동 여부';
COMMENT ON COLUMN promotion.event.is_winner_announced      IS '당첨자 발표 여부';
COMMENT ON COLUMN promotion.event.is_duplicate_winner      IS '중복 당첨 허용 여부';
COMMENT ON COLUMN promotion.event.is_multiple_entry        IS '복수 응모 허용 여부';
COMMENT ON COLUMN promotion.event.description              IS '이벤트 설명';
COMMENT ON COLUMN promotion.event.created_at               IS '등록 일시';
COMMENT ON COLUMN promotion.event.created_by               IS '등록자 식별자';
COMMENT ON COLUMN promotion.event.updated_at               IS '수정 일시';
COMMENT ON COLUMN promotion.event.updated_by               IS '수정자 식별자';
COMMENT ON COLUMN promotion.event.deleted_at               IS '삭제 일시';

-- ============================================================
-- [3] event_round
-- 역할: 이벤트 회차 기준
-- 관계: event (1) -> event_round (N)
-- ============================================================
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

-- ============================================================
-- [4] event_round_prize
-- 역할: 회차별 경품 정책
-- 관계: event_round (1) -> event_round_prize (N)
--       prize (1) -> event_round_prize (N)
-- ============================================================
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

-- ============================================================
-- [5] event_round_prize_probability
-- 역할: 회차-경품 확률 정책
-- 관계: event_round_prize (1) -> probability (N)
--       event_round (1) -> probability (N)
-- ============================================================
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

-- ============================================================
-- [6] event_applicant
-- 역할: 참여자 기준 (회차당 1인 1참여)
-- 관계: event_round (1) -> event_applicant (N)
-- ============================================================
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

-- ============================================================
-- [7] event_entry
-- 역할: 응모권/참여 이력 (1참여자 다중 응모)
-- Note: applicant_id는 값참조로 유지
-- 관계: event_applicant (1) -> event_entry (N)
-- ============================================================
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

-- ============================================================
-- [8] event_win
-- 역할: 당첨 결과 SoT
-- Note: entry_id는 값참조로 유지
-- ============================================================
CREATE TABLE promotion.event_win
(
    id                   BIGINT GENERATED ALWAYS AS IDENTITY,
    entry_id             BIGINT      NOT NULL,
    round_id             BIGINT      NOT NULL,
    event_id             BIGINT      NOT NULL,
    member_id            BIGINT      NOT NULL,
    event_round_prize_id BIGINT,
    is_deleted           BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           BIGINT      NOT NULL,
    updated_at           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by           BIGINT      NOT NULL,
    deleted_at           TIMESTAMP,
    CONSTRAINT pk_event_win PRIMARY KEY (id)
);
COMMENT ON TABLE  promotion.event_win                     IS '당첨 결과 SoT';
COMMENT ON COLUMN promotion.event_win.entry_id            IS '응모 식별자 (1응모 최대 1당첨)';
COMMENT ON COLUMN promotion.event_win.round_id            IS '회차 식별자';
COMMENT ON COLUMN promotion.event_win.event_id            IS '이벤트 식별자 (조회용 값참조)';
COMMENT ON COLUMN promotion.event_win.member_id           IS '당첨 회원 식별자 (외부 시스템)';
COMMENT ON COLUMN promotion.event_win.event_round_prize_id IS '로컬 보상 확정 경품 식별자 (SoT)';
COMMENT ON COLUMN promotion.event_win.is_deleted          IS '논리 삭제 여부';
COMMENT ON COLUMN promotion.event_win.created_at          IS '등록 일시';
COMMENT ON COLUMN promotion.event_win.created_by          IS '등록자 식별자';
COMMENT ON COLUMN promotion.event_win.updated_at          IS '수정 일시';
COMMENT ON COLUMN promotion.event_win.updated_by          IS '수정자 식별자';
COMMENT ON COLUMN promotion.event_win.deleted_at          IS '삭제 일시';

-- ============================================================
-- MINIMUM UNIQUE INDEX (논리 삭제 제외)
-- Note: event_applicant.event_id, event_entry.applicant_id,
--       event_win.entry_id 정합성은 Service 검증으로 보완한다.
-- ============================================================
-- event_round: 이벤트당 회차 번호 중복 방지
CREATE UNIQUE INDEX uq_event_round_event_round_no
    ON promotion.event_round (event_id, round_no)
    WHERE is_deleted = FALSE;

-- event_applicant: 동일 회차 내 1인 1참여 중복 방지
CREATE UNIQUE INDEX uq_event_applicant_round_member_id
    ON promotion.event_applicant (round_id, member_id)
    WHERE is_deleted = FALSE;

-- event_win: 응모 1건당 당첨 결과는 최대 1건
CREATE UNIQUE INDEX uq_event_win_entry_id
    ON promotion.event_win (entry_id)
    WHERE is_deleted = FALSE;

-- 아직 제안 안했음 이야기 해봐야함
CREATE INDEX idx_event_entry_event_member
    ON promotion.event_entry (event_id, member_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_event_entry_applicant_id
    ON promotion.event_entry (applicant_id)
    WHERE is_deleted = FALSE;