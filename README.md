# UIT Virtual Campus Backend

Spring Boot backend for UIT Virtual Campus.

## Architecture & Phases

- **Phase 1: Authentication & Core Setup** (Supabase PostgreSQL, Supabase Auth JWT resource server, application users & roles).
- **Phase 2: Campus Management** (Buildings, floors, rooms, room numbering validation, case-insensitive search, 3D model IDs).
- **Phase 3: Timetable Management** (Semesters, sections, subjects, course offerings, schedules, timetable filtering).
- **Phase 4: Room Availability & Reservations** (Reservation requests, admin approval, cancellation, calculated room availability).
- **Phase 5: Club Management** (Club profiles, memberships, leader/admin management, image URL galleries).

---

## Campus Structure & Hierarchy

The campus is modeled hierarchically:
```text
Building (1:M) -> Floor (1:M) -> Room
```

### Campus Layout:
- **Building 1** (6 floors, 6 rooms per floor: 111–166, + Multipurpose Hall)
- **Building 2** (6 floors, 6 rooms per floor: 211–266)
- **Building 3** (6 floors, 6 rooms per floor: 311–366)
- **Building 4** (6 floors, 6 rooms per floor: 411–466)
- **Multipurpose Hall**: Represented as a special room location with `roomType = "MULTIPURPOSE_HALL"` and `roomNumber = "MPH"`.

Total standard numbered rooms: 4 buildings × 6 floors × 6 rooms = **144 rooms**.

### Room Numbering Convention (ABC format):
Numbered rooms follow the 3-digit pattern **ABC**:
- **A**: Building number (`1` to `4`)
- **B**: Floor number (`1` to `6`)
- **C**: Room number on that floor (`1` to `6`)

**Examples:**
- `111` -> Building 1, Floor 1, Room 1 (Valid)
- `231` -> Building 2, Floor 3, Room 1 (Valid)
- `421` -> Building 4, Floor 2, Room 1 (Valid)
- `466` -> Building 4, Floor 6, Room 6 (Valid)
- `179` -> Invalid floor 7, room 9
- `237` -> Invalid room 7
- `501` -> Invalid building 5

*Note: Multipurpose Hall is exempt from the ABC numbering rule.*

---

## 3D Model Integration (`modelObjectId`)

Each room record contains a `modelObjectId` field (e.g., `"room_231"`, `"room_mph"`).
- The backend only stores and returns this identifier string.
- The 3D model files are not hosted in Spring Boot.
- The frontend (e.g. Three.js client) uses `modelObjectId` to interact with and highlight corresponding 3D objects in the campus model.

---

## Notice: Room Availability & Reservations

> **Note:** Room availability is calculated dynamically from official schedules and approved reservations. There is no `isAvailable` field on `Room`.

---

## Timetable Management

The timetable model uses this structure:
```text
Semester (1:M) -> CourseOffering
Section  (1:M) -> CourseOffering
Subject  (1:M) -> CourseOffering
CourseOffering (1:M) -> Schedule
Room (1:M) -> Schedule
```

A `CourseOffering` represents one subject taught to one section during one semester. The database enforces uniqueness for `(semester_id, section_id, subject_id)`.

`Schedule.dayOfWeek` uses ISO numeric day values:
```text
1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday
```

Schedules must have `endTime > startTime`. The service also rejects overlapping schedules for the same room and day using:
```text
existing.startTime < requestedEnd
AND existing.endTime > requestedStart
```
Back-to-back schedules are allowed. Because the current Supabase schema has no database exclusion constraint for time overlaps, this service-level overlap check is not fully concurrency-safe under simultaneous writes. Phase 3 does not modify the database schema.

---

## Room Availability & Reservations

Occupancy comes from:
```text
Official schedules
Approved reservations
```

Pending reservations do not make a room unavailable. A pending request is not confirmed occupancy and does not block another user from submitting a request. A reservation can only be approved if the room is still free at approval time.

Reservation status transitions:
```text
PENDING -> APPROVED
PENDING -> REJECTED
PENDING -> CANCELLED
APPROVED -> CANCELLED
```

Invalid status transitions return `409 Conflict`. If an approved reservation is cancelled, `approvedBy` is retained for approval history.

