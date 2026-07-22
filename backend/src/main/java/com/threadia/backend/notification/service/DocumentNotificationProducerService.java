package com.threadia.backend.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.document.domain.Document;
import com.threadia.backend.document.repository.DocumentIssueLinkRepository;
import com.threadia.backend.notification.domain.NotificationEvent;
import com.threadia.backend.notification.domain.NotificationRecipient;
import com.threadia.backend.notification.domain.NotificationTargetType;
import com.threadia.backend.notification.domain.NotificationType;
import com.threadia.backend.notification.repository.NotificationEventRepository;
import com.threadia.backend.notification.repository.NotificationRecipientRepository;

@Service
public class DocumentNotificationProducerService {

	private final DocumentIssueLinkRepository linkRepository;
	private final NotificationEventRepository eventRepository;
	private final NotificationRecipientRepository recipientRepository;

	@Autowired
	public DocumentNotificationProducerService(
			DocumentIssueLinkRepository linkRepository,
			NotificationEventRepository eventRepository,
			NotificationRecipientRepository recipientRepository) {
		this.linkRepository = linkRepository;
		this.eventRepository = eventRepository;
		this.recipientRepository = recipientRepository;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void documentUpdated(Document document, User actor, LocalDateTime occurredAt) {
		NotificationEvent event = eventRepository.save(new NotificationEvent(
				null,
				document.getProject().getCompany(),
				document.getProject(),
				actor,
				NotificationType.DOCUMENT_UPDATED,
				NotificationTargetType.DOCUMENT,
				document.getId(),
				"문서가 업데이트되었습니다.",
				null,
				null,
				occurredAt));
		List<NotificationRecipient> recipients = linkRepository
				.findActiveNotificationRecipientsByDocumentIdExcludingUserId(document.getId(), actor.getId())
				.stream()
				.map(user -> new NotificationRecipient(event, user, null, occurredAt))
				.toList();
		recipientRepository.saveAll(recipients);
	}
}
