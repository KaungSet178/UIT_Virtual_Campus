package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Club;
import com.kaung.uit_virtual_campus.entity.Event;
import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EventRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventRepository eventRepository;

    private User admin;
    private User leader;
    private Club club;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.of(2026, 8, 31, 8, 0, 0, 0, ZoneOffset.UTC);
        Role adminRole = entityManager.persist(new Role((short) 1, "ADMIN"));
        Role leaderRole = entityManager.persist(new Role((short) 2, "CLUB_LEADER"));
        admin = entityManager.persist(new User(UUID.randomUUID(), "admin@example.com", "Admin", null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of(adminRole)));
        leader = entityManager.persist(new User(UUID.randomUUID(), "leader@example.com", "Leader", null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of(leaderRole)));
        club = entityManager.persist(new Club(leader, "Programming Club", "Desc", null, null, null));

        entityManager.persist(new Event(
                admin,
                null,
                "Current Hall Event",
                "Campus event",
                now.minusHours(1),
                now.plusHours(1),
                "Multipurpose Hall",
                null
        ));
        entityManager.persist(new Event(
                leader,
                club,
                "Future Club Event",
                "Club event",
                now.plusDays(1),
                now.plusDays(1).plusHours(2),
                "Lab 1",
                null
        ));
        entityManager.persist(new Event(
                admin,
                null,
                "Past Event",
                "Past",
                now.minusDays(2),
                now.minusDays(2).plusHours(1),
                "Multipurpose Hall",
                null
        ));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Finds event detail with creator and nullable club")
    void findsEventDetailWithCreatorAndClub() {
        Event event = eventRepository.searchWithDetails(null, null, null, "Lab 1").getFirst();

        Optional<Event> found = eventRepository.findByIdWithDetails(event.getEventId());

        assertThat(found).isPresent();
        assertThat(found.get().getCreatedBy().getFullName()).isEqualTo("Leader");
        assertThat(found.get().getClub().getClubName()).isEqualTo("Programming Club");
        assertThat(found.get().getClub().getLeader().getUserId()).isEqualTo(leader.getUserId());
    }

    @Test
    @DisplayName("Upcoming includes in-progress events and orders soonest first")
    void upcomingIncludesInProgressAndOrdersSoonestFirst() {
        List<Event> upcoming = eventRepository.findUpcomingWithDetails(now);

        assertThat(upcoming).extracting(Event::getTitle)
                .containsExactly("Current Hall Event", "Future Club Event");
    }

    @Test
    @DisplayName("Filters events by club")
    void filtersByClub() {
        List<Event> events = eventRepository.searchWithDetails(club.getClubId(), null, null, null);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getTitle()).isEqualTo("Future Club Event");
    }

    @Test
    @DisplayName("Filters events by overlapping date range")
    void filtersByDateRange() {
        List<Event> events = eventRepository.searchWithDetails(null, now.minusMinutes(30), now.plusMinutes(30), null);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getTitle()).isEqualTo("Current Hall Event");
    }

    @Test
    @DisplayName("Filters events by location for Multipurpose Hall")
    void filtersByLocation() {
        List<Event> events = eventRepository.findUpcomingByLocationWithDetails(now, "Multipurpose Hall");

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getLocation()).isEqualTo("Multipurpose Hall");
    }

    @Test
    @DisplayName("Upcoming no-location query succeeds without nullable LOWER parameter")
    void upcomingWithoutLocationSucceeds() {
        List<Event> events = eventRepository.findUpcomingWithDetails(now);

        assertThat(events).extracting(Event::getTitle)
                .containsExactly("Current Hall Event", "Future Club Event");
    }

    @Test
    @DisplayName("Upcoming location filter is case-insensitive")
    void upcomingLocationFilterIsCaseInsensitive() {
        List<Event> events = eventRepository.findUpcomingByLocationWithDetails(now, "multipurpose hall");

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getTitle()).isEqualTo("Current Hall Event");
    }

    @Test
    @DisplayName("Upcoming club-only filter succeeds")
    void upcomingClubOnlyFilterSucceeds() {
        List<Event> events = eventRepository.findUpcomingByClubWithDetails(now, club.getClubId());

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getTitle()).isEqualTo("Future Club Event");
    }

    @Test
    @DisplayName("Upcoming club and location filter succeeds")
    void upcomingClubAndLocationFilterSucceeds() {
        List<Event> events = eventRepository.findUpcomingByClubAndLocationWithDetails(now, club.getClubId(), "Lab 1");

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getTitle()).isEqualTo("Future Club Event");
    }

    @Test
    @DisplayName("Non-upcoming club and location filter succeeds")
    void filteredClubAndLocationSucceeds() {
        List<Event> events = eventRepository.searchWithDetails(club.getClubId(), null, null, "lab 1");

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getTitle()).isEqualTo("Future Club Event");
    }

    @Test
    @DisplayName("Admin university event create populates audit timestamps")
    void universityEventCreatePopulatesAuditTimestamps() {
        Event event = eventRepository.saveAndFlush(new Event(
                admin,
                null,
                "Awarding Ceremony",
                "Campus event",
                now.plusDays(2),
                now.plusDays(2).plusHours(1),
                "Multipurpose Hall",
                null
        ));

        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Admin club event create populates audit timestamps")
    void clubEventCreatePopulatesAuditTimestamps() {
        Event event = eventRepository.saveAndFlush(new Event(
                admin,
                club,
                "Club Demo",
                "Club event",
                now.plusDays(3),
                now.plusDays(3).plusHours(1),
                "Lab 1",
                null
        ));

        assertThat(event.getClub().getClubId()).isEqualTo(club.getClubId());
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Event update preserves createdAt and refreshes updatedAt")
    void eventUpdatePreservesCreatedAtAndRefreshesUpdatedAt() {
        Event event = eventRepository.saveAndFlush(new Event(
                admin,
                null,
                "Original",
                "Campus event",
                now.plusDays(2),
                now.plusDays(2).plusHours(1),
                "Multipurpose Hall",
                null
        ));
        OffsetDateTime createdAt = event.getCreatedAt();
        OffsetDateTime initialUpdatedAt = event.getUpdatedAt();

        event.updateDetails("Updated", "Updated event", now.plusDays(4), now.plusDays(4).plusHours(1), "Room 101", null);
        entityManager.flush();

        assertThat(event.getCreatedAt()).isEqualTo(createdAt);
        assertThat(event.getUpdatedAt()).isAfterOrEqualTo(initialUpdatedAt);
    }

    @Test
    @DisplayName("List search with no filters succeeds")
    void listSearchWithNoFiltersSucceeds() {
        List<Event> events = eventRepository.searchWithDetails(null, null, null, null);

        assertThat(events).extracting(Event::getTitle)
                .containsExactly("Past Event", "Current Hall Event", "Future Club Event");
    }

    @Test
    @DisplayName("List search with from-only filter succeeds")
    void listSearchWithFromOnlySucceeds() {
        List<Event> events = eventRepository.searchWithDetails(null, now, null, null);

        assertThat(events).extracting(Event::getTitle)
                .containsExactly("Current Hall Event", "Future Club Event");
    }

    @Test
    @DisplayName("List search with to-only filter succeeds")
    void listSearchWithToOnlySucceeds() {
        List<Event> events = eventRepository.searchWithDetails(null, null, now, null);

        assertThat(events).extracting(Event::getTitle)
                .containsExactly("Past Event", "Current Hall Event");
    }

    @Test
    @DisplayName("List search with location and date range succeeds")
    void listSearchWithLocationAndDateRangeSucceeds() {
        List<Event> events = eventRepository.searchWithDetails(null, now.minusMinutes(30), now.plusMinutes(30), "multipurpose hall");

        assertThat(events).extracting(Event::getTitle)
                .containsExactly("Current Hall Event");
    }

    @Test
    @DisplayName("List search with club and date range succeeds")
    void listSearchWithClubAndDateRangeSucceeds() {
        List<Event> events = eventRepository.searchWithDetails(club.getClubId(), now, now.plusDays(2), null);

        assertThat(events).extracting(Event::getTitle)
                .containsExactly("Future Club Event");
    }
}
