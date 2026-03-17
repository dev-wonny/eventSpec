SET search_path TO event, public;

INSERT INTO event.prize (
    prize_name,
    reward_type,
    point_amount,
    is_active,
    is_deleted,
    prize_description,
    created_by,
    updated_by
) VALUES (
    '3월 출석 포인트 30P',
    'POINT',
    30,
    TRUE,
    FALSE,
    '로컬 개발용 출석 포인트 보상',
    'seed',
    'seed'
);

INSERT INTO event.event (
    event_name,
    event_type,
    start_at,
    end_at,
    supplier_id,
    event_url,
    priority,
    is_active,
    is_visible,
    is_auto_entry,
    is_sns_linked,
    is_winner_announced,
    is_duplicate_winner,
    is_multiple_entry,
    description,
    created_by,
    updated_by
) VALUES (
    '2026년 3월 출석체크 이벤트',
    'ATTENDANCE',
    '2026-03-01T00:00:00+09:00',
    '2026-03-31T23:59:59+09:00',
    1,
    'https://event-api.dolfarmer.com/event/v1/events/1',
    1,
    TRUE,
    TRUE,
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    '로컬 개발용 3월 출석체크 이벤트',
    'seed',
    'seed'
);

INSERT INTO event.event_round (
    event_id,
    round_no,
    round_start_at,
    round_end_at,
    draw_at,
    is_confirmed,
    is_deleted,
    created_by,
    updated_by
)
SELECT
    e.id,
    gs.round_no,
    '2026-03-01T00:00:00+09:00'::timestamptz + ((gs.round_no - 1) || ' days')::interval,
    '2026-03-01T23:59:59+09:00'::timestamptz + ((gs.round_no - 1) || ' days')::interval,
    NULL,
    FALSE,
    FALSE,
    'seed',
    'seed'
FROM event.event e
CROSS JOIN generate_series(1, 31) AS gs(round_no)
WHERE e.event_name = '2026년 3월 출석체크 이벤트';

INSERT INTO event.event_applicant (
    event_id,
    round_id,
    member_id,
    created_by,
    updated_by
)
SELECT
    e.id,
    r.id,
    1001,
    'seed',
    'seed'
FROM event.event e
JOIN event.event_round r
    ON r.event_id = e.id
   AND r.round_no = 1
WHERE e.event_name = '2026년 3월 출석체크 이벤트';

INSERT INTO event.event_round_prize (
    round_id,
    prize_id,
    priority,
    is_active,
    is_deleted,
    created_by,
    updated_by
)
SELECT
    r.id,
    p.id,
    0,
    TRUE,
    FALSE,
    'seed',
    'seed'
FROM event.event_round r
JOIN event.event e
    ON e.id = r.event_id
JOIN event.prize p
    ON p.prize_name = '3월 출석 포인트 30P'
WHERE e.event_name = '2026년 3월 출석체크 이벤트';

INSERT INTO event.event_entry (
    applicant_id,
    event_id,
    round_id,
    member_id,
    applied_at,
    event_round_prize_id,
    is_winner,
    created_by,
    updated_by
)
SELECT
    a.id,
    e.id,
    r.id,
    1001,
    r.round_start_at,
    erp.id,
    TRUE,
    'seed',
    'seed'
FROM event.event_applicant a
JOIN event.event e
    ON e.id = a.event_id
JOIN event.event_round r
    ON r.event_id = e.id
JOIN event.event_round_prize erp
    ON erp.round_id = r.id
WHERE e.event_name = '2026년 3월 출석체크 이벤트'
  AND a.member_id = 1001
  AND r.round_no IN (1, 2, 5);

INSERT INTO event.event_win (
    entry_id,
    round_id,
    event_id,
    member_id,
    event_round_prize_id,
    created_by,
    updated_by
)
SELECT
    ee.id,
    ee.round_id,
    ee.event_id,
    ee.member_id,
    ee.event_round_prize_id,
    'seed',
    'seed'
FROM event.event_entry ee
JOIN event.event e
    ON e.id = ee.event_id
JOIN event.event_round r
    ON r.id = ee.round_id
WHERE e.event_name = '2026년 3월 출석체크 이벤트'
  AND ee.member_id = 1001
  AND r.round_no IN (1, 2, 5);
