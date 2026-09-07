-- Development Seed Data for UIT Virtual Campus (Phase 2: Campus Management)
-- Manual execution only. Do not run automatically in production.

-- 1. Insert Buildings
INSERT INTO public.buildings (building_id, building_name, description) VALUES
(1, 'Building 1', 'Main Academic Building 1 housing classrooms and faculty offices'),
(2, 'Building 2', 'Academic Building 2 housing computing labs and lecture halls'),
(3, 'Building 3', 'Academic Building 3 housing specialized laboratories and tutorial rooms'),
(4, 'Building 4', 'Academic Building 4 housing advanced research labs and seminar rooms')
ON CONFLICT (building_id) DO NOTHING;

-- 2. Insert Floors (6 floors per building)
-- Building 1 (Floors 1-6)
INSERT INTO public.floors (floor_id, building_id, floor_number) VALUES
(101, 1, 1),
(102, 1, 2),
(103, 1, 3),
(104, 1, 4),
(105, 1, 5),
(106, 1, 6),
-- Building 2 (Floors 1-6)
(201, 2, 1),
(202, 2, 2),
(203, 2, 3),
(204, 2, 4),
(205, 2, 5),
(206, 2, 6),
-- Building 3 (Floors 1-6)
(301, 3, 1),
(302, 3, 2),
(303, 3, 3),
(304, 3, 4),
(305, 3, 5),
(306, 3, 6),
-- Building 4 (Floors 1-6)
(401, 4, 1),
(402, 4, 2),
(403, 4, 3),
(404, 4, 4),
(405, 4, 5),
(406, 4, 6)
ON CONFLICT (floor_id) DO NOTHING;

-- 3. Insert Rooms (6 rooms per floor, 144 numbered rooms + 1 Multipurpose Hall)

-- Building 1 Rooms (111 - 166)
INSERT INTO public.rooms (floor_id, room_number, room_name, room_type, description, model_object_id) VALUES
-- Floor 1
(101, '111', 'Room 111', 'CLASSROOM', 'Standard Classroom 111', 'room_111'),
(101, '112', 'Room 112', 'CLASSROOM', 'Standard Classroom 112', 'room_112'),
(101, '113', 'Room 113', 'CLASSROOM', 'Standard Classroom 113', 'room_113'),
-- Floor 2
(102, '121', 'Room 121', 'CLASSROOM', 'Standard Classroom 121', 'room_121'),
(102, '122', 'Room 122', 'CLASSROOM', 'Standard Classroom 122', 'room_122'),
(102, '123', 'Room 123', 'CLASSROOM', 'Standard Classroom 123', 'room_123'),
-- Floor 3
(103, '131', 'Room 131', 'CLASSROOM', 'Standard Classroom 131', 'room_131'),
(103, '132', 'Room 132', 'CLASSROOM', 'Standard Classroom 132', 'room_132'),
(103, '133', 'Room 133', 'CLASSROOM', 'Standard Classroom 133', 'room_133'),
(103, '134', 'Room 134', 'CLASSROOM', 'Standard Classroom 134', 'room_134'),
(103, '135', 'Room 135', 'CLASSROOM', 'Standard Classroom 135', 'room_135'),
(103, '136', 'Room 136', 'CLASSROOM', 'Standard Classroom 136', 'room_136'),
-- Floor 4
(104, '141', 'Room 141', 'CLASSROOM', 'Standard Classroom 141', 'room_141'),
(104, '142', 'Room 142', 'CLASSROOM', 'Standard Classroom 142', 'room_142'),
(104, '143', 'Room 143', 'CLASSROOM', 'Standard Classroom 143', 'room_143'),
(104, '144', 'Room 144', 'CLASSROOM', 'Standard Classroom 144', 'room_144'),
(104, '145', 'Room 145', 'CLASSROOM', 'Standard Classroom 145', 'room_145'),
(104, '146', 'Room 146', 'CLASSROOM', 'Standard Classroom 146', 'room_146'),
-- Floor 5
(105, '151', 'Room 151', 'CLASSROOM', 'Standard Classroom 151', 'room_151'),
(105, '152', 'Room 152', 'CLASSROOM', 'Standard Classroom 152', 'room_152'),
(105, '153', 'Room 153', 'CLASSROOM', 'Standard Classroom 153', 'room_153'),
(105, '154', 'Room 154', 'CLASSROOM', 'Standard Classroom 154', 'room_154'),
(105, '155', 'Room 155', 'CLASSROOM', 'Standard Classroom 155', 'room_155'),
(105, '156', 'Room 156', 'CLASSROOM', 'Standard Classroom 156', 'room_156'),
-- Floor 6
(106, '161', 'Room 161', 'CLASSROOM', 'Standard Classroom 161', 'room_161'),
(106, '162', 'Room 162', 'CLASSROOM', 'Standard Classroom 162', 'room_162'),
(106, '163', 'Room 163', 'CLASSROOM', 'Standard Classroom 163', 'room_163'),
(106, '164', 'Room 164', 'CLASSROOM', 'Standard Classroom 164', 'room_164'),
(106, '165', 'Room 165', 'CLASSROOM', 'Standard Classroom 165', 'room_165'),
(106, '166', 'Room 166', 'CLASSROOM', 'Standard Classroom 166', 'room_166'),

