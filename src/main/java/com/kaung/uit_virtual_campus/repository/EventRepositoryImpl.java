package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Event;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventRepositoryImpl implements EventRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Event> searchWithDetails(Long clubId, OffsetDateTime from, OffsetDateTime to, String location) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> query = cb.createQuery(Event.class);
        Root<Event> event = query.from(Event.class);

        event.fetch("createdBy", JoinType.INNER);
        event.fetch("club", JoinType.LEFT).fetch("leader", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        if (clubId != null) {
            predicates.add(cb.equal(event.join("club", JoinType.INNER).get("clubId"), clubId));
        }
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(event.get("endDatetime"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(event.get("startDatetime"), to));
        }
        if (location != null) {
            predicates.add(cb.equal(cb.lower(event.get("location")), location.toLowerCase(Locale.ROOT)));
        }

        query.select(event)
                .distinct(true)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(cb.asc(event.get("startDatetime")), cb.asc(event.get("eventId")));

        return entityManager.createQuery(query).getResultList();
    }
}
