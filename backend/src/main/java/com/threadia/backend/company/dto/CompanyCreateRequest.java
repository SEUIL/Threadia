package com.threadia.backend.company.dto;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyCreateRequest(
		@NotBlank @Size(max = 150) String name,
		@Size(max = 100) String representativeName,
		String description,
		@Size(max = 100) String industryCategory,
		@Size(max = 500) @URL String logoUrl) {
}
