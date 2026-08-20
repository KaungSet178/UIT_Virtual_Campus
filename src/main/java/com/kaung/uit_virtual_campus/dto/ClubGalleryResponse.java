package com.kaung.uit_virtual_campus.dto;

import java.time.OffsetDateTime;

public record ClubGalleryResponse(
        Long galleryId,
        Long clubId,
        String imageUrl,
        String caption,
        OffsetDateTime uploadedAt
) {
}
