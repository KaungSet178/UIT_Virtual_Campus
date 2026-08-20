package com.kaung.uit_virtual_campus.dto;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
        UUID userId,
        String email,
        String fullName,
        List<String> roles
) {
}
