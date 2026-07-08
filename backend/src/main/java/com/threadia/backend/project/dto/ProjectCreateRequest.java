package com.threadia.backend.project.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.threadia.backend.project.domain.ProjectStatus;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
		@NotBlank @Size(max = 150) String name,
		@NotBlank @Size(max = 20) String projectKey,
		String description,
		ProjectStatus status,
		LocalDate startDate,
		LocalDate endDate) {

	@JsonIgnore
	@AssertTrue(message = "프로젝트 생성 시 ARCHIVED 상태는 사용할 수 없습니다.")
	public boolean isCreatableStatus() {
		return status == null || status != ProjectStatus.ARCHIVED;
	}

	@JsonIgnore
	@AssertTrue(message = "프로젝트 종료일은 시작일보다 빠를 수 없습니다.")
	public boolean isValidDateRange() {
		return startDate == null || endDate == null || !startDate.isAfter(endDate);
	}
}
