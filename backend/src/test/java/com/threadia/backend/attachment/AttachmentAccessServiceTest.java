package com.threadia.backend.attachment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.threadia.backend.attachment.domain.AttachmentTargetType;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.directmessage.domain.DirectMessage;
import com.threadia.backend.directmessage.domain.DirectMessageRoom;
import com.threadia.backend.document.domain.Document;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.repository.ProjectMemberRepository;

import jakarta.persistence.EntityManager;

class AttachmentAccessServiceTest {
	private final EntityManager entityManager = mock(EntityManager.class);
	private final CompanyMembershipService companyMembership = mock(CompanyMembershipService.class);
	private final ProjectMemberRepository projectMembers = mock(ProjectMemberRepository.class);
	private final AuthContext actor = mock(AuthContext.class);
	private final Company company = mock(Company.class);
	private final Project project = mock(Project.class);
	private AttachmentAccessService service;

	@BeforeEach
	void setUp() {
		service = new AttachmentAccessService(entityManager, companyMembership, projectMembers);
		when(actor.userId()).thenReturn(200L);
		when(company.getId()).thenReturn(1L);
		when(project.getId()).thenReturn(10L);
		when(project.getCompany()).thenReturn(company);
		when(companyMembership.hasActiveRole(1L, actor, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
	}

	@Test
	void activeProjectMemberCanReadButCannotMutateAnotherAuthorsDocument() {
		Document document = mock(Document.class);
		User author = user(100L);
		ProjectMember member = mock(ProjectMember.class);
		when(member.getStatus()).thenReturn(ProjectMemberStatus.ACTIVE);
		when(member.getRole()).thenReturn(ProjectMemberRole.MEMBER);
		when(document.getProject()).thenReturn(project);
		when(document.getAuthor()).thenReturn(author);
		when(entityManager.find(Document.class, 50L)).thenReturn(document);
		when(projectMembers.findByProject_IdAndUser_Id(10L, 200L)).thenReturn(Optional.of(member));

		assertThatCode(() -> service.requireReadAccess(AttachmentTargetType.DOCUMENT, 50L, actor)).doesNotThrowAnyException();
		assertThatThrownBy(() -> service.requireMutationAccess(AttachmentTargetType.DOCUMENT, 50L, actor)).isInstanceOf(ForbiddenException.class);
	}

	@Test
	void projectAdminCanMutateAnotherAuthorsDocument() {
		Document document = mock(Document.class);
		ProjectMember member = mock(ProjectMember.class);
		User author = user(100L);
		when(member.getStatus()).thenReturn(ProjectMemberStatus.ACTIVE);
		when(member.getRole()).thenReturn(ProjectMemberRole.PROJECT_ADMIN);
		when(document.getProject()).thenReturn(project);
		when(document.getAuthor()).thenReturn(author);
		when(entityManager.find(Document.class, 50L)).thenReturn(document);
		when(projectMembers.findByProject_IdAndUser_Id(10L, 200L)).thenReturn(Optional.of(member));

		assertThatCode(() -> service.requireMutationAccess(AttachmentTargetType.DOCUMENT, 50L, actor)).doesNotThrowAnyException();
	}

	@Test
	void otherDirectMessageParticipantCannotMutateSendersAttachment() {
		DirectMessage message = mock(DirectMessage.class);
		DirectMessageRoom room = mock(DirectMessageRoom.class);
		User sender = user(100L);
		when(message.getRoom()).thenReturn(room);
		when(message.getSender()).thenReturn(sender);
		when(room.getCompany()).thenReturn(company);
		when(room.includes(200L)).thenReturn(true);
		when(companyMembership.isActiveMember(1L, 200L)).thenReturn(true);
		when(entityManager.find(DirectMessage.class, 60L)).thenReturn(message);

		assertThatThrownBy(() -> service.requireMutationAccess(AttachmentTargetType.DIRECT_MESSAGE, 60L, actor)).isInstanceOf(ForbiddenException.class);
	}

	@Test
	void userOutsideCompanyCannotReadCompanyLogo() {
		when(entityManager.find(Company.class, 1L)).thenReturn(company);
		doThrowForbiddenForCompanyRead();

		assertThatThrownBy(() -> service.requireReadAccess(AttachmentTargetType.COMPANY_LOGO, 1L, actor)).isInstanceOf(ForbiddenException.class);
	}

	@Test
	void avatarOwnerCanReadWithoutSharedCompanyLookup() {
		User owner = user(200L);
		when(entityManager.find(User.class, 200L)).thenReturn(owner);

		assertThatCode(() -> service.requireReadAccess(AttachmentTargetType.USER_AVATAR, 200L, actor)).doesNotThrowAnyException();

		org.mockito.Mockito.verifyNoInteractions(companyMembership);
	}

	@Test
	void sharedActiveCompanyMemberCanReadAvatar() {
		User owner = user(100L);
		when(entityManager.find(User.class, 100L)).thenReturn(owner);
		when(companyMembership.sharesActiveCompany(200L, 100L)).thenReturn(true);

		assertThatCode(() -> service.requireReadAccess(AttachmentTargetType.USER_AVATAR, 100L, actor)).doesNotThrowAnyException();
	}

	@Test
	void requesterWithoutSharedCompanyCannotReadAvatar() {
		assertAvatarReadDeniedWhenNoActiveSharedCompany(100L);
	}

	@Test
	void suspendedMembershipDoesNotGrantAvatarRead() {
		assertAvatarReadDeniedWhenNoActiveSharedCompany(101L);
	}

	@Test
	void leftMembershipDoesNotGrantAvatarRead() {
		assertAvatarReadDeniedWhenNoActiveSharedCompany(102L);
	}

	@Test
	void inactiveCompanyDoesNotGrantAvatarRead() {
		assertAvatarReadDeniedWhenNoActiveSharedCompany(103L);
	}

	@Test
	void replacingAndClearingCurrentAvatarReferenceUpdatesOnlyTheCurrentReference() {
		User owner = user(200L);
		when(owner.getAvatarAttachmentId()).thenReturn(10L);
		when(entityManager.find(User.class, 200L)).thenReturn(owner);

		assertThat(service.replaceManagedReference(AttachmentTargetType.USER_AVATAR, 200L, 11L)).isEqualTo(10L);
		verify(owner).setAvatarAttachmentId(11L);

		when(owner.getAvatarAttachmentId()).thenReturn(11L);
		service.clearManagedReferenceIfCurrent(AttachmentTargetType.USER_AVATAR, 200L, 11L);
		verify(owner).setAvatarAttachmentId(null);
	}

	private void doThrowForbiddenForCompanyRead() {
		org.mockito.Mockito.doThrow(new ForbiddenException("회사 접근 권한이 없습니다."))
				.when(companyMembership).requireActiveMember(1L, actor);
	}

	private void assertAvatarReadDeniedWhenNoActiveSharedCompany(Long ownerUserId) {
		User owner = user(ownerUserId);
		when(entityManager.find(User.class, ownerUserId)).thenReturn(owner);
		when(companyMembership.sharesActiveCompany(200L, ownerUserId)).thenReturn(false);

		assertThatThrownBy(() -> service.requireReadAccess(AttachmentTargetType.USER_AVATAR, ownerUserId, actor))
				.isInstanceOf(ForbiddenException.class);
	}

	private User user(Long id) {
		User user = mock(User.class);
		when(user.getId()).thenReturn(id);
		return user;
	}
}