-- Building 2 Rooms (211 - 266)
-- Floor 1
(201, '213', 'Room 213', 'CLASSROOM', 'Lecture Room 213', 'room_213'),
(201, '214', 'Room 214', 'CLASSROOM', 'Lecture Room 214', 'room_214'),
(201, '215', 'Room 215', 'CLASSROOM', 'Lecture Room 215', 'room_215'),
-- Floor 3
(203, '231', 'Database Lab', 'LABORATORY', 'Database and Big Data Laboratory', 'room_231'),
(203, '232', 'Software Engineering Lab', 'LABORATORY', 'Software Development Lab', 'room_232'),
(203, '233', 'Room 233', 'CLASSROOM', 'Lecture Room 233', 'room_233'),
(203, '234', 'Room 234', 'CLASSROOM', 'Lecture Room 234', 'room_234'),
(203, '235', 'Room 235', 'CLASSROOM', 'Lecture Room 235', 'room_235'),
(203, '236', 'Room 236', 'CLASSROOM', 'Lecture Room 236', 'room_236'),
-- Floor 4
(204, '241', 'Room 241', 'CLASSROOM', 'Lecture Room 241', 'room_241'),
(204, '242', 'Room 242', 'CLASSROOM', 'Lecture Room 242', 'room_242'),
(204, '243', 'Room 243', 'CLASSROOM', 'Lecture Room 243', 'room_243'),
(204, '244', 'Room 244', 'CLASSROOM', 'Lecture Room 244', 'room_244'),
(204, '245', 'Room 245', 'CLASSROOM', 'Lecture Room 245', 'room_245'),
(204, '246', 'Room 246', 'CLASSROOM', 'Lecture Room 246', 'room_246'),
-- Floor 5
(205, '251', 'Room 251', 'CLASSROOM', 'Lecture Room 251', 'room_251'),
(205, '252', 'Room 252', 'CLASSROOM', 'Lecture Room 252', 'room_252'),
(205, '253', 'Room 253', 'CLASSROOM', 'Lecture Room 253', 'room_253'),
(205, '254', 'Room 254', 'CLASSROOM', 'Lecture Room 254', 'room_254'),
(205, '255', 'Room 255', 'CLASSROOM', 'Lecture Room 255', 'room_255'),
(205, '256', 'Room 256', 'CLASSROOM', 'Lecture Room 256', 'room_256'),
-- Floor 6
(206, '261', 'Room 261', 'CLASSROOM', 'Lecture Room 261', 'room_261'),
(206, '262', 'Room 262', 'CLASSROOM', 'Lecture Room 262', 'room_262'),
(206, '263', 'Room 263', 'CLASSROOM', 'Lecture Room 263', 'room_263'),
(206, '264', 'Room 264', 'CLASSROOM', 'Lecture Room 264', 'room_264'),
(206, '265', 'Room 265', 'CLASSROOM', 'Lecture Room 265', 'room_265'),
(206, '266', 'Room 266', 'CLASSROOM', 'Lecture Room 266', 'room_266'),

