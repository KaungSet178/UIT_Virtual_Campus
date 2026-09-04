# UIT Virtual Campus Backend

Spring Boot backend for UIT Virtual Campus.

## Architecture & Phases

- **Phase 1: Authentication & Core Setup** (Supabase PostgreSQL, Supabase Auth JWT resource server, application users & roles).
- **Phase 2: Campus Management** (Buildings, floors, rooms, room numbering validation, case-insensitive search, 3D model IDs).
- **Phase 3: Timetable Management** (Semesters, academic groups, subjects, course offerings, schedules, timetable filtering).
- **Phase 4: Room Availability & Reservations** (Reservation requests, admin approval, cancellation, calculated room availability).
- **Phase 5: Club Management** (Club profiles, memberships, leader/admin management, image URL galleries).
- **Phase 6: Events & Multipurpose Hall Integration** (University-wide events, club events, ownership-secured management, upcoming hall event queries).
- **Phase 7: Frontend Integration & API Hardening** (explicit CORS, production current-user endpoint, consistent errors, frontend contract documentation).

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
AcademicGroup (1:M) -> CourseOffering
Subject  (1:M) -> CourseOffering
CourseOffering (1:M) -> Schedule
Room (1:M) -> Schedule
```

A `CourseOffering` represents one subject taught to one academic group during one semester. The database enforces uniqueness for `(semester_id, academic_group_id, subject_id)`.

Student academic identity uses this path:
```text
User -> StudentProfile -> AcademicGroup -> CourseOffering -> Schedule
```

`AcademicGroup` is the source of truth for timetable membership. Year 1 and Year 2 groups use `groupType = "SECTION"` with `sectionName`; Year 3+ groups use `groupType = "MAJOR"` with `department` and `major`.

Examples:
```text
Year 1 Section A -> year=1, groupType=SECTION, sectionName=A
Year 2 Section B -> year=2, groupType=SECTION, sectionName=B
Year 3 CS SE     -> year=3, groupType=MAJOR, department=CS, major=SE
Year 3 CT Cybersecurity -> year=3, groupType=MAJOR, department=CT, major=Cybersecurity
```

SECTION groups are unique by `(year, section_name)`. MAJOR groups are unique by `(year, department, major)`. Obsolete `student_profiles.section` and `student_profiles.section_id` are not timetable authorities.

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

## Event Management

Events are managed with this structure:
```text
User (createdBy) 1:M Event
Club 0:M Event
```

University-wide events have `clubId = null`. Club events have a `clubId` pointing to the owning club. `createdBy` always comes from the authenticated Supabase identity through `CurrentUserService`; clients never send creator user IDs.

Authorization rules:
- `ROLE_ADMIN` can create, update, and delete any event.
- A club leader can create, update, and delete events only for clubs where `clubs.leader_id` equals their authenticated `user_id`.
- `ROLE_CLUB_LEADER` alone is not enough to manage another leader's club events.
- Club leaders cannot create university-wide events.

Event club ownership is immutable after creation. Update requests do not include `clubId`, and Phase 6 does not support event reassignment between clubs.

Upcoming events are defined as:
```text
event.endDatetime >= current time
```
This intentionally includes events that are currently in progress. Upcoming results are ordered by soonest `startDatetime` first.

The Multipurpose Hall integration uses the existing text `location` field on `public.events`. Inputs equivalent to `Multipurpose Hall`, including extra whitespace or different casing, are normalized and stored as `Multipurpose Hall`. Phase 6 does not add a room foreign key, hall event table, database enum, messaging, or automatic room reservation creation.

Schema limitation: `public.events.club_id` uses `ON DELETE SET NULL`. If a club is deleted, its historical club events may become `clubId = null`. The backend treats those events as admin-only for future management, but they should not be interpreted as originally university-wide events.

---

## API Endpoints

All campus endpoints require an authenticated Bearer token.

### Current User API
- `GET /api/me` - Return the authenticated application user, roles, profile image, and student profile when present.
- `POST /api/me/student-profile` - Complete authenticated student self-onboarding after Supabase signup/sign-in. Public self-onboarding always creates or preserves only the `STUDENT` role; `ADMIN` and `CLUB_LEADER` are not accepted from clients.

Student onboarding request:
```json
{
  "fullName": "Kaung Set Thar",
  "studentNumber": "UIT-001",
  "academicGroupId": 1,
  "phone": null
}
```

The endpoint derives identity from `jwt.sub` and email from the authenticated Supabase JWT. If the Supabase auth trigger has already inserted `public.users`, that row is completed. If the trigger has not inserted it yet, the backend creates the matching `public.users` row for the authenticated UUID. `AcademicGroup` is the timetable identity source: Year 1/2 `SECTION` groups derive profile year with null department/programme, and Year 3-5 `MAJOR` groups derive year, department, and programme from the selected group.

Sample response:
```json
{
  "userId": "44444444-4444-4444-4444-444444444444",
  "email": "student@uit.edu.mm",
  "fullName": "Student User",
  "profileImage": "https://example.com/profile.png",
  "roles": ["CLUB_LEADER", "STUDENT"],
  "studentProfile": {
    "studentNumber": "UIT-001",
    "department": "CS",
    "programme": "Software Engineering",
    "year": 3,
    "academicGroup": {
      "groupId": 6,
      "year": 3,
      "groupType": "MAJOR",
      "sectionName": null,
      "department": "CS",
      "major": "SE"
    }
  }
}
```

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
- `GET /api/academic-groups` - List academic groups.
- `GET /api/academic-groups?year={year}&groupType={SECTION|MAJOR}&department={department}` - Filter academic groups.
- `GET /api/academic-groups/{groupId}` - Get an academic group.
- `GET /api/subjects` - List subjects.
- `GET /api/subjects/{subjectId}` - Get a subject.

### Course Offering APIs
- `GET /api/course-offerings` - List course offerings.
- `GET /api/course-offerings/{offeringId}` - Get a course offering.
- `GET /api/course-offerings?semesterId={id}&academicGroupId={id}&subjectId={id}` - Filter course offerings.
- `POST /api/course-offerings` - Create a course offering. Requires `ROLE_ADMIN`.
- `DELETE /api/course-offerings/{offeringId}` - Delete a course offering. Requires `ROLE_ADMIN`.

### Schedule & Timetable APIs
- `GET /api/timetable` - List timetable entries.
- `GET /api/timetable?semesterId={id}&academicGroupId={id}&subjectId={id}&roomId={id}` - Filter timetable entries.
- `GET /api/timetable/me` - List the authenticated student's timetable for the active semester.
- `GET /api/schedules` - List schedules using the same filters.
- `GET /api/schedules/{scheduleId}` - Get a schedule.
- `POST /api/schedules` - Create a schedule. Requires `ROLE_ADMIN`.
- `PUT /api/schedules/{scheduleId}` - Update a schedule. Requires `ROLE_ADMIN`.
- `DELETE /api/schedules/{scheduleId}` - Delete a schedule. Requires `ROLE_ADMIN`.

All timetable, schedule, and reference-data GET endpoints require an authenticated Bearer token.

`GET /api/timetable/me` derives the academic group from the authenticated user's `StudentProfile`; the frontend must not send `academicGroupId`, `sectionId`, `year`, or `studentId`. The active semester is determined by the current date where `startDate <= today` and `endDate >= today`. If the student has no assigned academic group, the endpoint returns a conflict response. If no semester is active, it returns an empty list. Multiple active semesters are treated as a data conflict.

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

### Event APIs
- `GET /api/events` - List events.
- `GET /api/events?clubId={id}&from={datetime}&to={datetime}&location={location}` - Filter events by club, overlapping date/time range, and exact case-insensitive location.
- `GET /api/events/upcoming` - List upcoming events where `endDatetime >= now`, ordered soonest first.
- `GET /api/events/upcoming?location=Multipurpose%20Hall` - List upcoming Multipurpose Hall events for the frontend hall hub.
- `GET /api/events/{eventId}` - Get event details.
- `POST /api/events` - Create a university-wide or club event. University-wide events require `ROLE_ADMIN`; club events require `ROLE_ADMIN` or that club's exact leader.
- `PUT /api/events/{eventId}` - Update event details without changing club ownership. Requires `ROLE_ADMIN` or that club's exact leader.
- `DELETE /api/events/{eventId}` - Delete an event. Requires `ROLE_ADMIN` or that club's exact leader.

---

## Frontend Integration

The frontend integration architecture is:
```text
Next.js frontend
  -> Supabase Auth for login/session only
  -> Spring Boot API with Authorization: Bearer <Supabase access token>
  -> PostgreSQL through backend services
