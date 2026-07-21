package com.threadia.backend.issue.dto;

import java.time.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.threadia.backend.issue.domain.Issue;

/**
 * `projectId`는 이슈 단독 참조 화면에서 프로젝트 route를 복원하기 위한 필드다.
 * 알림/검색처럼 issueId만 가진 컨텍스트는 이 값이 없으면 이슈가 속한 프로젝트로
 * 이동할 수 없다.
 *
 * `reporterUserId`는 표시용이 아니라 **권한 판정용**이다. trash/restore는 P/C 또는
 * 등록자에게 허용되는데(IssueService.trashOrRestore), 이 필드가 없으면 클라이언트가
 * "등록자" 경로를 판정할 수 없어 일반 멤버가 자기 이슈를 휴지통에 넣는 UI를 만들 수 없다.
 * reporter는 immutable이다(API-명세서 §7).
 */
public record IssueResponse(Long id, Long projectId, String issueKey, String title, String status, String description, String priority, Long assigneeUserId, Long reporterUserId, Long sprintId, LocalDate dueDate, String sourceType, Long sourceMessageId, @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC") LocalDateTime trashedAt, @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC") LocalDateTime restoredAt) {
	public IssueResponse(Long id, String issueKey, String title, String status) { this(id, null, issueKey, title, status, null, "MEDIUM", null, null, null, null, "MANUAL", null, null, null); }
	public IssueResponse(Long id, Long projectId, String issueKey, String title, String status, String description, String priority, Long assigneeUserId, Long reporterUserId, Long sprintId, LocalDate dueDate, String sourceType, LocalDateTime trashedAt, LocalDateTime restoredAt) { this(id, projectId, issueKey, title, status, description, priority, assigneeUserId, reporterUserId, sprintId, dueDate, sourceType, null, trashedAt, restoredAt); }

	public static IssueResponse from(Issue issue) {
		return new IssueResponse(
				issue.getId(),
				issue.getProject().getId(),
				issue.getIssueKey(),
				issue.getTitle(),
			issue.getStatus().name(), issue.getDescription(), issue.getPriority().name(),
			issue.getAssignee() == null ? null : issue.getAssignee().getId(),
			issue.getReporter() == null ? null : issue.getReporter().getId(),
			issue.getSprint() == null ? null : issue.getSprint().getId(), issue.getDueDate(), issue.getSourceType().name(), issue.getSourceMessage() == null ? null : issue.getSourceMessage().getId(), issue.getTrashedAt(), issue.getRestoredAt());
	}
}
