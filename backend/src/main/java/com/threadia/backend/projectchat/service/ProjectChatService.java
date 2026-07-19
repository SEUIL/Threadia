package com.threadia.backend.projectchat.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.exception.ProjectNotFoundException;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;
import com.threadia.backend.projectchat.domain.Message;
import com.threadia.backend.projectchat.domain.ProjectChatMemberState;
import com.threadia.backend.projectchat.domain.ProjectChatMemberStateId;
import com.threadia.backend.projectchat.dto.MarkReadData;
import com.threadia.backend.projectchat.dto.MarkReadRequest;
import com.threadia.backend.projectchat.dto.MessageCreateRequest;
import com.threadia.backend.projectchat.dto.MessageData;
import com.threadia.backend.projectchat.dto.MessageResponse;
import com.threadia.backend.projectchat.dto.MessagesData;
import com.threadia.backend.projectchat.dto.UnreadCountData;
import com.threadia.backend.projectchat.exception.MessageNotFoundException;
import com.threadia.backend.projectchat.exception.ProjectChatConflictException;
import com.threadia.backend.projectchat.exception.ProjectChatValidationException;
import com.threadia.backend.projectchat.repository.MessageRepository;
import com.threadia.backend.projectchat.repository.ProjectChatMemberStateRepository;

@Service
public class ProjectChatService {

	public static final int DEFAULT_PAGE_SIZE = 50;
	public static final int MAX_PAGE_SIZE = 100;

	private final AuthContextService authContextService;
	private final CompanyMembershipService companyMembershipService;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final MessageRepository messageRepository;
	private final ProjectChatMemberStateRepository stateRepository;
	private final ProjectChatCursorCodec cursorCodec;
	private final Clock clock;

	@Autowired
	public ProjectChatService(
			AuthContextService authContextService,
			CompanyMembershipService companyMembershipService,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			MessageRepository messageRepository,
			ProjectChatMemberStateRepository stateRepository,
			ProjectChatCursorCodec cursorCodec) {
		this(authContextService, companyMembershipService, projectRepository, projectMemberRepository,
				messageRepository, stateRepository, cursorCodec, Clock.systemUTC());
	}

	ProjectChatService(
			AuthContextService authContextService,
			CompanyMembershipService companyMembershipService,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			MessageRepository messageRepository,
			ProjectChatMemberStateRepository stateRepository,
			ProjectChatCursorCodec cursorCodec,
			Clock clock) {
		this.authContextService = authContextService;
		this.companyMembershipService = companyMembershipService;
		this.projectRepository = projectRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.messageRepository = messageRepository;
		this.stateRepository = stateRepository;
		this.cursorCodec = cursorCodec;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public MessagesData list(String accessToken, Long projectId, String cursor, Integer requestedLimit) {
		AuthContext authContext = activeAuthContext(accessToken);
		Project project = project(projectId);
		requireProjectAccess(project, authContext);
		int limit = requestedLimit == null ? DEFAULT_PAGE_SIZE : requestedLimit;
		if (limit < 1 || limit > MAX_PAGE_SIZE) {
			throw ProjectChatValidationException.invalidLimit();
		}
		ProjectChatCursorCodec.Cursor decoded = cursor == null ? null : cursorCodec.decode(cursor, projectId);
		List<Message> page = messageRepository.findPageByProjectId(projectId,
				decoded == null ? null : decoded.createdAt(), decoded == null ? null : decoded.id(),
				PageRequest.of(0, limit + 1));
		boolean hasNext = page.size() > limit;
		List<Message> messages = hasNext ? page.subList(0, limit) : page;
		String nextCursor = hasNext ? cursorCodec.encode(projectId, messages.get(messages.size() - 1)) : null;
		return new MessagesData(messages.stream().map(MessageResponse::from).toList(), nextCursor);
	}

	@Transactional
	public MessageData create(String accessToken, Long projectId, MessageCreateRequest request) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		User author = authContextService.requireActiveUser(authContext);
		Project project = project(projectId);
		requireProjectAccess(project, authContext);
		if (request.threadId() != null) {
			throw ProjectChatConflictException.threadDeferred();
		}
		Message message = messageRepository.save(new Message(project, author, request.content().trim(), now()));
		return new MessageData(MessageResponse.from(message));
	}