```

The frontend must not perform direct CRUD against public application tables through the Supabase Data API. Supabase can be used directly by the browser for Auth, and Storage can be considered later if an upload feature is formally added.

### Authentication Flow

1. Next.js signs in with Supabase Auth.
2. Supabase returns a session and access token.
3. Next.js sends API requests with:
   ```http
   Authorization: Bearer <access_token>
   ```
4. Spring Boot validates JWT signature, issuer, audience, and expiry.
5. Spring resolves `jwt.sub` against `public.users.user_id`.
6. Roles come from `public.user_roles` and `public.roles`.
7. Next.js calls `GET /api/me` to bootstrap identity, roles, navigation, and profile display.

The frontend must not trust a role selected in the login UI. Any role tab or persona selector used during prototyping must be removed or treated as display-only once Supabase Auth is wired.

### Environment

Backend:
```text
DB_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>
SUPABASE_ISSUER_URI=https://<project-ref>.supabase.co/auth/v1
SUPABASE_JWT_AUDIENCE=authenticated
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
```

`APP_CORS_ALLOWED_ORIGINS` accepts a comma-separated list:
```text
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,https://example.com
```

Frontend:
```text
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_SUPABASE_URL=https://<project-ref>.supabase.co
NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY=<publishable-key>
```

Never expose these in frontend code or `NEXT_PUBLIC_*` variables:
- database credentials
- Supabase `service_role` or secret keys
- backend secrets

### API Client Shape

Recommended frontend structure:
```text
lib/
  api/
    client.ts
    rooms.ts
    timetable.ts
    reservations.ts
    clubs.ts
    events.ts
  auth/
    supabase.ts
  types/
