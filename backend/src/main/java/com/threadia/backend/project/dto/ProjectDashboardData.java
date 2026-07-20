package com.threadia.backend.project.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record ProjectDashboardData(
		Progress progress,
		List<RecentIssue> recentIssues,
		List<RecentDocument> recentDocuments,
		SprintSummary sprintSummary) {

	public record Progress(long totalIssueCount, long doneIssueCount, int percentage) {
	}

	public record RecentIssue(Long id, String issueKey, String title, String status, String updatedAt) {
		public RecentIssue(Long id, String issueKey, String title, String status, LocalDateTime updatedAt) {
			this(id, issueKey, title, status, formatUtc(updatedAt));
		}
	}

	public record RecentDocument(Long id, String title, String type, String updatedAt) {
		public RecentDocument(Long id, String title, String type, LocalDateTime updatedAt) {
			this(id, title, type, formatUtc(updatedAt));
		}
	}

	public record SprintSummary(
			Long id,
			String name,
			String goal,
			LocalDate startDate,
			LocalDate endDate,
			long issueCount,
			long doneIssueCount) {
	}

	private static String formatUtc(LocalDateTime value) {
		return value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
