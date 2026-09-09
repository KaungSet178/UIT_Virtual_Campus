package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.EventCreateRequest;
import com.kaung.uit_virtual_campus.dto.EventResponse;
import com.kaung.uit_virtual_campus.dto.EventUpdateRequest;
import com.kaung.uit_virtual_campus.entity.Club;
import com.kaung.uit_virtual_campus.entity.Event;
import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.EventRepository;
import com.kaung.uit_virtual_campus.repository.ClubMembershipRepository;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ClubService clubService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubMembershipRepository clubMembershipRepository;

    private EventService eventService;
    private User admin;
    private User leader;
    private User otherLeader;
    private User student;
    private Club club;
    private Club otherClub;
    private OffsetDateTime start;
    private OffsetDateTime end;

    @BeforeEach
    void setUp() {
        ClubAuthorizationService authorizationService = new ClubAuthorizationService();
        eventService = new EventService(
                eventRepository,
                clubService,
                currentUserService,
                authorizationService,
                new EventMapper(),
                Clock.fixed(Instant.parse("2026-08-31T08:00:00Z"), ZoneOffset.UTC),
                notificationService,
                userRepository,
                clubMembershipRepository
        );
        Role adminRole = new Role((short) 1, "ADMIN");
        Role leaderRole = new Role((short) 2, "CLUB_LEADER");
        Role studentRole = new Role((short) 3, "STUDENT");
        admin = user("admin@example.com", "Admin", Set.of(adminRole));
        leader = user("leader@example.com", "Leader", Set.of(leaderRole));
        otherLeader = user("other@example.com", "Other Leader", Set.of(leaderRole));
        student = user("student@example.com", "Student", Set.of(studentRole));
        club = new Club(10L, leader, "Programming Club", "Desc", null, null, null, null, null);
        otherClub = new Club(20L, otherLeader, "Robotics Club", "Desc", null, null, null, null, null);
        start = OffsetDateTime.parse("2026-09-01T09:00:00Z");
        end = OffsetDateTime.parse("2026-09-01T11:00:00Z");
    }

    @Test
    @DisplayName("Authenticated user reads events")
    void authenticatedUserReadsEvents() {
        when(eventRepository.searchWithDetails(null, null, null, null))
                .thenReturn(List.of(event(50L, admin, null)));

        List<EventResponse> responses = eventService.listEvents(null, null, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().title()).isEqualTo("Event");
    }

    @Test
    @DisplayName("Admin creates university event")
    void adminCreatesUniversityEvent() {
        EventCreateRequest request = createRequest(null);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            return new Event(50L, event.getCreatedBy(), event.getClub(), event.getTitle(), event.getDescription(),
                    event.getStartDatetime(), event.getEndDatetime(), event.getLocation(), event.getImageUrl(), null, null);
        });

        EventResponse response = eventService.createEvent(request);

        assertThat(response.eventId()).isEqualTo(50L);
        assertThat(response.clubId()).isNull();
        assertThat(response.createdById()).isEqualTo(admin.getUserId());
    }

    @Test
    @DisplayName("Admin creates club event")
    void adminCreatesClubEvent() {
        EventCreateRequest request = createRequest(10L);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(clubService.findClubWithLeader(10L)).thenReturn(club);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> savedEvent(50L, invocation.getArgument(0)));

        EventResponse response = eventService.createEvent(request);

        assertThat(response.clubId()).isEqualTo(10L);
        assertThat(response.createdById()).isEqualTo(admin.getUserId());
    }

    @Test
    @DisplayName("Creating event for nonexistent club returns not found")
    void createEventForNonexistentClubReturnsNotFound() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(clubService.findClubWithLeader(999L))
                .thenThrow(new ResourceNotFoundException("Club not found with id: 999"));

        assertThatThrownBy(() -> eventService.createEvent(createRequest(999L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Club not found with id: 999");
    }

    @Test
    @DisplayName("Club leader creates event for own club and Multipurpose Hall location is normalized")
    void leaderCreatesOwnClubEvent() {
        EventCreateRequest request = new EventCreateRequest(10L, " Event ", null, start, end, " multipurpose hall ", null);
        when(currentUserService.getCurrentUser()).thenReturn(leader);
        when(clubService.findClubWithLeader(10L)).thenReturn(club);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> savedEvent(50L, invocation.getArgument(0)));

        EventResponse response = eventService.createEvent(request);

        assertThat(response.clubId()).isEqualTo(10L);
        assertThat(response.location()).isEqualTo("Multipurpose Hall");
    }

    @Test
    @DisplayName("Club leader cannot create event for another club")
    void leaderCannotCreateForAnotherClub() {
        when(currentUserService.getCurrentUser()).thenReturn(leader);
        when(clubService.findClubWithLeader(20L)).thenReturn(otherClub);

        assertThatThrownBy(() -> eventService.createEvent(createRequest(20L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Club leader cannot create university-wide event")
    void leaderCannotCreateUniversityEvent() {
        when(currentUserService.getCurrentUser()).thenReturn(leader);

        assertThatThrownBy(() -> eventService.createEvent(createRequest(null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Student cannot create club event")
    void studentCannotCreateClubEvent() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(clubService.findClubWithLeader(10L)).thenReturn(club);

        assertThatThrownBy(() -> eventService.createEvent(createRequest(10L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Leader updates own club event without changing club")
    void leaderUpdatesOwnClubEvent() {
        Event existing = event(50L, leader, club);
        EventUpdateRequest request = new EventUpdateRequest("Updated", "Desc", start, end, "MULTIPURPOSE HALL", null);
        when(currentUserService.getCurrentUser()).thenReturn(leader);
        when(eventRepository.findByIdWithDetails(50L)).thenReturn(Optional.of(existing));

        EventResponse response = eventService.updateEvent(50L, request);

        assertThat(response.title()).isEqualTo("Updated");
        assertThat(response.clubId()).isEqualTo(10L);
        assertThat(response.location()).isEqualTo("Multipurpose Hall");
    }

    @Test
    @DisplayName("Leader cannot update another leader's club event")
    void leaderCannotUpdateOtherClubEvent() {
        when(currentUserService.getCurrentUser()).thenReturn(leader);
        when(eventRepository.findByIdWithDetails(50L)).thenReturn(Optional.of(event(50L, otherLeader, otherClub)));

        assertThatThrownBy(() -> eventService.updateEvent(50L, updateRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Leader cannot delete another leader's club event")
    void leaderCannotDeleteOtherClubEvent() {
        when(currentUserService.getCurrentUser()).thenReturn(leader);
        when(eventRepository.findByIdWithDetails(50L)).thenReturn(Optional.of(event(50L, otherLeader, otherClub)));

        assertThatThrownBy(() -> eventService.deleteEvent(50L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Admin can manage any event")
    void adminCanDeleteAnyEvent() {
        Event existing = event(50L, otherLeader, otherClub);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(eventRepository.findByIdWithDetails(50L)).thenReturn(Optional.of(existing));

        eventService.deleteEvent(50L);

        verify(eventRepository).delete(existing);
    }

    @Test
    @DisplayName("Club events with deleted club become admin-only")
    void deletedClubEventIsAdminOnly() {
        when(currentUserService.getCurrentUser()).thenReturn(leader);
        when(eventRepository.findByIdWithDetails(50L)).thenReturn(Optional.of(event(50L, leader, null)));

        assertThatThrownBy(() -> eventService.deleteEvent(50L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Invalid date range is rejected")
    void invalidDateRangeRejected() {
        EventCreateRequest request = new EventCreateRequest(10L, "Event", null, end, start, null, null);

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Invalid filter date range is rejected")
    void invalidFilterRangeRejected() {
        assertThatThrownBy(() -> eventService.listEvents(null, end, start, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Upcoming uses event end datetime greater than or equal to current time")
    void upcomingUsesEndDatetime() {
        when(eventRepository.findUpcomingByLocationWithDetails(eq(OffsetDateTime.parse("2026-08-31T08:00:00Z")), eq("Multipurpose Hall")))
                .thenReturn(List.of());

        eventService.getUpcomingEvents(null, " multipurpose hall ");

        verify(eventRepository).findUpcomingByLocationWithDetails(
                OffsetDateTime.parse("2026-08-31T08:00:00Z"),
                "Multipurpose Hall"
        );
    }

    @Test
    @DisplayName("Upcoming with no filters does not pass nullable location into LOWER")
    void upcomingNoFiltersUsesNoLocationQuery() {
        when(eventRepository.findUpcomingWithDetails(OffsetDateTime.parse("2026-08-31T08:00:00Z")))
                .thenReturn(List.of(event(50L, admin, null)));

        List<EventResponse> responses = eventService.getUpcomingEvents(null, null);

        assertThat(responses).hasSize(1);
        verify(eventRepository).findUpcomingWithDetails(OffsetDateTime.parse("2026-08-31T08:00:00Z"));
    }

    @Test
    @DisplayName("Upcoming club-only filter uses club-specific query")
    void upcomingClubOnlyUsesClubQuery() {
        when(eventRepository.findUpcomingByClubWithDetails(OffsetDateTime.parse("2026-08-31T08:00:00Z"), 10L))
                .thenReturn(List.of(event(50L, leader, club)));

        List<EventResponse> responses = eventService.getUpcomingEvents(10L, " ");

        assertThat(responses).hasSize(1);
        verify(eventRepository).findUpcomingByClubWithDetails(OffsetDateTime.parse("2026-08-31T08:00:00Z"), 10L);
    }

    @Test
    @DisplayName("Upcoming club and location filter uses combined query")
    void upcomingClubAndLocationUsesCombinedQuery() {
        when(eventRepository.findUpcomingByClubAndLocationWithDetails(
                OffsetDateTime.parse("2026-08-31T08:00:00Z"),
                10L,
                "Multipurpose Hall"
        )).thenReturn(List.of(event(50L, leader, club)));

        List<EventResponse> responses = eventService.getUpcomingEvents(10L, "multipurpose hall");

        assertThat(responses).hasSize(1);
        verify(eventRepository).findUpcomingByClubAndLocationWithDetails(
                OffsetDateTime.parse("2026-08-31T08:00:00Z"),
                10L,
                "Multipurpose Hall"
        );
    }

    @Test
    @DisplayName("Non-upcoming location filter uses location-specific query")
    void listEventsLocationUsesLocationQuery() {
        when(eventRepository.searchWithDetails(null, null, null, "Multipurpose Hall"))
                .thenReturn(List.of(event(50L, admin, null)));

        List<EventResponse> responses = eventService.listEvents(null, null, null, " multipurpose hall ");

        assertThat(responses).hasSize(1);
        verify(eventRepository).searchWithDetails(null, null, null, "Multipurpose Hall");
    }

    @Test
    @DisplayName("Non-upcoming date filters use dynamic search query")
    void listEventsDateFiltersUseDynamicSearchQuery() {
        when(eventRepository.searchWithDetails(10L, start, end, "Multipurpose Hall"))
                .thenReturn(List.of(event(50L, leader, club)));

        List<EventResponse> responses = eventService.listEvents(10L, start, end, " multipurpose hall ");

        assertThat(responses).hasSize(1);
        verify(eventRepository).searchWithDetails(10L, start, end, "Multipurpose Hall");
    }

    private User user(String email, String name, Set<Role> roles) {
        return new User(UUID.randomUUID(), email, name, null, LocalDateTime.now(), LocalDateTime.now(), roles);
    }

    private EventCreateRequest createRequest(Long clubId) {
        return new EventCreateRequest(clubId, "Event", "Desc", start, end, "Room 111", "https://example.com/event.jpg");
    }

    private EventUpdateRequest updateRequest() {
        return new EventUpdateRequest("Event", "Desc", start, end, "Room 111", null);
    }

    private Event event(Long eventId, User createdBy, Club club) {
        return new Event(eventId, createdBy, club, "Event", "Desc", start, end, "Room 111", null, null, null);
    }

    private Event savedEvent(Long eventId, Event event) {
        return new Event(eventId, event.getCreatedBy(), event.getClub(), event.getTitle(), event.getDescription(),
                event.getStartDatetime(), event.getEndDatetime(), event.getLocation(), event.getImageUrl(), null, null);
    }
}