-- Building 3 Rooms (311 - 366)
-- Floor 1
(301, '311', 'Hardware Lab', 'LABORATORY', 'Computer Architecture and Hardware Lab', 'room_311'),
(301, '312', 'Embedded Systems Lab', 'LABORATORY', 'Embedded Systems and IoT Lab', 'room_312'),
-- Floor 2
(302, '321', 'Networking Lab', 'LABORATORY', 'Computer Networks and Cisco Lab', 'room_321'),
(302, '322', 'Room 322', 'CLASSROOM', 'Tutorial Room 322', 'room_322'),
(302, '323', 'Room 323', 'CLASSROOM', 'Tutorial Room 323', 'room_323'),
(302, '324', 'Room 324', 'CLASSROOM', 'Tutorial Room 324', 'room_324'),
(302, '325', 'Room 325', 'CLASSROOM', 'Tutorial Room 325', 'room_325'),
(302, '326', 'Room 326', 'CLASSROOM', 'Tutorial Room 326', 'room_326'),
-- Floor 3
(303, '331', 'Cybersecurity Lab', 'LABORATORY', 'Information Security & Digital Forensics Lab', 'room_331'),
(303, '332', 'Room 332', 'CLASSROOM', 'Tutorial Room 332', 'room_332'),
(303, '333', 'Room 333', 'CLASSROOM', 'Tutorial Room 333', 'room_333'),
(303, '334', 'Room 334', 'CLASSROOM', 'Tutorial Room 334', 'room_334'),
(303, '335', 'Room 335', 'CLASSROOM', 'Tutorial Room 335', 'room_335'),
(303, '336', 'Room 336', 'CLASSROOM', 'Tutorial Room 336', 'room_336'),
-- Floor 4
(304, '341', 'Room 341', 'CLASSROOM', 'Tutorial Room 341', 'room_341'),
(304, '342', 'Room 342', 'CLASSROOM', 'Tutorial Room 342', 'room_342'),
(304, '343', 'Room 343', 'CLASSROOM', 'Tutorial Room 343', 'room_343'),
(304, '344', 'Room 344', 'CLASSROOM', 'Tutorial Room 344', 'room_344'),
(304, '345', 'Room 345', 'CLASSROOM', 'Tutorial Room 345', 'room_345'),
(304, '346', 'Room 346', 'CLASSROOM', 'Tutorial Room 346', 'room_346'),
-- Floor 5
(305, '351', 'Room 351', 'CLASSROOM', 'Tutorial Room 351', 'room_351'),
(305, '352', 'Room 352', 'CLASSROOM', 'Tutorial Room 352', 'room_352'),
(305, '353', 'Room 353', 'CLASSROOM', 'Tutorial Room 353', 'room_353'),
(305, '354', 'Room 354', 'CLASSROOM', 'Tutorial Room 354', 'room_354'),
(305, '355', 'Room 355', 'CLASSROOM', 'Tutorial Room 355', 'room_355'),
(305, '356', 'Room 356', 'CLASSROOM', 'Tutorial Room 356', 'room_356'),
-- Floor 6
(306, '361', 'Room 361', 'CLASSROOM', 'Tutorial Room 361', 'room_361'),
(306, '362', 'Room 362', 'CLASSROOM', 'Tutorial Room 362', 'room_362'),
(306, '363', 'Room 363', 'CLASSROOM', 'Tutorial Room 363', 'room_363'),
(306, '364', 'Room 364', 'CLASSROOM', 'Tutorial Room 364', 'room_364'),
(306, '365', 'Room 365', 'CLASSROOM', 'Tutorial Room 365', 'room_365'),
(306, '366', 'Room 366', 'CLASSROOM', 'Tutorial Room 366', 'room_366'),

