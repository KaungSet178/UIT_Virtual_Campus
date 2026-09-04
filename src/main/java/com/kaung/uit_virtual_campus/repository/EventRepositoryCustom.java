package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Event;

import java.time.OffsetDateTime;
import java.util.List;

public interface EventRepositoryCustom {

    List<Event> searchWithDetails(Long clubId, OffsetDateTime from, OffsetDateTime to, String location);
}
