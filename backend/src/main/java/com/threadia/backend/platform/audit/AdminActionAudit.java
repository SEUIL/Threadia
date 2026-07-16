package com.threadia.backend.platform.audit;

import java.time.LocalDateTime;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.supportcase.domain.SupportCase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_action_audits")
public class AdminActionAudit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "support_case_id", nullable = true, foreignKey = @ForeignKey(name = "fk_aaa_case"))
	private SupportCase supportCase;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "actor_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_aaa_actor"))
	private User actor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_platform_admin_id", foreignKey = @ForeignKey(name = "fk_aaa_platform_admin"))
	private PlatformAdmin actorPlatformAdmin;

	@Enumerated(EnumType.STRING)
	@Column(name = "authority_source", nullable = false, length = 30)
	private AuditAuthoritySource authoritySource;

	@Enumerated(EnumType.STRING)
	@Column(name = "authority_role", length = 30)
	private PlatformAdminRole authorityRole;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AdminAction action;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 30)
	private AuditTargetType targetType;

	@Column(name = "target_id", nullable = false)
	private Long targetId;

	@Column(nullable = false, length = 500)
	private String reason;

	@Column(name = "before_state", columnDefinition = "LONGTEXT")
	private String beforeState;

	@Column(name = "after_state", nullable = false, columnDefinition = "LONGTEXT")
	private String afterState;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	protected AdminActionAudit() {
	}

	public AdminActionAudit(
			SupportCase supportCase,
			User actor,
			PlatformAdmin actorPlatformAdmin,
			AuditAuthoritySource authoritySource,
			PlatformAdminRole authorityRole,
			AdminAction action,
			AuditTargetType targetType,
			Long targetId,
			String reason,
			String beforeState,
			String afterState,
			LocalDateTime createdAt) {
		this.supportCase = supportCase;
		this.actor = actor;
		this.actorPlatformAdmin = actorPlatformAdmin;
		this.authoritySource = authoritySource;
		this.authorityRole = authorityRole;
		this.action = action;
		this.targetType = targetType;
		this.targetId = targetId;
		this.reason = reason;
		this.beforeState = beforeState;
		this.afterState = afterState;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public SupportCase getSupportCase() {
		return supportCase;
	}

	public User getActor() {
		return actor;
	}

	public PlatformAdmin getActorPlatformAdmin() {
		return actorPlatformAdmin;
	}

	public AuditAuthoritySource getAuthoritySource() {
		return authoritySource;
	}

	public PlatformAdminRole getAuthorityRole() {
		return authorityRole;
	}

	public AdminAction getAction() {
		return action;
	}

	public AuditTargetType getTargetType() {
		return targetType;
	}

	public Long getTargetId() {
		return targetId;
	}

	public String getReason() {
		return reason;
	}

	public String getBeforeState() {
		return beforeState;
	}

	public String getAfterState() {
		return afterState;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
