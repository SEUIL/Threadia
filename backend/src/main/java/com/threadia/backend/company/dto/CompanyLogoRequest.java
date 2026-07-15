package com.threadia.backend.company.dto;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyLogoRequest(@NotBlank @Size(max = 500) @URL String logoUrl) {
}
