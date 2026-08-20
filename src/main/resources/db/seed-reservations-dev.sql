-- Development-only reservation fixture.
-- Replace the UUID placeholders with real public.users.user_id values before running.
-- Run campus and timetable seed scripts first so referenced rooms and schedules exist.
-- This script is not executed automatically by Spring Boot.

-- Example:
-- \set requester_uuid '''00000000-0000-0000-0000-000000000000'''
-- \set admin_uuid '''11111111-1111-1111-1111-111111111111'''

INSERT INTO public.reservations (
    user_id,
    room_id,
    reservation_date,
    start_time,
    end_time,
    purpose,
    status,
    approved_by
)
SELECT
    :'requester_uuid'::uuid,
    r.room_id,
    DATE '2026-08-24',
    TIME '15:00',
    TIME '16:00',
    'Student study group',
    'PENDING',
    NULL
FROM public.rooms r
WHERE r.room_number = '111';

INSERT INTO public.reservations (
    user_id,
    room_id,
    reservation_date,
    start_time,
    end_time,
    purpose,
    status,
    approved_by
)
SELECT
    :'requester_uuid'::uuid,
    r.room_id,
    DATE '2026-08-25',
    TIME '13:00',
    TIME '14:00',
    'Approved project meeting',
    'APPROVED',
    :'admin_uuid'::uuid
FROM public.rooms r
WHERE r.room_number = '111';
