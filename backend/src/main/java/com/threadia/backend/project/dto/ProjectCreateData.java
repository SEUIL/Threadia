package com.threadia.backend.project.dto;

public record ProjectCreateData(
		ProjectResponse project,
		ProjectMemberSummaryResponse projectMember) {
}
