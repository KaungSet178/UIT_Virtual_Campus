package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findAllByOrderBySubjectCodeAsc();
}
