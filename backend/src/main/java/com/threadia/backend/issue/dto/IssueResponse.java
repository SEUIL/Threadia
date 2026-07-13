package com.threadia.backend.issue.dto;

import com.threadia.backend.issue.domain.Issue;

public record IssueResponse(Long id, String issueKey, String title, String status) {

	public static IssueResponse from(Issue issue) {
		return new IssueResponse(
				issue.getId(),
				issue.getIssueKey(),
				issue.getTitle(),
				issue.getStatus().name());
	}
}
