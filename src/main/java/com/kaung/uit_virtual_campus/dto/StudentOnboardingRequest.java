package com.kaung.uit_virtual_campus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentOnboardingRequest(
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Size(max = 50) String studentNumber,
        @NotNull Long academicGroupId,
        @Size(max = 50) String phone
) {
}
