package com.threadia.backend.document.dto;
import com.threadia.backend.document.domain.DocumentType;
import com.threadia.backend.document.validation.TrimmedSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record DocumentCreateRequest(@NotBlank @TrimmedSize(max = 200) String title, @NotNull DocumentType type, @NotBlank String content) { }
