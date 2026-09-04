package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.EventResponse;
import com.kaung.uit_virtual_campus.entity.Club;
import com.kaung.uit_virtual_campus.entity.Event;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public EventResponse toResponse(Event event) {
        Club club = event.getClub();
        return new EventResponse(
                event.getEventId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartDatetime(),
                event.getEndDatetime(),
                event.getLocation(),
                event.getImageUrl(),
                event.getCreatedBy().getUserId(),
                event.getCreatedBy().getFullName(),
                club == null ? null : club.getClubId(),
                club == null ? null : club.getClubName(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
