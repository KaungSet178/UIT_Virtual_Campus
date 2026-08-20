-- Development-only club fixture.
-- Replace the UUID placeholders with real public.users.user_id values before running.
-- Required setup:
-- - leader_uuid exists in public.users
-- - leader_uuid exists in public.student_profiles
-- - leader_uuid has STUDENT and CLUB_LEADER roles in public.user_roles
-- - member_uuid exists in public.users and public.student_profiles
-- This script is not executed automatically by Spring Boot.

-- Example:
-- \set leader_uuid '''00000000-0000-0000-0000-000000000000'''
-- \set member_uuid '''11111111-1111-1111-1111-111111111111'''

INSERT INTO public.clubs (
    leader_id,
    club_name,
    description,
    contact_email,
    contact_phone,
    logo_url
)
VALUES (
    :'leader_uuid'::uuid,
    'Programming Club',
    'Student club for programming practice and peer learning',
    'programming.club@uit.example',
    NULL,
    NULL
)
ON CONFLICT (club_name) DO NOTHING;

INSERT INTO public.club_memberships (club_id, student_id, status)
SELECT c.club_id, :'leader_uuid'::uuid, 'ACTIVE'
FROM public.clubs c
WHERE c.club_name = 'Programming Club'
ON CONFLICT (club_id, student_id) DO UPDATE SET status = 'ACTIVE';

INSERT INTO public.club_memberships (club_id, student_id, status)
SELECT c.club_id, :'member_uuid'::uuid, 'ACTIVE'
FROM public.clubs c
WHERE c.club_name = 'Programming Club'
ON CONFLICT (club_id, student_id) DO UPDATE SET status = 'ACTIVE';

INSERT INTO public.club_galleries (club_id, image_url, caption)
SELECT c.club_id, 'https://example.com/programming-club.jpg', 'Programming Club activities'
FROM public.clubs c
WHERE c.club_name = 'Programming Club';
