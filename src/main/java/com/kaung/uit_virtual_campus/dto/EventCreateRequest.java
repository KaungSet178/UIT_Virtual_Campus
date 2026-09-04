package com.kaung.uit_virtual_campus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.OffsetDateTime;

public record EventCreateRequest(
        Long clubId,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String description,
        @NotNull OffsetDateTime startDatetime,
        @NotNull OffsetDateTime endDatetime,
        @Size(max = 500) String location,
        @Size(max = 1000) @URL @Pattern(regexp = "https?://.*") String imageUrl
) {
}
