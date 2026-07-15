package com.threadia.backend.document.dto;
import jakarta.validation.constraints.NotNull;
public record DocumentLinkCreateRequest(@NotNull Long documentId, @NotNull Long issueId) { }