Availability and reservation conflict checks use:
```text
existing.startTime < requestedEnd
AND existing.endTime > requestedStart
```
Back-to-back bookings are allowed.

Reservation creation and approval use a pessimistic lock on the room row before conflict checks. Schedule create/update uses the same room-lock convention. This reduces race conditions inside the backend, but the database still has no exclusion constraint, so direct database writes or code paths that bypass this service can still create overlapping approved occupancy.

---

## Club Management

Clubs are managed with this structure:
```text
User (leader) 1:M Club
Club 1:M ClubMembership
StudentProfile 1:M ClubMembership
Club 1:M ClubGallery
```

Each club has exactly one leader through `leaderId`. A leader must be a student with both `ROLE_STUDENT` and `ROLE_CLUB_LEADER`, and must keep an `ACTIVE` membership in their own club. One user may lead multiple clubs.

Membership statuses:
```text
ACTIVE
LEFT
SUSPENDED
```

Students can join clubs and leave clubs they joined. A leader cannot leave their own club while they remain leader. A `LEFT` membership can be reactivated by joining again. A `SUSPENDED` membership cannot be reactivated by the student.

Only the exact club leader or an admin can edit a club profile or manage that club's gallery. `ROLE_CLUB_LEADER` alone does not allow editing another leader's club. Phase 5 stores gallery `imageUrl` values only; it does not implement image upload, Supabase Storage, events, or messaging.

---

## API Endpoints

All campus endpoints require an authenticated Bearer token.

### Buildings API
- `GET /api/buildings` — List all campus buildings.
- `GET /api/buildings/{buildingId}` — Get details of a specific building.
- `GET /api/buildings/{buildingId}/floors` — List all floors for a specific building.

### Floors API
- `GET /api/floors/{floorId}` — Get details of a specific floor.
- `GET /api/floors/{floorId}/rooms` — List all rooms on a specific floor.

### Rooms API
- `GET /api/rooms` — List all rooms across campus.
- `GET /api/rooms/{roomId}` — Get details of a specific room.
- `GET /api/rooms/search?query={query}` — Search rooms by number or name (case-insensitive).

### Timetable Reference APIs
- `GET /api/semesters` - List semesters.
- `GET /api/semesters/{semesterId}` - Get a semester.
- `GET /api/sections` - List sections.
- `GET /api/sections/{sectionId}` - Get a section.
- `GET /api/subjects` - List subjects.
- `GET /api/subjects/{subjectId}` - Get a subject.

### Course Offering APIs
- `GET /api/course-offerings` - List course offerings.
- `GET /api/course-offerings/{offeringId}` - Get a course offering.
- `GET /api/course-offerings?semesterId={id}&sectionId={id}&subjectId={id}` - Filter course offerings.
- `POST /api/course-offerings` - Create a course offering. Requires `ROLE_ADMIN`.
- `DELETE /api/course-offerings/{offeringId}` - Delete a course offering. Requires `ROLE_ADMIN`.

### Schedule & Timetable APIs
- `GET /api/timetable` - List timetable entries.
- `GET /api/timetable?semesterId={id}&sectionId={id}&subjectId={id}&roomId={id}` - Filter timetable entries.
- `GET /api/schedules` - List schedules using the same filters.
- `GET /api/schedules/{scheduleId}` - Get a schedule.
- `POST /api/schedules` - Create a schedule. Requires `ROLE_ADMIN`.
- `PUT /api/schedules/{scheduleId}` - Update a schedule. Requires `ROLE_ADMIN`.
- `DELETE /api/schedules/{scheduleId}` - Delete a schedule. Requires `ROLE_ADMIN`.

All timetable, schedule, and reference-data GET endpoints require an authenticated Bearer token.

### Reservation APIs
- `POST /api/reservations` - Create a pending room reservation request.
- `GET /api/reservations/me` - List the authenticated user's reservations.
- `GET /api/reservations/{reservationId}` - Get a reservation owned by the authenticated user, or any reservation for admins.
- `PATCH /api/reservations/{reservationId}/cancel` - Cancel a pending or approved reservation.