-- Building 4 Rooms (411 - 466)
-- Floor 1
(401, '411', 'AI Research Lab', 'LABORATORY', 'Artificial Intelligence & NLP Research Lab', 'room_411'),
(401, '412', 'K-Lab', 'LABORATORY', 'Innovation and Knowledge Laboratory', 'room_412'),
(401, '413', 'Room 413', 'CLASSROOM', 'Seminar Room 413', 'room_413'),
(401, '414', 'Room 414', 'CLASSROOM', 'Seminar Room 414', 'room_414'),
(401, '415', 'Room 415', 'CLASSROOM', 'Seminar Room 415', 'room_415'),
(401, '416', 'Room 416', 'CLASSROOM', 'Seminar Room 416', 'room_416'),
-- Floor 2
(402, '421', 'High Performance Computing Lab', 'LABORATORY', 'HPC and Cloud Computing Lab', 'room_421'),
(402, '422', 'Room 422', 'CLASSROOM', 'Seminar Room 422', 'room_422'),
(402, '423', 'Room 423', 'CLASSROOM', 'Seminar Room 423', 'room_423'),
(402, '424', 'Room 424', 'CLASSROOM', 'Seminar Room 424', 'room_424'),
(402, '425', 'Room 425', 'CLASSROOM', 'Seminar Room 425', 'room_425'),
(402, '426', 'Room 426', 'CLASSROOM', 'Seminar Room 426', 'room_426'),
-- Floor 3
(403, '431', 'Robotics Lab', 'LABORATORY', 'Robotics and Automation Lab', 'room_431'),
(403, '432', 'Room 432', 'CLASSROOM', 'Seminar Room 432', 'room_432'),
(403, '433', 'Room 433', 'CLASSROOM', 'Seminar Room 433', 'room_433'),
(403, '434', 'Room 434', 'CLASSROOM', 'Seminar Room 434', 'room_434'),
(403, '435', 'Room 435', 'CLASSROOM', 'Seminar Room 435', 'room_435'),
(403, '436', 'Room 436', 'CLASSROOM', 'Seminar Room 436', 'room_436'),
-- Floor 4
(404, '441', 'Room 441', 'CLASSROOM', 'Seminar Room 441', 'room_441'),
(404, '442', 'Room 442', 'CLASSROOM', 'Seminar Room 442', 'room_442'),
(404, '443', 'Room 443', 'CLASSROOM', 'Seminar Room 443', 'room_443'),
(404, '444', 'Room 444', 'CLASSROOM', 'Seminar Room 444', 'room_444'),
(404, '445', 'Room 445', 'CLASSROOM', 'Seminar Room 445', 'room_445'),
(404, '446', 'Room 446', 'CLASSROOM', 'Seminar Room 446', 'room_446'),
-- Floor 5
(405, '451', 'Room 451', 'CLASSROOM', 'Seminar Room 451', 'room_451'),
(405, '452', 'Room 452', 'CLASSROOM', 'Seminar Room 452', 'room_452'),
(405, '453', 'Room 453', 'CLASSROOM', 'Seminar Room 453', 'room_453'),
(405, '454', 'Room 454', 'CLASSROOM', 'Seminar Room 454', 'room_454'),
(405, '455', 'Room 455', 'CLASSROOM', 'Seminar Room 455', 'room_455'),
(405, '456', 'Room 456', 'CLASSROOM', 'Seminar Room 456', 'room_456'),
-- Floor 6
(406, '461', 'Room 461', 'CLASSROOM', 'Seminar Room 461', 'room_461'),
(406, '462', 'Room 462', 'CLASSROOM', 'Seminar Room 462', 'room_462'),
(406, '463', 'Room 463', 'CLASSROOM', 'Seminar Room 463', 'room_463'),
(406, '464', 'Room 464', 'CLASSROOM', 'Seminar Room 464', 'room_464'),
(406, '465', 'Room 465', 'CLASSROOM', 'Seminar Room 465', 'room_465'),
(406, '466', 'Room 466', 'CLASSROOM', 'Seminar Room 466', 'room_466'),

-- Multipurpose Hall (Special Room on Building 1, Floor 1)
(101, 'MPH', 'Multipurpose Hall', 'MULTIPURPOSE_HALL', 'Grand Multipurpose Hall for campus events and assemblies', 'room_mph')
ON CONFLICT (floor_id, room_number) DO NOTHING;
