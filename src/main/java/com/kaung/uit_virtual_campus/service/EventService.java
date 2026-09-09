package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.EventCreateRequest;
import com.kaung.uit_virtual_campus.dto.EventResponse;
import com.kaung.uit_virtual_campus.dto.EventUpdateRequest;
import com.kaung.uit_virtual_campus.entity.Club;
import com.kaung.uit_virtual_campus.entity.ClubMembershipStatus;
import com.kaung.uit_virtual_campus.entity.Event;
import com.kaung.uit_virtual_campus.entity.NotificationEntityType;
import com.kaung.uit_virtual_campus.entity.NotificationType;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.ClubMembershipRepository;
import com.kaung.uit_virtual_campus.repository.EventRepository;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EventService {

    private static final String MULTIPURPOSE_HALL_LOCATION = "Multipurpose Hall";

    private final EventRepository eventRepository;
    private final ClubService clubService;
    private final CurrentUserService currentUserService;
    private final ClubAuthorizationService authorizationService;
    private final EventMapper eventMapper;
    private final Clock clock;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ClubMembershipRepository clubMembershipRepository;

    public EventService(
            EventRepository eventRepository,
            ClubService clubService,
            CurrentUserService currentUserService,
            ClubAuthorizationService authorizationService,
            EventMapper eventMapper,
            Clock clock,
            NotificationService notificationService,
            UserRepository userRepository,
            ClubMembershipRepository clubMembershipRepository
    ) {
        this.eventRepository = eventRepository;
        this.clubService = clubService;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.eventMapper = eventMapper;
        this.clock = clock;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.clubMembershipRepository = clubMembershipRepository;
    }

    public List<EventResponse> listEvents(Long clubId, OffsetDateTime from, OffsetDateTime to, String location) {
        validateFilterRange(from, to);
        String normalizedLocation = normalizeLocation(location);
        return eventRepository.searchWithDetails(clubId, from, to, normalizedLocation).stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    public List<EventResponse> getUpcomingEvents(Long clubId, String location) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        String normalizedLocation = normalizeLocation(location);
        return findUpcomingEvents(now, clubId, normalizedLocation).stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    public EventResponse getEvent(Long eventId) {
        return eventMapper.toResponse(findEventWithDetails(eventId));
    }

    @Transactional
    public EventResponse createEvent(EventCreateRequest request) {
        validateDateRange(request.startDatetime(), request.endDatetime());
        User currentUser = currentUserService.getCurrentUser();
        Club club = resolveCreateClub(request.clubId(), currentUser);

        Event saved = eventRepository.save(new Event(
                currentUser,
                club,
                normalizeRequired(request.title()),
                trimToNull(request.description()),
                request.startDatetime(),
                request.endDatetime(),
                normalizeLocation(request.location()),
                trimToNull(request.imageUrl())
        ));
        notifyEventAudience(saved, NotificationType.EVENT_CREATED, eventCreatedTitle(saved), eventCreatedMessage(saved));
        return eventMapper.toResponse(saved);
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, EventUpdateRequest request) {
        validateDateRange(request.startDatetime(), request.endDatetime());
        User currentUser = currentUserService.getCurrentUser();
        Event event = findEventWithDetails(eventId);
        requireEventManager(currentUser, event);
        boolean meaningfulChange = hasMeaningfulChange(event, request);

        event.updateDetails(
                normalizeRequired(request.title()),
                trimToNull(request.description()),
                request.startDatetime(),
                request.endDatetime(),
                normalizeLocation(request.location()),
                trimToNull(request.imageUrl())
        );
        if (meaningfulChange) {
            notifyEventAudience(event, NotificationType.EVENT_UPDATED, "Event Updated",
                    event.getTitle() + " has been updated.");
        }
        return eventMapper.toResponse(event);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        User currentUser = currentUserService.getCurrentUser();
        Event event = findEventWithDetails(eventId);
        requireEventManager(currentUser, event);
        notifyEventAudience(event, NotificationType.EVENT_CANCELLED, "Event Cancelled",
                event.getTitle() + " has been cancelled.");
        eventRepository.delete(event);
    }

    private Club resolveCreateClub(Long clubId, User currentUser) {
        if (clubId == null) {
            if (!authorizationService.isAdmin(currentUser)) {
                throw new AccessDeniedException("Only admins can create university-wide events.");
            }
            return null;
        }

        Club club = clubService.findClubWithLeader(clubId);
        if (!authorizationService.canManageClub(currentUser, club)) {
            throw new AccessDeniedException("Only this club's leader or an admin can create events for this club.");
        }
        return club;
    }

    private void requireEventManager(User user, Event event) {
        Club club = event.getClub();
        if (club == null) {
            if (!authorizationService.isAdmin(user)) {
                throw new AccessDeniedException("Only admins can manage university-wide events.");
            }
            return;
        }
        if (!authorizationService.canManageClub(user, club)) {
            throw new AccessDeniedException("Only this club's leader or an admin can manage this club event.");
        }
    }

    private Event findEventWithDetails(Long eventId) {
        return eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
    }

    private List<Event> findUpcomingEvents(OffsetDateTime now, Long clubId, String location) {
        if (clubId != null && location != null) {
            return eventRepository.findUpcomingByClubAndLocationWithDetails(now, clubId, location);
        }
        if (clubId != null) {
            return eventRepository.findUpcomingByClubWithDetails(now, clubId);
        }
        if (location != null) {
            return eventRepository.findUpcomingByLocationWithDetails(now, location);
        }
        return eventRepository.findUpcomingWithDetails(now);
    }

    private void validateDateRange(OffsetDateTime startDatetime, OffsetDateTime endDatetime) {
        if (!endDatetime.isAfter(startDatetime)) {
            throw new IllegalArgumentException("End datetime must be after start datetime.");
        }
    }

    private void validateFilterRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("Filter 'to' must be after or equal to 'from'.");
        }
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeLocation(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        if (trimmed.equalsIgnoreCase(MULTIPURPOSE_HALL_LOCATION)) {
            return MULTIPURPOSE_HALL_LOCATION;
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private void notifyEventAudience(Event event, NotificationType type, String title, String message) {
        notificationService.notifyUsers(
                eventAudienceUserIds(event),
                type,
                title,
                message,
                NotificationEntityType.EVENT,
                event.getEventId()
        );
    }

    private List<UUID> eventAudienceUserIds(Event event) {
        Club club = event.getClub();
        if (club == null) {
            return userRepository.findUserIdsByRoleName("STUDENT");
        }
        return clubMembershipRepository.findStudentUserIdsByClubIdAndStatus(
                club.getClubId(),
                ClubMembershipStatus.ACTIVE
        );
    }

    private boolean hasMeaningfulChange(Event event, EventUpdateRequest request) {
        return !Objects.equals(event.getTitle(), normalizeRequired(request.title()))
                || !Objects.equals(event.getStartDatetime(), request.startDatetime())
                || !Objects.equals(event.getEndDatetime(), request.endDatetime())
                || !Objects.equals(event.getLocation(), normalizeLocation(request.location()));
    }

    private String eventCreatedTitle(Event event) {
        Club club = event.getClub();
        return club == null ? "New University Event" : "New " + club.getClubName() + " Event";
    }

    private String eventCreatedMessage(Event event) {
        Club club = event.getClub();
        if (club == null) {
            String message = event.getTitle() + " has been scheduled for "
                    + event.getStartDatetime().format(DateTimeFormatter.ofPattern("MMM d"));
            if (event.getLocation() != null) {
                message += " at " + event.getLocation();
            }
            return message + ".";
        }
        return club.getClubName() + " has scheduled " + event.getTitle() + ".";
    }
}
