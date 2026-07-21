package com.threadia.backend.attachment;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "threadia.storage.orphan-cleanup.enabled", havingValue = "true")
public class AttachmentOrphanCleanupJob {
	private final AttachmentService attachmentService;
	private final Duration uploadCleanupDelay;
	public AttachmentOrphanCleanupJob(AttachmentService attachmentService, @Value("${threadia.storage.upload-cleanup-delay:PT24H}") Duration uploadCleanupDelay) {
		this.attachmentService = attachmentService;
		this.uploadCleanupDelay = uploadCleanupDelay;
	}

	@Scheduled(fixedDelayString = "${threadia.storage.orphan-cleanup-delay:PT1H}")
	public void purgeLogicallyDeletedAttachments() {
		attachmentService.expireStaleUploads(uploadCleanupDelay);
		attachmentService.purgeLogicallyDeleted();
	}
}
