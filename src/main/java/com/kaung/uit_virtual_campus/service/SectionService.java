package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.SectionResponse;
import com.kaung.uit_virtual_campus.entity.Section;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.SectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SectionService {

    private final SectionRepository sectionRepository;

    public SectionService(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    public List<SectionResponse> getAllSections() {
        return sectionRepository.findAllByOrderBySectionNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public SectionResponse getSectionById(Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + sectionId));
        return toResponse(section);
    }

    private SectionResponse toResponse(Section section) {
        return new SectionResponse(
                section.getSectionId(),
                section.getSectionName()
        );
    }
}
