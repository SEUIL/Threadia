package com.threadia.backend.company.dto;

import jakarta.validation.constraints.Size;

public record CompanyLogoRequest(@Size(max = 500) String logoUrl) {
}