	@Transactional(readOnly = true)
	public MessageData detail(String accessToken, Long messageId) {
		AuthContext authContext = activeAuthContext(accessToken);
		Message message = message(messageId);
		requireProjectAccess(message.getProject(), authContext);
		return new MessageData(MessageResponse.from(message));
	}

	@Transactional
	public MarkReadData markRead(String accessToken, Long projectId, MarkReadRequest request) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		User user = authContextService.requireActiveUser(authContext);
		Project project = project(projectId);
		requireProjectAccess(project, authContext);
		Message requestedPointer = message(request.lastReadMessageId());
		if (!requestedPointer.getProject().getId().equals(projectId)) {
			throw new ForbiddenException("다른 프로젝트의 메시지는 읽음 위치로 지정할 수 없습니다.");
		}
		ProjectChatMemberStateId stateId = new ProjectChatMemberStateId(projectId, user.getId());
		stateRepository.insertIfAbsent(projectId, user.getId(), now());
		ProjectChatMemberState state = stateRepository.findByIdForUpdate(stateId)
				.orElseThrow(() -> new IllegalStateException("프로젝트 채팅 읽음 상태를 잠글 수 없습니다."));
		Message currentPointer = state.getLastReadMessage();
		if (currentPointer == null || isAfter(requestedPointer, currentPointer)) {
			state.moveReadPointerTo(requestedPointer, now());
			stateRepository.save(state);
		}
		return new MarkReadData(projectId, state.getLastReadMessage().getId(), unreadCount(projectId, user.getId(), state));
	}

	@Transactional(readOnly = true)
	public UnreadCountData unreadCount(String accessToken, Long projectId) {
		AuthContext authContext = activeAuthContext(accessToken);
		Project project = project(projectId);
		requireProjectAccess(project, authContext);
		ProjectChatMemberState state = stateRepository.findById(new ProjectChatMemberStateId(projectId, authContext.userId())).orElse(null);
		return new UnreadCountData(projectId, unreadCount(projectId, authContext.userId(), state));
	}

	private long unreadCount(Long projectId, Long userId, ProjectChatMemberState state) {
		if (state == null || state.getLastReadMessage() == null) {
			return messageRepository.countUnreadWithoutPointer(projectId, userId);
		}
		Message pointer = state.getLastReadMessage();
		return messageRepository.countUnreadAfterPointer(projectId, userId, pointer.getCreatedAt(), pointer.getId());
	}

	private void requireProjectAccess(Project project, AuthContext authContext) {
		Long companyId = project.getCompany().getId();
		if (companyMembershipService.hasActiveRole(companyId, authContext, CompanyMemberRole.COMPANY_ADMIN)) return;
		companyMembershipService.requireActiveMember(companyId, authContext);
		projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), authContext.userId())
				.filter(member -> member.getStatus() == ProjectMemberStatus.ACTIVE)
				.orElseThrow(() -> new ForbiddenException("프로젝트 접근 권한이 없습니다."));
	}

	private AuthContext activeAuthContext(String accessToken) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		authContextService.requireActiveUser(authContext);
		return authContext;
	}

	private Project project(Long projectId) {
		return projectRepository.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));
	}

	private Message message(Long messageId) {
		return messageRepository.findById(messageId).orElseThrow(() -> new MessageNotFoundException(messageId));
	}

	private LocalDateTime now() {
		return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime();
	}

	private static boolean isAfter(Message candidate, Message reference) {
		return candidate.getCreatedAt().isAfter(reference.getCreatedAt())
				|| (candidate.getCreatedAt().isEqual(reference.getCreatedAt()) && candidate.getId() > reference.getId());
	}

}
