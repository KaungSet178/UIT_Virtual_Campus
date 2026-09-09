-- Development-only timetable fixture.
-- Run src/main/resources/db/seed-campus-dev.sql first so referenced rooms exist.
-- This script is not executed automatically by Spring Boot.

INSERT INTO public.semesters (name, start_date, end_date)
VALUES
    ('2026 Semester 1', DATE '2026-01-05', DATE '2026-05-29'),
    ('2026 Semester 2', DATE '2026-06-15', DATE '2026-10-30')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.academic_groups (year, group_type, section_name, department, major)
VALUES
    (1, 'SECTION', 'A', NULL, NULL),
    (1, 'SECTION', 'B', NULL, NULL),
    (2, 'SECTION', 'A', NULL, NULL),
    (2, 'SECTION', 'B', NULL, NULL),
    (3, 'MAJOR', NULL, 'CS', 'KE'),
    (3, 'MAJOR', NULL, 'CS', 'SE'),
    (3, 'MAJOR', NULL, 'CS', 'BIS'),
    (3, 'MAJOR', NULL, 'CT', 'Cybersecurity'),
    (3, 'MAJOR', NULL, 'CT', 'Network'),
    (3, 'MAJOR', NULL, 'CT', 'Embedded Systems')
ON CONFLICT DO NOTHING;

INSERT INTO public.subjects (subject_code, subject_name, description)
VALUES
    ('CS101', 'Programming Fundamentals', 'Introduction to programming concepts'),
    ('CS102', 'Database Systems', 'Relational database design and SQL'),
    ('CS301', 'Software Engineering', 'Software development process and design'),
    ('CST-4105', 'Enterprise Applications Development using Java (Keystone Project)', 'Second Year elective subject'),
    ('CST-4307', 'Advanced Web Technology with PHP (Keystone Project)', 'Second Year elective subject'),
    ('CST-4406', 'Data and Computer Communications', 'Second Year elective subject'),
    ('CST-4407', 'Engineering Circuits', 'Second Year elective subject'),
    ('CST-4408', 'Foundations of Cybersecurity', 'Second Year elective subject')
ON CONFLICT (subject_code) DO NOTHING;

INSERT INTO public.course_offerings (semester_id, academic_group_id, subject_id)
SELECT sem.semester_id, ag.group_id, sub.subject_id
FROM public.semesters sem
CROSS JOIN public.academic_groups ag
CROSS JOIN public.subjects sub
WHERE sem.name = '2026 Semester 1'
  AND (
      (ag.group_type = 'SECTION' AND ag.year = 1 AND ag.section_name = 'A' AND sub.subject_code IN ('CS101', 'CS102'))
      OR (ag.group_type = 'MAJOR' AND ag.year = 3 AND ag.department = 'CS' AND ag.major = 'SE' AND sub.subject_code = 'CS301')
  )
ON CONFLICT (semester_id, academic_group_id, subject_id) DO NOTHING;

INSERT INTO public.schedules (offering_id, room_id, day_of_week, start_time, end_time)
SELECT co.offering_id, r.room_id, 1, TIME '09:00', TIME '10:30'
FROM public.course_offerings co
JOIN public.semesters sem ON sem.semester_id = co.semester_id
JOIN public.academic_groups ag ON ag.group_id = co.academic_group_id
JOIN public.subjects sub ON sub.subject_id = co.subject_id
JOIN public.rooms r ON r.room_number = '111'
WHERE sem.name = '2026 Semester 1'
  AND ag.group_type = 'SECTION'
  AND ag.year = 1
  AND ag.section_name = 'A'
  AND sub.subject_code = 'CS101'
  AND NOT EXISTS (
      SELECT 1
      FROM public.schedules existing
      WHERE existing.offering_id = co.offering_id
        AND existing.room_id = r.room_id
        AND existing.day_of_week = 1
        AND existing.start_time = TIME '09:00'
        AND existing.end_time = TIME '10:30'
  );

INSERT INTO public.schedules (offering_id, room_id, day_of_week, start_time, end_time)
SELECT co.offering_id, r.room_id, 3, TIME '13:00', TIME '14:30'
FROM public.course_offerings co
JOIN public.semesters sem ON sem.semester_id = co.semester_id
JOIN public.academic_groups ag ON ag.group_id = co.academic_group_id
JOIN public.subjects sub ON sub.subject_id = co.subject_id
JOIN public.rooms r ON r.room_number = '231'
WHERE sem.name = '2026 Semester 1'
  AND ag.group_type = 'SECTION'
  AND ag.year = 1
  AND ag.section_name = 'A'
  AND sub.subject_code = 'CS102'
  AND NOT EXISTS (
      SELECT 1
      FROM public.schedules existing
      WHERE existing.offering_id = co.offering_id
        AND existing.room_id = r.room_id
        AND existing.day_of_week = 3
        AND existing.start_time = TIME '13:00'
        AND existing.end_time = TIME '14:30'
  );

INSERT INTO public.schedules (offering_id, room_id, day_of_week, start_time, end_time)
SELECT co.offering_id, r.room_id, 5, TIME '10:30', TIME '12:00'
FROM public.course_offerings co
JOIN public.semesters sem ON sem.semester_id = co.semester_id
JOIN public.academic_groups ag ON ag.group_id = co.academic_group_id
JOIN public.subjects sub ON sub.subject_id = co.subject_id
JOIN public.rooms r ON r.room_number = '121'
WHERE sem.name = '2026 Semester 1'
  AND ag.group_type = 'MAJOR'
  AND ag.year = 3
  AND ag.department = 'CS'
  AND ag.major = 'SE'
  AND sub.subject_code = 'CS301'
  AND NOT EXISTS (
      SELECT 1
      FROM public.schedules existing
      WHERE existing.offering_id = co.offering_id
        AND existing.room_id = r.room_id
        AND existing.day_of_week = 5
        AND existing.start_time = TIME '10:30'
        AND existing.end_time = TIME '12:00'
  );
