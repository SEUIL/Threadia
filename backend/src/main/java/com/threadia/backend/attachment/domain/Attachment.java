package com.threadia.backend.attachment.domain;

import java.time.LocalDateTime;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.company.domain.Company;

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
@Table(name = "attachments")
public class Attachment {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id", foreignKey = @ForeignKey(name = "fk_attachments_company")) private Company company;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attachments_created_by")) private User createdBy;
	@Enumerated(EnumType.STRING) @Column(name = "target_type", nullable = false, length = 30) private AttachmentTargetType targetType;
	@Column(name = "target_id", nullable = false) private Long targetId;
	@Column(name = "object_key", nullable = false, unique = true, length = 500) private String objectKey;
	@Column(name = "original_filename", nullable = false, length = 255) private String originalFilename;
	@Column(name = "content_type", nullable = false, length = 255) private String contentType;
	@Column(name = "content_length", nullable = false) private long contentLength;
	@Column(name = "checksum_sha256", nullable = false, length = 88) private String checksumSha256;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AttachmentStatus status;
	@Column(name = "completed_at", columnDefinition = "DATETIME") private LocalDateTime completedAt;
	@Column(name = "deleted_at", columnDefinition = "DATETIME") private LocalDateTime deletedAt;
	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME") private LocalDateTime createdAt;

	protected Attachment() { }
	public Attachment(Company company, User createdBy, AttachmentTargetType targetType, Long targetId, String objectKey, String originalFilename, String contentType, long contentLength, String checksumSha256, LocalDateTime createdAt) {
		this.company = company; this.createdBy = createdBy; this.targetType = targetType; this.targetId = targetId; this.objectKey = objectKey;
		this.originalFilename = originalFilename; this.contentType = contentType; this.contentLength = contentLength; this.checksumSha256 = checksumSha256;
		this.status = AttachmentStatus.UPLOADING; this.createdAt = createdAt;
	}
	public Long getId() { return id; }
	public Company getCompany() { return company; }
	public User getCreatedBy() { return createdBy; }
	public AttachmentTargetType getTargetType() { return targetType; }
	public Long getTargetId() { return targetId; }
	public String getObjectKey() { return objectKey; }
	public String getOriginalFilename() { return originalFilename; }
	public String getContentType() { return contentType; }
	public long getContentLength() { return contentLength; }
	public String getChecksumSha256() { return checksumSha256; }
	public AttachmentStatus getStatus() { return status; }
	public LocalDateTime getCompletedAt() { return completedAt; }
	public LocalDateTime getDeletedAt() { return deletedAt; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void complete(LocalDateTime completedAt) { this.status = AttachmentStatus.READY; this.completedAt = completedAt; }
	public void delete(LocalDateTime deletedAt) { this.status = AttachmentStatus.DELETED; this.deletedAt = deletedAt; }
}