```

Example request:
```ts
await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/rooms`, {
  headers: {
    Authorization: `Bearer ${accessToken}`,
  },
});
```

Backend DTO names are the contract. The frontend should adapt old mock fields rather than renaming backend responses:
- `room.roomName` -> UI name
- `room.roomNumber` -> UI code
- `room.buildingName` -> UI building
- `reservation.status === "PENDING"` -> warning
- `reservation.status === "APPROVED"` -> success
- `reservation.status === "REJECTED"` -> error
- `reservation.status === "CANCELLED"` -> neutral

### Room And Availability Contract

Use `GET /api/rooms`, `GET /api/rooms/search?query=...`, and `GET /api/rooms/{roomId}` as the room source of truth. Do not use old mock room codes.

Room availability is calculated, not stored:
```text
GET /api/rooms/{roomId}/availability?date=YYYY-MM-DD&startTime=HH:mm&endTime=HH:mm
```

Frontend interpretation:
- no conflicts -> `AVAILABLE`
- conflict type `SCHEDULE` -> `OCCUPIED`
- conflict type `APPROVED_RESERVATION` -> `RESERVED`

If both conflict types are present, display detailed conflicts or apply a deterministic UI priority.

### Timetable Contract

`dayOfWeek` follows ISO numeric days:
```text
1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday
```

The current schema does not contain lecturer data. Personalized timetable views should use `GET /api/timetable/me`; grouping comes from `student_profiles.academic_group_id`, not a frontend-selected section or frontend inference from year/department/programme.

### Three.js Contract

`RoomResponse.modelObjectId` must match the Three.js object's `object.name` or equivalent metadata.

Important examples:
- `room_412`
- `room_321`
- `room_mph`

Multipurpose Hall uses:
```text
modelObjectId = room_mph
GET /api/events/upcoming?location=Multipurpose%20Hall
```

Room clicks can use:
```text
GET /api/rooms/{roomId}
GET /api/rooms/{roomId}/availability?date=YYYY-MM-DD&startTime=HH:mm&endTime=HH:mm
```

### Error Format

Common error envelope:
```json
{
  "timestamp": "2026-09-01T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/events",
  "fieldErrors": {
    "title": "must not be blank"
  }
}
```

`fieldErrors` is present only when field-level validation details are available.

### Unsupported Frontend Features

These are not implemented in the backend and should be hidden, disabled, or marked as future work in the frontend:
- messaging
- notifications
- student club registration approval workflow
- event Draft/Published status
- event RSVP or going count
- timetable CSV import
- lecturer/teacher timetable field
- admin user invite/edit/suspend workflow
- reservation rejection reason persistence
- club officer roles
- extra profile fields such as phone and enrolled period

---

## Supabase Security Findings

The current intended data path is frontend -> Spring Boot -> PostgreSQL. The frontend should use Supabase directly only for Auth during Phase 7.

Live Supabase findings from the Phase 7 audit:
- RLS is enabled on public application tables.
- No public-table RLS policies currently exist.
- `anon` and `authenticated` have broad table grants, currently constrained by RLS having no policies.
- Auth trigger functions `public.handle_new_auth_user()` and `public.sync_user_email_from_auth()` are `SECURITY DEFINER` functions and are executable by browser-facing roles.
- Supabase Auth leaked-password protection is disabled.

Phase 7 does not modify RLS policies, grants, `SECURITY DEFINER` functions, or schema. Treat these as follow-up database/Auth hardening tasks after confirming Auth trigger behavior and deployment requirements.

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
Run the campus seed first, then the timetable seed. It inserts sample semesters, academic groups, subjects, course offerings, and non-overlapping schedules.

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

Phase 6 event seed data is located at:
```text
src/main/resources/db/seed-events-dev.sql
```
It contains UUID and club placeholders for real development data and is intended for manual local testing only. Creating events through the authenticated API is safer because it exercises `createdBy` and authorization rules.

---

## Configuration & Environment Variables

Set these environment variables before running:
```text
DB_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>
SUPABASE_ISSUER_URI=https://<project-ref>.supabase.co/auth/v1
SUPABASE_JWT_AUDIENCE=authenticated
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
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
