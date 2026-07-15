package com.threadia.backend.directmessage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.directmessage.domain.DirectMessage;
import com.threadia.backend.directmessage.domain.DirectMessageRoom;
import com.threadia.backend.directmessage.domain.DirectMessageRoomUserState;
import com.threadia.backend.directmessage.dto.DirectMessageMarkReadRequest;
import com.threadia.backend.directmessage.dto.DirectMessageRoomCreateRequest;
import com.threadia.backend.directmessage.exception.DirectMessageRoomNotFoundException;
import com.threadia.backend.directmessage.exception.DirectMessageValidationException;
import com.threadia.backend.directmessage.repository.DirectMessageRepository;
import com.threadia.backend.directmessage.repository.DirectMessageRoomRepository;
import com.threadia.backend.directmessage.repository.DirectMessageRoomUserStateRepository;

@ExtendWith(MockitoExtension.class)
class DirectMessageServiceTest {
	@Mock AuthContextService authContextService; @Mock CompanyMembershipService membershipService; @Mock UserRepository userRepository;
	@Mock CompanyRepository companyRepository;
	@Mock DirectMessageRoomRepository roomRepository; @Mock DirectMessageRepository messageRepository; @Mock DirectMessageRoomUserStateRepository stateRepository;
	private DirectMessageService service;
	private final AuthContext auth = new AuthContext(1L, "me@example.com");
	@BeforeEach void setUp() {
		service = new DirectMessageService(authContextService, membershipService, companyRepository, userRepository, roomRepository, messageRepository, stateRepository, Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneOffset.UTC));
		lenient().when(authContextService.fromAccessToken("token")).thenReturn(auth); lenient().when(authContextService.requireActiveUser(auth)).thenReturn(user(1L));
	}
	@Test void rejectsSelfDirectMessageBeforeRoomLookup() {
		assertThatThrownBy(() -> service.getOrCreateRoom("token", 10L, new DirectMessageRoomCreateRequest(1L))).isInstanceOf(DirectMessageValidationException.class);
		verify(roomRepository, never()).findByCompanyIdAndUserPair(any(), any(), any());
	}
	@Test void rejectsMissingOrInactiveMembershipForCurrentAndOtherUser() {
		when(membershipService.isActiveMember(10L, 1L)).thenReturn(false);
		assertThatThrownBy(() -> service.getOrCreateRoom("token", 10L, new DirectMessageRoomCreateRequest(2L))).isInstanceOf(DirectMessageValidationException.class);
		verify(userRepository, never()).findById(2L);
		when(membershipService.isActiveMember(10L, 1L)).thenReturn(true); when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L))); when(membershipService.isActiveMember(10L, 2L)).thenReturn(false);
		assertThatThrownBy(() -> service.getOrCreateRoom("token", 10L, new DirectMessageRoomCreateRequest(2L))).isInstanceOf(DirectMessageValidationException.class);
	}
	@Test void rejectsInactiveOtherUserBeforeCreatingRoom() {
		User inactiveUser = user(2L); inactiveUser.setStatus(UserStatus.SUSPENDED);
		when(membershipService.isActiveMember(10L, 1L)).thenReturn(true); when(userRepository.findById(2L)).thenReturn(Optional.of(inactiveUser));
		assertThatThrownBy(() -> service.getOrCreateRoom("token", 10L, new DirectMessageRoomCreateRequest(2L))).isInstanceOf(DirectMessageValidationException.class);
		verify(roomRepository, never()).findByCompanyIdAndUserPair(any(), any(), any());
	}
	@Test void reReadsTheRoomWithALockAfterLosingConcurrentCreation() {
		DirectMessageRoom room = room(30L, 10L, 1L, 2L);
		when(membershipService.isActiveMember(10L, 1L)).thenReturn(true); when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L))); when(membershipService.isActiveMember(10L, 2L)).thenReturn(true); when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(room.getCompany())); when(roomRepository.findByCompanyIdAndUserPair(10L, 1L, 2L)).thenReturn(Optional.empty()); when(roomRepository.insertIgnore(any(), any(), any(), any())).thenReturn(0); when(roomRepository.findByCompanyIdAndUserPairForUpdate(10L, 1L, 2L)).thenReturn(Optional.of(room));
		assertThat(service.getOrCreateRoom("token", 10L, new DirectMessageRoomCreateRequest(2L)).data().room().id()).isEqualTo(30L);
		org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(authContextService, membershipService, userRepository, companyRepository, roomRepository);
		inOrder.verify(authContextService).fromAccessToken("token");
		inOrder.verify(authContextService).requireActiveUser(auth);
		inOrder.verify(membershipService).isActiveMember(10L, 1L);
		inOrder.verify(userRepository).findById(2L);
		inOrder.verify(membershipService).isActiveMember(10L, 2L);
		inOrder.verify(companyRepository).findByIdForUpdate(10L);
		inOrder.verify(roomRepository).findByCompanyIdAndUserPair(10L, 1L, 2L);
		inOrder.verify(roomRepository).insertIgnore(any(), any(), any(), any());
		verify(roomRepository).findByCompanyIdAndUserPairForUpdate(10L, 1L, 2L);
	}
	@Test void normalizesOppositeDirectionCallerToTheSameRoom() {
		AuthContext otherAuth = new AuthContext(2L, "you@example.com");
		DirectMessageRoom room = room(30L, 10L, 1L, 2L);
		when(authContextService.fromAccessToken("token-2")).thenReturn(otherAuth); when(authContextService.requireActiveUser(otherAuth)).thenReturn(user(2L));
		when(membershipService.isActiveMember(10L, 2L)).thenReturn(true); when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L))); when(membershipService.isActiveMember(10L, 1L)).thenReturn(true); when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(room.getCompany())); when(roomRepository.findByCompanyIdAndUserPair(10L, 1L, 2L)).thenReturn(Optional.of(room));
		assertThat(service.getOrCreateRoom("token-2", 10L, new DirectMessageRoomCreateRequest(1L)).data().room().id()).isEqualTo(30L);
		verify(roomRepository).findByCompanyIdAndUserPair(10L, 1L, 2L);
	}
	@Test void hidesNonparticipantAndWrongCompanyRoomAsNotFound() {
		DirectMessageRoom room = room(30L, 20L, 2L, 3L); when(roomRepository.findByIdWithParticipants(30L)).thenReturn(Optional.of(room));
		assertThatThrownBy(() -> service.listMessages("token", 30L)).isInstanceOf(DirectMessageRoomNotFoundException.class);
		DirectMessageRoom participantRoom = room(31L, 20L, 1L, 2L); when(roomRepository.findByIdWithParticipants(31L)).thenReturn(Optional.of(participantRoom)); when(membershipService.isActiveMember(20L, 1L)).thenReturn(false);
		assertThatThrownBy(() -> service.listMessages("token", 31L)).isInstanceOf(DirectMessageRoomNotFoundException.class);
	}
	@Test void hidesRoomWhenEitherParticipantIsNoLongerActive() {
		DirectMessageRoom room = room(30L, 10L, 1L, 2L);
		when(roomRepository.findByIdWithParticipants(30L)).thenReturn(Optional.of(room));
		when(membershipService.isActiveMember(10L, 1L)).thenReturn(true); when(membershipService.isActiveMember(10L, 2L)).thenReturn(false);
		assertThatThrownBy(() -> service.listMessages("token", 30L)).isInstanceOf(DirectMessageRoomNotFoundException.class);
	}
	@Test void rejectsReadPointerForMessageInAnotherRoom() {
		DirectMessageRoom room = room(30L, 10L, 1L, 2L); DirectMessageRoom otherRoom = room(31L, 10L, 1L, 3L); DirectMessage otherMessage = new DirectMessage(otherRoom, user(3L), "other", java.time.LocalDateTime.now()); setId(otherMessage, 40L);
		when(roomRepository.findByIdWithParticipants(30L)).thenReturn(Optional.of(room)); when(membershipService.isActiveMember(10L, 1L)).thenReturn(true); when(membershipService.isActiveMember(10L, 2L)).thenReturn(true); when(messageRepository.findById(40L)).thenReturn(Optional.of(otherMessage));
		assertThatThrownBy(() -> service.markRead("token", 30L, new DirectMessageMarkReadRequest(40L))).isInstanceOf(DirectMessageValidationException.class);
	}
	@Test void markReadStoresServerUtcAndNeverRegressesThePointer() {
		DirectMessageRoom room = room(30L, 10L, 1L, 2L); DirectMessage newer = new DirectMessage(room, user(2L), "newer", java.time.LocalDateTime.now()); setId(newer, 50L); DirectMessage older = new DirectMessage(room, user(2L), "older", java.time.LocalDateTime.now()); setId(older, 40L);
		DirectMessageRoomUserState state = new DirectMessageRoomUserState(room, user(1L), java.time.LocalDateTime.parse("2026-07-14T00:00:00")); state.advanceTo(newer, java.time.LocalDateTime.parse("2026-07-14T00:00:00"));
		when(roomRepository.findByIdWithParticipants(30L)).thenReturn(Optional.of(room)); when(membershipService.isActiveMember(10L, 1L)).thenReturn(true); when(membershipService.isActiveMember(10L, 2L)).thenReturn(true); when(messageRepository.findById(40L)).thenReturn(Optional.of(older)); when(stateRepository.findByRoomIdAndUserIdForUpdate(30L, 1L)).thenReturn(Optional.of(state)); when(messageRepository.countUnreadByRoomIdAndUserId(30L, 1L)).thenReturn(0L);
		var result = service.markRead("token", 30L, new DirectMessageMarkReadRequest(40L));
		assertThat(result.lastReadDirectMessageId()).isEqualTo(50L); assertThat(result.readAt()).isEqualTo(java.time.LocalDateTime.parse("2026-07-14T00:00:00"));
	}
	@Test void unreadCountExcludesRoomsWhoseParticipantsAreNotBothActive() {
		DirectMessageRoom activeRoom = room(30L, 10L, 1L, 2L); DirectMessageRoom inactiveRoom = room(31L, 10L, 1L, 3L);
		when(roomRepository.findByParticipant(1L)).thenReturn(List.of(activeRoom, inactiveRoom)); when(membershipService.isActiveMember(10L, 1L)).thenReturn(true); when(membershipService.isActiveMember(10L, 2L)).thenReturn(true); when(membershipService.isActiveMember(10L, 3L)).thenReturn(false); when(messageRepository.countUnreadByRoomIdAndUserId(30L, 1L)).thenReturn(2L);
		assertThat(service.unreadCount("token").unreadCount()).isEqualTo(2L);
		verify(messageRepository, never()).countUnreadByRoomIdAndUserId(31L, 1L);
	}
	private static User user(Long id) { User user = new User("user" + id + "@example.com", "hash", "user", null, UserStatus.ACTIVE, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()); setId(user, id); return user; }
	private static DirectMessageRoom room(Long id, Long companyId, Long firstUserId, Long secondUserId) { User first = user(firstUserId); Company company = new Company("company", null, null, null, null, "ACTIVE", first, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()); setId(company, companyId); DirectMessageRoom room = new DirectMessageRoom(company, first, user(secondUserId), java.time.LocalDateTime.now()); setId(room, id); return room; }
	private static void setId(Object target, Long id) { try { Field field = target.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(target, id); } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); } }
}
