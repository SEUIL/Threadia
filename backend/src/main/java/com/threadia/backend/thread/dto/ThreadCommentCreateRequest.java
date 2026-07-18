package com.threadia.backend.thread.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ThreadCommentCreateRequest(@NotBlank @Size(max = 10000) String content) { }
