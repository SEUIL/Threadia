package com.threadia.backend.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.notification.domain.NotificationRecipient;
import com.threadia.backend.notification.domain.NotificationType;
import com.threadia.backend.notification.dto.NotificationPageData;
import com.threadia.backend.notification.dto.NotificationReadAllData;
import com.threadia.backend.notification.dto.NotificationReadAllRequest;
import com.threadia.backend.notification.dto.NotificationReadData;
import com.threadia.backend.notification.dto.NotificationResponse;
import com.threadia.backend.notification.dto.NotificationUnreadCountData;
import com.threadia.backend.notification.exception.NotificationRecipientNotFoundException;
import com.threadia.backend.notification.exception.NotificationValidationException;
import com.threadia.backend.notification.repository.NotificationRecipientRepository;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.exception.ProjectNotFoundException;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@Service
public class NotificationService {

	public static final int DEFAULT_PAGE_SIZE = 20;
	public static final int MAX_PAGE_SIZE = 100;

	private final AuthContextService authContextService;
	private final CompanyMembershipService companyMembershipService;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final NotificationRecipientRepository recipientRepository;
	private final NotificationCursorCodec cursorCodec;
	private final Clock clock;

	@Autowired
	public NotificationService(
			AuthContextService authContextService,
			CompanyMembershipService companyMembershipService,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			NotificationRecipientRepository recipientRepository,
			NotificationCursorCodec cursorCodec) {
		this(authContextService, companyMembershipService, projectRepository, projectMemberRepository,
				recipientRepository, cursorCodec, Clock.systemUTC());
	}

	NotificationService(
			AuthContextService authContextService,
			CompanyMembershipService companyMembershipService,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			NotificationRecipientRepository recipientRepository,
			NotificationCursorCodec cursorCodec,
			Clock clock) {
		this.authContextService = authContextService;
		this.companyMembershipService = companyMembershipService;
		this.projectRepository = projectRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.recipientRepository = recipientRepository;
		this.cursorCodec = cursorCodec;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public NotificationPageData list(
			String accessToken,
			Long companyId,
			Long projectId,
			String type,
			Boolean unreadOnly,
			String cursor,
			Integer requestedSize) {
		AuthContext authContext = activeAuthContext(accessToken);
		NotificationFilters filters = filters(companyId, projectId, type, Boolean.TRUE.equals(unreadOnly));
		requireScopeAccess(filters, authContext);
		int size = pageSize(requestedSize);
		NotificationCursorCodec.Cursor decoded = cursor == null ? null : cursorCodec.decode(cursor, authContext.userId(), filters);
		List<NotificationRecipient> page = recipientRepository.findPage(
				authContext.userId(),
				filters.companyId(),
				filters.projectId(),
				filters.type(),
				filters.unreadOnly(),
				decoded == null ? null : decoded.createdAt(),
				decoded == null ? null : decoded.id(),
				PageRequest.of(0, size + 1));
		boolean hasNext = page.size() > size;
		List<NotificationRecipient> recipients = hasNext ? page.subList(0, size) : page;
		String nextCursor = hasNext
				? cursorCodec.encode(authContext.userId(), filters, recipients.get(recipients.size() - 1))
				: null;
		return new NotificationPageData(
				recipients.stream().map(NotificationResponse::from).toList(),
				nextCursor,
				hasNext,
				size);
	}

	@Transactional(readOnly = true)
	public NotificationUnreadCountData unreadCount(
			String accessToken,
			Long companyId,
			Long projectId,
			String type) {
		AuthContext authContext = activeAuthContext(accessToken);
		NotificationFilters filters = filters(companyId, projectId, type, true);
		requireScopeAccess(filters, authContext);
		return new NotificationUnreadCountData(recipientRepository.countUnread(
				authContext.userId(), filters.companyId(), filters.projectId(), filters.type()));
	}

	@Transactional
	public NotificationReadData markRead(String accessToken, Long notificationRecipientId) {
		AuthContext authContext = activeAuthContext(accessToken);
		NotificationRecipient recipient = recipientRepository
				.findOwnedByIdForUpdate(notificationRecipientId, authContext.userId())
				.orElseGet(() -> rejectMissingOrForeignRecipient(notificationRecipientId));
		recipient.markRead(now());
		return new NotificationReadData(recipient.getId(), NotificationResponse.formatUtc(recipient.getReadAt()));
	}

	@Transactional
	public NotificationReadAllData markAllRead(String accessToken, NotificationReadAllRequest request) {
		AuthContext authContext = activeAuthContext(accessToken);
		NotificationFilters filters = filters(request.companyId(), request.projectId(), request.type(), true);
		requireScopeAccess(filters, authContext);
		int updatedCount = recipientRepository.markAllRead(
				authContext.userId(),
				filters.companyId(),
				filters.projectId(),
				filters.type() == null ? null : filters.type().name(),
				now());
		return new NotificationReadAllData(updatedCount);
	}

	private NotificationRecipient rejectMissingOrForeignRecipient(Long notificationRecipientId) {
		if (recipientRepository.existsById(notificationRecipientId)) {
			throw new ForbiddenException("본인 알림만 읽음 처리할 수 있습니다.");
		}
		throw new NotificationRecipientNotFoundException(notificationRecipientId);
	}

	private void requireScopeAccess(NotificationFilters filters, AuthContext authContext) {
		if (filters.projectId() != null) {
			Project project = projectRepository.findById(filters.projectId())
					.orElseThrow(() -> new ProjectNotFoundException(filters.projectId()));
			Long projectCompanyId = project.getCompany().getId();
			if (filters.companyId() != null && !filters.companyId().equals(projectCompanyId)) {
				throw NotificationValidationException.inconsistentCompanyAndProject();
			}
			if (companyMembershipService.hasActiveRole(
					projectCompanyId, authContext, CompanyMemberRole.COMPANY_ADMIN)) {
				return;
			}
			companyMembershipService.requireActiveMember(projectCompanyId, authContext);
			if (!projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(
					filters.projectId(), authContext.userId(), ProjectMemberStatus.ACTIVE)) {
				throw new ForbiddenException("해당 범위의 알림을 조회할 수 없습니다.");
			}
			return;
		}
		if (filters.companyId() != null) {
			companyMembershipService.requireActiveMember(filters.companyId(), authContext);
		}
	}

	private AuthContext activeAuthContext(String accessToken) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		authContextService.requireActiveUser(authContext);
		return authContext;
	}

	private static NotificationFilters filters(
			Long companyId,
			Long projectId,
			String type,
			boolean unreadOnly) {
		return new NotificationFilters(companyId, projectId, parseType(type), unreadOnly);
	}

	private static NotificationType parseType(String value) {
		if (value == null) {
			return null;
		}
		try {
			return NotificationType.valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw NotificationValidationException.invalidType();
		}
	}

	private static int pageSize(Integer requestedSize) {
		int size = requestedSize == null ? DEFAULT_PAGE_SIZE : requestedSize;
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw NotificationValidationException.invalidSize();
		}
		return size;
	}

	private LocalDateTime now() {
		return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime();
	}
}
