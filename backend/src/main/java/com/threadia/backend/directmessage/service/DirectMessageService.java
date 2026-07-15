package com.threadia.backend.directmessage.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.directmessage.domain.DirectMessage;
import com.threadia.backend.directmessage.domain.DirectMessageRoom;
import com.threadia.backend.directmessage.domain.DirectMessageRoomUserState;
import com.threadia.backend.directmessage.dto.DirectMessageData;
import com.threadia.backend.directmessage.dto.DirectMessageMarkReadRequest;
import com.threadia.backend.directmessage.dto.DirectMessageReadData;
import com.threadia.backend.directmessage.dto.DirectMessageResponse;
import com.threadia.backend.directmessage.dto.DirectMessageRoomCreateRequest;
import com.threadia.backend.directmessage.dto.DirectMessageRoomCreationResult;
import com.threadia.backend.directmessage.dto.DirectMessageRoomData;
import com.threadia.backend.directmessage.dto.DirectMessageRoomResponse;
import com.threadia.backend.directmessage.dto.DirectMessageRoomsData;
import com.threadia.backend.directmessage.dto.DirectMessageSendRequest;
import com.threadia.backend.directmessage.dto.DirectMessageUnreadCountData;
import com.threadia.backend.directmessage.dto.DirectMessagesData;
import com.threadia.backend.directmessage.exception.DirectMessageRoomNotFoundException;
import com.threadia.backend.directmessage.exception.DirectMessageValidationException;
import com.threadia.backend.directmessage.repository.DirectMessageRepository;
import com.threadia.backend.directmessage.repository.DirectMessageRoomRepository;
import com.threadia.backend.directmessage.repository.DirectMessageRoomUserStateRepository;

@Service
public class DirectMessageService {
	private final AuthContextService authContextService;
	private final CompanyMembershipService companyMembershipService;
	private final UserRepository userRepository;
	private final DirectMessageRoomRepository roomRepository;
	private final DirectMessageRepository messageRepository;
	private final DirectMessageRoomUserStateRepository stateRepository;
	private final Clock clock;

	@Autowired
	public DirectMessageService(AuthContextService authContextService, CompanyMembershipService companyMembershipService, UserRepository userRepository, DirectMessageRoomRepository roomRepository, DirectMessageRepository messageRepository, DirectMessageRoomUserStateRepository stateRepository) {
		this(authContextService, companyMembershipService, userRepository, roomRepository, messageRepository, stateRepository, Clock.systemUTC());
	}

	DirectMessageService(AuthContextService authContextService, CompanyMembershipService companyMembershipService, UserRepository userRepository, DirectMessageRoomRepository roomRepository, DirectMessageRepository messageRepository, DirectMessageRoomUserStateRepository stateRepository, Clock clock) {
		this.authContextService = authContextService; this.companyMembershipService = companyMembershipService; this.userRepository = userRepository;
		this.roomRepository = roomRepository; this.messageRepository = messageRepository; this.stateRepository = stateRepository; this.clock = clock;
	}

	@Transactional(readOnly = true)
	public DirectMessageRoomsData listRooms(String accessToken, Long companyId) {
		AuthContext auth = activeAuth(accessToken);
		requireActiveMembership(companyId, auth.userId());
		List<DirectMessageRoomResponse> rooms = roomRepository.findByCompanyIdAndParticipant(companyId, auth.userId()).stream()
				.filter(this::hasActiveParticipants)
				.map(room -> DirectMessageRoomResponse.from(room, auth.userId(), messageRepository.countUnreadByRoomIdAndUserId(room.getId(), auth.userId()))).toList();
		return new DirectMessageRoomsData(rooms);
	}

	@Transactional
	public DirectMessageRoomCreationResult getOrCreateRoom(String accessToken, Long companyId, DirectMessageRoomCreateRequest request) {
		AuthContext auth = activeAuth(accessToken);
		if (auth.userId().equals(request.otherUserId())) throw new DirectMessageValidationException("자기 자신과 DM 방을 만들 수 없습니다.");
		requireActiveMembership(companyId, auth.userId());
		User otherUser = userRepository.findById(request.otherUserId()).orElseThrow(() -> new DirectMessageValidationException("같은 회사의 활성 사용자를 선택해야 합니다."));
		if (otherUser.getStatus() != UserStatus.ACTIVE) throw new DirectMessageValidationException("같은 회사의 활성 사용자를 선택해야 합니다.");
		requireActiveMembership(companyId, otherUser.getId());
		long user1Id = Math.min(auth.userId(), otherUser.getId());
		long user2Id = Math.max(auth.userId(), otherUser.getId());
		DirectMessageRoom existing = roomRepository.findByCompanyIdAndUserPair(companyId, user1Id, user2Id).orElse(null);
		RoomLookup roomLookup = existing == null ? createAndReread(companyId, user1Id, user2Id) : new RoomLookup(existing, false);
		boolean created = roomLookup.created();
		DirectMessageRoom room = roomLookup.room();
		ensureState(room.getId(), auth.userId());
		ensureState(room.getId(), otherUser.getId());
		return new DirectMessageRoomCreationResult(new DirectMessageRoomData(DirectMessageRoomResponse.from(room, auth.userId(), messageRepository.countUnreadByRoomIdAndUserId(room.getId(), auth.userId()))), created);
	}

