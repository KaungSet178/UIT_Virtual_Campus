-- Development-only timetable fixture.
-- Run src/main/resources/db/seed-campus-dev.sql first so referenced rooms exist.
-- This script is not executed automatically by Spring Boot.

INSERT INTO public.semesters (name, start_date, end_date)
VALUES
    ('2026 Semester 1', DATE '2026-01-05', DATE '2026-05-29'),
    ('2026 Semester 2', DATE '2026-06-15', DATE '2026-10-30')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.sections (section_name)
VALUES
    ('CS-1A'),
    ('CS-1B'),
    ('CS-2A')
ON CONFLICT (section_name) DO NOTHING;

INSERT INTO public.subjects (subject_code, subject_name, description)
VALUES
    ('CS101', 'Programming Fundamentals', 'Introduction to programming concepts'),
    ('CS102', 'Database Systems', 'Relational database design and SQL'),
    ('CS201', 'Data Structures', 'Core data structures and algorithms')
ON CONFLICT (subject_code) DO NOTHING;

INSERT INTO public.course_offerings (semester_id, section_id, subject_id)
SELECT sem.semester_id, sec.section_id, sub.subject_id
FROM public.semesters sem
CROSS JOIN public.sections sec
CROSS JOIN public.subjects sub
WHERE sem.name = '2026 Semester 1'
  AND (
      (sec.section_name = 'CS-1A' AND sub.subject_code IN ('CS101', 'CS102'))
      OR (sec.section_name = 'CS-2A' AND sub.subject_code = 'CS201')
  )
ON CONFLICT (semester_id, section_id, subject_id) DO NOTHING;

INSERT INTO public.schedules (offering_id, room_id, day_of_week, start_time, end_time)
SELECT co.offering_id, r.room_id, 1, TIME '09:00', TIME '10:30'
FROM public.course_offerings co
JOIN public.semesters sem ON sem.semester_id = co.semester_id
JOIN public.sections sec ON sec.section_id = co.section_id
JOIN public.subjects sub ON sub.subject_id = co.subject_id
JOIN public.rooms r ON r.room_number = '111'
WHERE sem.name = '2026 Semester 1'
  AND sec.section_name = 'CS-1A'
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
JOIN public.sections sec ON sec.section_id = co.section_id
JOIN public.subjects sub ON sub.subject_id = co.subject_id
JOIN public.rooms r ON r.room_number = '231'
WHERE sem.name = '2026 Semester 1'
  AND sec.section_name = 'CS-1A'
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
JOIN public.sections sec ON sec.section_id = co.section_id
JOIN public.subjects sub ON sub.subject_id = co.subject_id
JOIN public.rooms r ON r.room_number = '121'
WHERE sem.name = '2026 Semester 1'
  AND sec.section_name = 'CS-2A'
  AND sub.subject_code = 'CS201'
  AND NOT EXISTS (
      SELECT 1
      FROM public.schedules existing
      WHERE existing.offering_id = co.offering_id
        AND existing.room_id = r.room_id
        AND existing.day_of_week = 5
        AND existing.start_time = TIME '10:30'
        AND existing.end_time = TIME '12:00'
  );
