package com.kaung.uit_virtual_campus.dto;

public record AdminDashboardSummaryResponse(
        long totalUsers,
        long totalStudents,
        long totalRooms,
        long pendingReservations,
        long totalClubs,
        long upcomingEvents,
        long totalSchedules
) {
}