	@Transactional(readOnly = true)
	public DirectMessagesData listMessages(String accessToken, Long roomId) {
		AuthContext auth = activeAuth(accessToken);
		DirectMessageRoom room = participantRoom(roomId, auth.userId());
		return new DirectMessagesData(messageRepository.findByRoomIdOrderByCreatedAtAscIdAsc(room.getId()).stream().map(DirectMessageResponse::from).toList());
	}

	@Transactional
	public DirectMessageData send(String accessToken, Long roomId, DirectMessageSendRequest request) {
		AuthContext auth = activeAuth(accessToken);
		DirectMessageRoom room = participantRoom(roomId, auth.userId());
		User sender = authContextService.requireActiveUser(auth);
		DirectMessage message = messageRepository.save(new DirectMessage(room, sender, request.content(), now()));
		return new DirectMessageData(DirectMessageResponse.from(message));
	}

	@Transactional
	public DirectMessageReadData markRead(String accessToken, Long roomId, DirectMessageMarkReadRequest request) {
		AuthContext auth = activeAuth(accessToken);
		DirectMessageRoom room = participantRoom(roomId, auth.userId());
		DirectMessage message = messageRepository.findById(request.lastReadDirectMessageId())
				.filter(candidate -> candidate.getRoom().getId().equals(room.getId()))
				.orElseThrow(() -> new DirectMessageValidationException("해당 DM 방의 메시지만 읽음 위치로 지정할 수 있습니다."));
		ensureState(room.getId(), auth.userId());
		DirectMessageRoomUserState state = stateRepository.findByRoomIdAndUserIdForUpdate(room.getId(), auth.userId()).orElseThrow();
		state.advanceTo(message, now());
		Long pointer = state.getLastReadDirectMessage() == null ? null : state.getLastReadDirectMessage().getId();
		return new DirectMessageReadData(room.getId(), pointer, messageRepository.countUnreadByRoomIdAndUserId(room.getId(), auth.userId()), state.getLastReadAt());
	}

	@Transactional(readOnly = true)
	public DirectMessageUnreadCountData unreadCount(String accessToken) {
		AuthContext auth = activeAuth(accessToken);
		long unreadCount = roomRepository.findByParticipant(auth.userId()).stream()
				.filter(this::hasActiveParticipants)
				.mapToLong(room -> messageRepository.countUnreadByRoomIdAndUserId(room.getId(), auth.userId()))
				.sum();
		return new DirectMessageUnreadCountData(unreadCount);
	}

	private RoomLookup createAndReread(Long companyId, long user1Id, long user2Id) {
		boolean created = roomRepository.insertIgnore(companyId, user1Id, user2Id, now()) == 1;
		DirectMessageRoom room = roomRepository.findByCompanyIdAndUserPairForUpdate(companyId, user1Id, user2Id).orElseThrow();
		return new RoomLookup(room, created);
	}

	private DirectMessageRoom participantRoom(Long roomId, Long userId) {
		DirectMessageRoom room = roomRepository.findByIdWithParticipants(roomId).orElseThrow(() -> new DirectMessageRoomNotFoundException(roomId));
		if (!room.includes(userId) || !hasActiveParticipants(room)) throw new DirectMessageRoomNotFoundException(roomId);
		return room;
	}
	private boolean hasActiveParticipants(DirectMessageRoom room) {
		Long companyId = room.getCompany().getId();
		return room.getUser1().getStatus() == UserStatus.ACTIVE
				&& room.getUser2().getStatus() == UserStatus.ACTIVE
				&& companyMembershipService.isActiveMember(companyId, room.getUser1().getId())
				&& companyMembershipService.isActiveMember(companyId, room.getUser2().getId());
	}

	private void ensureState(Long roomId, Long userId) { stateRepository.insertIgnore(roomId, userId, now()); }
	private void requireActiveMembership(Long companyId, Long userId) {
		if (!companyMembershipService.isActiveMember(companyId, userId)) throw new DirectMessageValidationException("같은 회사의 활성 구성원만 DM을 사용할 수 있습니다.");
	}
	private AuthContext activeAuth(String accessToken) { AuthContext auth = authContextService.fromAccessToken(accessToken); authContextService.requireActiveUser(auth); return auth; }
	private LocalDateTime now() { return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime(); }
	private record RoomLookup(DirectMessageRoom room, boolean created) { }
}
