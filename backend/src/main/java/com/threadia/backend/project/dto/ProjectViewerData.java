package com.threadia.backend.project.dto;

import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.project.domain.ProjectMemberRole;

public record ProjectViewerData(String companyRole, String projectRole, boolean canManageProject) {

	public static ProjectViewerData from(CompanyMemberRole companyRole, ProjectMemberRole projectRole) {
		boolean canManageProject = companyRole == CompanyMemberRole.COMPANY_ADMIN
				|| projectRole == ProjectMemberRole.PROJECT_ADMIN;
		return new ProjectViewerData(roleName(companyRole), roleName(projectRole), canManageProject);
	}

	private static String roleName(Enum<?> role) {
		return role == null ? null : role.name();
	}
}
