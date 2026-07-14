package com.threadia.backend.sprint.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.threadia.backend.project.domain.Project;
import jakarta.persistence.*;

@Entity
@Table(name = "sprints")
public class Sprint {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sprints_project"))
	private Project project;
	@Column(nullable = false, length = 150) private String name;
	@Column(columnDefinition = "TEXT") private String goal;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SprintStatus status;
	@Column(name = "start_date", columnDefinition = "DATE") private LocalDate startDate;
	@Column(name = "end_date", columnDefinition = "DATE") private LocalDate endDate;
	@Column(name = "started_at", columnDefinition = "DATETIME") private LocalDateTime startedAt;
	@Column(name = "completed_at", columnDefinition = "DATETIME") private LocalDateTime completedAt;
	@Column(columnDefinition = "TEXT") private String summary;
	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME") private LocalDateTime createdAt;
	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME") private LocalDateTime updatedAt;
	protected Sprint() {}
	public Sprint(Project project, String name, String goal, LocalDate startDate, LocalDate endDate, LocalDateTime now) {
		this.project = project; this.name = name; this.goal = goal; this.status = SprintStatus.PLANNED;
		this.startDate = startDate; this.endDate = endDate; this.createdAt = now; this.updatedAt = now;
	}
	public Long getId() { return id; } public Project getProject() { return project; } public String getName() { return name; }
	public String getGoal() { return goal; } public SprintStatus getStatus() { return status; } public LocalDate getStartDate() { return startDate; }
	public LocalDate getEndDate() { return endDate; } public LocalDateTime getStartedAt() { return startedAt; }
	public LocalDateTime getCompletedAt() { return completedAt; } public String getSummary() { return summary; }
	public LocalDateTime getCreatedAt() { return createdAt; } public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void update(String name, String goal, LocalDate startDate, LocalDate endDate, LocalDateTime now) { this.name=name; this.goal=goal; this.startDate=startDate; this.endDate=endDate; this.updatedAt=now; }
	public void start(LocalDateTime now) { this.status=SprintStatus.ACTIVE; this.startedAt=now; this.updatedAt=now; }
	public void complete(String summary, LocalDateTime now) { this.status=SprintStatus.COMPLETED; this.summary=summary; this.completedAt=now; this.updatedAt=now; }
}
