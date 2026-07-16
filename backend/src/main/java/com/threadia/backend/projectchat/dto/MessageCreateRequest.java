package com.threadia.backend.projectchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageCreateRequest(@NotBlank @Size(max = 10000) String content, Long threadId) {
}
