-- Development-only event fixture.
-- Replace the UUID and club placeholders with real public.users.user_id and public.clubs.club_id values before running.
-- Required setup:
-- - admin_uuid exists in public.users and has ADMIN role
-- - leader_uuid exists in public.users and leads club_id
-- - club_id exists in public.clubs
-- This script is not executed automatically by Spring Boot.
-- Prefer API-based creation for manual testing because it verifies authenticated createdBy behavior.

-- Example:
-- \set admin_uuid '''00000000-0000-0000-0000-000000000000'''
-- \set leader_uuid '''11111111-1111-1111-1111-111111111111'''
-- \set club_id 10

INSERT INTO public.events (
    created_by,
    club_id,
    title,
    description,
    start_datetime,
    end_datetime,
    location,
    image_url
)
VALUES (
    :'admin_uuid'::uuid,
    NULL,
    'University Orientation',
    'University-wide orientation event',
    now() + interval '7 days',
    now() + interval '7 days 2 hours',
    'Multipurpose Hall',
    NULL
);

INSERT INTO public.events (
    created_by,
    club_id,
    title,
    description,
    start_datetime,
    end_datetime,
    location,
    image_url
)
VALUES (
    :'leader_uuid'::uuid,
    :club_id,
    'Club Showcase',
    'Club event hosted in the Multipurpose Hall',
    now() + interval '14 days',
    now() + interval '14 days 3 hours',
    'Multipurpose Hall',
    NULL
);

INSERT INTO public.events (
    created_by,
    club_id,
    title,
    description,
    start_datetime,
    end_datetime,
    location,
    image_url
)
VALUES (
    :'admin_uuid'::uuid,
    NULL,
    'Past Campus Briefing',
    'Past event for filtering tests',
    now() - interval '14 days',
    now() - interval '14 days' + interval '1 hour',
    'Multipurpose Hall',
    NULL
);
