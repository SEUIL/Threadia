package com.threadia.backend.sprint.dto;
import java.time.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.threadia.backend.sprint.domain.Sprint;
public record SprintResponse(Long id, Long projectId, String name, String goal, String status, LocalDate startDate, LocalDate endDate, @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC") LocalDateTime startedAt, @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC") LocalDateTime completedAt, String summary) {
	public static SprintResponse from(Sprint sprint) { return new SprintResponse(sprint.getId(), sprint.getProject().getId(), sprint.getName(), sprint.getGoal(), sprint.getStatus().name(), sprint.getStartDate(), sprint.getEndDate(), sprint.getStartedAt(), sprint.getCompletedAt(), sprint.getSummary()); }
}
