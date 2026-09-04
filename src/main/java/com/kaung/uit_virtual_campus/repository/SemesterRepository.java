package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    List<Semester> findAllByOrderByStartDateDescSemesterIdDesc();

    List<Semester> findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscSemesterIdAsc(
            LocalDate startsOnOrBefore,
            LocalDate endsOnOrAfter
    );
}
