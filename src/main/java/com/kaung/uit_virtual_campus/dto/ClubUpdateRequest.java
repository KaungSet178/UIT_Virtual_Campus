package com.kaung.uit_virtual_campus.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClubUpdateRequest(
        @NotBlank @Size(max = 150) String clubName,
        @Size(max = 2000) String description,
        @Email @Size(max = 255) String contactEmail,
        @Size(max = 50) String contactPhone,
        @Size(max = 1000) String logoUrl
) {
}
