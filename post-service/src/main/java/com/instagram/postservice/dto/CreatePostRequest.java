package com.instagram.postservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Frontend uploads the image to Cloudinary and sends only the resulting URL. */
public record CreatePostRequest(
        @NotBlank String imageUrl,
        @Size(max = 2200) String caption) {
}
