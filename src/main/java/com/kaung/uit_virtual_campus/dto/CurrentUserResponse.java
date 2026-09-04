package com.kaung.uit_virtual_campus.dto;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
        UUID userId,
        String email,
        String fullName,
        String profileImage,
        String phone,
        List<String> roles,
        StudentProfileResponse studentProfile,
        StaffProfileResponse staffProfile
) {
}
