package com.threadia.backend.projectchat.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ProjectChatMemberStateId implements Serializable {

	@Column(name = "project_id")
	private Long projectId;

	@Column(name = "user_id")
	private Long userId;

	protected ProjectChatMemberStateId() {
	}

	public ProjectChatMemberStateId(Long projectId, Long userId) {
		this.projectId = projectId;
		this.userId = userId;
	}

	public Long getProjectId() { return projectId; }
	public Long getUserId() { return userId; }

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof ProjectChatMemberStateId that)) return false;
		return Objects.equals(projectId, that.projectId) && Objects.equals(userId, that.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(projectId, userId);
	}
}
