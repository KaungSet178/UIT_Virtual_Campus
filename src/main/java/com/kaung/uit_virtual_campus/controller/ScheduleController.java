package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.ScheduleRequest;
import com.kaung.uit_virtual_campus.dto.ScheduleResponse;
import com.kaung.uit_virtual_campus.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/timetable")
    public List<ScheduleResponse> getTimetable(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long academicGroupId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long roomId
    ) {
        return scheduleService.getTimetable(semesterId, academicGroupId, subjectId, roomId);
    }

    @GetMapping("/timetable/me")
    @PreAuthorize("hasAnyRole('STUDENT', 'CLUB_LEADER')")
    public List<ScheduleResponse> getCurrentStudentTimetable() {
        return scheduleService.getCurrentStudentTimetable();
    }

    @GetMapping("/schedules")
    public List<ScheduleResponse> listSchedules(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long academicGroupId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long roomId
    ) {
        return scheduleService.getTimetable(semesterId, academicGroupId, subjectId, roomId);
    }

    @GetMapping("/schedules/{scheduleId}")
    public ScheduleResponse getSchedule(@PathVariable Long scheduleId) {
        return scheduleService.getScheduleById(scheduleId);
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleResponse> createSchedule(@Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse response = scheduleService.createSchedule(request);
        return ResponseEntity
                .created(URI.create("/api/schedules/" + response.scheduleId()))
                .body(response);
    }

    @PutMapping("/schedules/{scheduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ScheduleResponse updateSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleRequest request
    ) {
        return scheduleService.updateSchedule(scheduleId, request);
    }

    @DeleteMapping("/schedules/{scheduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }
}
