package com.threadia.backend.company.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CompanyStatusConverter implements AttributeConverter<CompanyStatus, String> {

	@Override
	public String convertToDatabaseColumn(CompanyStatus status) {
		return status == null ? null : status.name();
	}

	@Override
	public CompanyStatus convertToEntityAttribute(String status) {
		return status == null ? null : CompanyStatus.valueOf(status);
	}
}
