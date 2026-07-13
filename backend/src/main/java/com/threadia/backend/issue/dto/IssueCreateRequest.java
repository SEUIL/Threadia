package com.threadia.backend.issue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IssueCreateRequest(@NotBlank @Size(max = 200) String title) {
}
