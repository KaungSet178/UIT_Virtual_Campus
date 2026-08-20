package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.SectionResponse;
import com.kaung.uit_virtual_campus.service.SectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sections")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping
    public List<SectionResponse> listSections() {
        return sectionService.getAllSections();
    }

    @GetMapping("/{sectionId}")
    public SectionResponse getSection(@PathVariable Long sectionId) {
        return sectionService.getSectionById(sectionId);
    }
}