### Admin Reservation APIs
- `GET /api/admin/reservations` - List reservations for management. Requires `ROLE_ADMIN`.
- `GET /api/admin/reservations?status={status}&roomId={id}&userId={uuid}&date={date}` - Filter reservations. Requires `ROLE_ADMIN`.
- `PATCH /api/admin/reservations/{reservationId}/approve` - Approve a pending reservation. Requires `ROLE_ADMIN`.
- `PATCH /api/admin/reservations/{reservationId}/reject` - Reject a pending reservation. Requires `ROLE_ADMIN`.

### Availability API
- `GET /api/rooms/{roomId}/availability?date=YYYY-MM-DD&startTime=HH:mm&endTime=HH:mm` - Check calculated room availability.

Availability responses for ordinary users identify official schedule conflicts and approved reservation occupancy without exposing another reservation owner's identity, email, or purpose.

### Club APIs
- `GET /api/clubs` - List clubs.
- `GET /api/clubs/me` - List the authenticated student's club memberships.
- `GET /api/clubs/{clubId}` - Get a club profile.
- `PUT /api/clubs/{clubId}` - Update a club profile. Requires that club's leader or `ROLE_ADMIN`.
- `GET /api/clubs/{clubId}/members` - List active members with minimal public member information.
- `GET /api/clubs/{clubId}/members/manage` - List richer member details. Requires that club's leader or `ROLE_ADMIN`.
- `POST /api/clubs/{clubId}/join` - Join a club. Requires a student user.
- `PATCH /api/clubs/{clubId}/leave` - Leave a club. Requires a student user.
- `GET /api/clubs/{clubId}/gallery` - List club gallery image URLs.
- `POST /api/clubs/{clubId}/gallery` - Add a gallery image URL. Requires that club's leader or `ROLE_ADMIN`.
- `DELETE /api/clubs/{clubId}/gallery/{galleryId}` - Delete a gallery item. Requires that club's leader or `ROLE_ADMIN`.

### Admin Club APIs
- `POST /api/admin/clubs` - Create a club and ensure active leader membership. Requires `ROLE_ADMIN`.
- `DELETE /api/admin/clubs/{clubId}` - Delete a club. Requires `ROLE_ADMIN`.

### Search Examples:
- `GET /api/rooms/search?query=231` -> Matches room number `231`
- `GET /api/rooms/search?query=Database` -> Matches `Database Lab`
- `GET /api/rooms/search?query=K-Lab` -> Matches `K-Lab`
- `GET /api/rooms/search?query=multipurpose` -> Matches `Multipurpose Hall`

### Sample Room Response:
```json
{
  "roomId": 10,
  "roomNumber": "231",
  "roomName": "Database Lab",
  "roomType": "LABORATORY",
  "description": "Database and Big Data Laboratory",
  "buildingId": 2,
  "buildingName": "Building 2",
  "floorId": 203,
  "floorNumber": 3,
  "modelObjectId": "room_231"
}
```

---

## Development Seed Data

A development-only seed script is located at:
```text
src/main/resources/db/seed-campus-dev.sql
```
This script populates all 4 buildings, 24 floors, 144 numbered rooms, and the Multipurpose Hall.
It is a **manual** development fixture and is **not** executed automatically on startup or in production.

Phase 3 timetable seed data is located at:
```text
src/main/resources/db/seed-timetable-dev.sql
```
Run the campus seed first, then the timetable seed. It inserts sample semesters, sections, subjects, course offerings, and non-overlapping schedules.

Phase 4 reservation seed data is located at:
```text
src/main/resources/db/seed-reservations-dev.sql
```
It contains UUID placeholders for real development users and is intended for manual local testing only.

Phase 5 club seed data is located at:
```text
src/main/resources/db/seed-clubs-dev.sql
```
It contains UUID placeholders for real development users and is intended for manual local testing only.

---

## Configuration & Environment Variables

Set these environment variables before running:
```text
DB_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>
SUPABASE_ISSUER_URI=https://<project-ref>.supabase.co/auth/v1
SUPABASE_JWT_AUDIENCE=authenticated
```

---

## Running the Application

```bash
# Linux/macOS
./mvnw spring-boot:run

# Windows PowerShell
.\mvnw.cmd spring-boot:run
```

---

## Running Tests

```bash
# Linux/macOS
./mvnw test

# Windows PowerShell
.\mvnw.cmd test
```
The test suite uses an in-memory H2 PostgreSQL-compatible database and does not affect production data.
