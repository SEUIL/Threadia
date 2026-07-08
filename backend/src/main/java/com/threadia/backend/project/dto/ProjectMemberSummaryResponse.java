package com.threadia.backend.project.dto;

import com.threadia.backend.project.domain.ProjectMember;

public record ProjectMemberSummaryResponse(
		Long id,
		String role,
		String status) {

	public static ProjectMemberSummaryResponse from(ProjectMember projectMember) {
		return new ProjectMemberSummaryResponse(
				projectMember.getId(),
				projectMember.getRole().name(),
				projectMember.getStatus().name());
	}
}
