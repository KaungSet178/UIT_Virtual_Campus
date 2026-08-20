package com.kaung.uit_virtual_campus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record ClubGalleryCreateRequest(
        @NotBlank @Size(max = 1000) @URL @Pattern(regexp = "https?://.*") String imageUrl,
        @Size(max = 500) String caption
) {
}
