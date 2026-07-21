package com.threadia.backend.attachment;

import org.springframework.stereotype.Service;

import com.threadia.backend.attachment.domain.AttachmentTargetType;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.directmessage.domain.DirectMessage;
import com.threadia.backend.document.domain.Document;
import com.threadia.backend.issue.domain.Issue;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.projectchat.domain.Message;
import com.threadia.backend.thread.domain.ThreadComment;

import jakarta.persistence.EntityManager;

@Service
public class AttachmentAccessService {
	private final EntityManager entityManager;
	private final CompanyMembershipService companyMembershipService;
	private final ProjectMemberRepository projectMemberRepository;

	public AttachmentAccessService(EntityManager entityManager, CompanyMembershipService companyMembershipService, ProjectMemberRepository projectMemberRepository) {
		this.entityManager = entityManager;
		this.companyMembershipService = companyMembershipService;
		this.projectMemberRepository = projectMemberRepository;
	}

	public Company requireReadAccess(AttachmentTargetType targetType, Long targetId, AuthContext context) {
		return switch (targetType) {
			case DOCUMENT, BINARY_DOCUMENT -> requireProjectRead(document(targetId).getProject(), context);
			case ISSUE -> requireProjectRead(issue(targetId).getProject(), context);
			case MESSAGE -> requireProjectRead(message(targetId).getProject(), context);
			case THREAD_COMMENT -> requireProjectRead(comment(targetId).getThread().getProject(), context);
			case DIRECT_MESSAGE -> requireDirectMessageRead(directMessage(targetId), context);
			case USER_AVATAR -> requireAvatarRead(targetId, context);
			case COMPANY_LOGO -> requireCompanyRead(company(targetId), context);
		};
	}

	public Company requireMutationAccess(AttachmentTargetType targetType, Long targetId, AuthContext context) {
		return switch (targetType) {
			case DOCUMENT, BINARY_DOCUMENT -> requireProjectMutation(document(targetId).getProject(), document(targetId).getAuthor().getId(), context);
			case ISSUE -> requireProjectMutation(issue(targetId).getProject(), issue(targetId).getReporter().getId(), context);
			case MESSAGE -> requireProjectMutation(message(targetId).getProject(), message(targetId).getAuthor().getId(), context);
			case THREAD_COMMENT -> requireProjectMutation(comment(targetId).getThread().getProject(), comment(targetId).getAuthor().getId(), context);
			case DIRECT_MESSAGE -> requireDirectMessageMutation(directMessage(targetId), context);
			case USER_AVATAR -> requireAvatarOwner(targetId, context);
			case COMPANY_LOGO -> requireCompanyAdmin(company(targetId), context);
		};
	}

	private Company requireProjectRead(Project project, AuthContext context) {
		Company company = project.getCompany();
		if (companyMembershipService.hasActiveRole(company.getId(), context, CompanyMemberRole.COMPANY_ADMIN)) return company;
		companyMembershipService.requireActiveMember(company.getId(), context);
		projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), context.userId())
				.filter(member -> member.getStatus() == ProjectMemberStatus.ACTIVE)
				.orElseThrow(() -> new ForbiddenException("프로젝트 접근 권한이 없습니다."));
		return company;
	}

	private Company requireProjectMutation(Project project, Long authorUserId, AuthContext context) {
		Company company = requireProjectRead(project, context);
		if (authorUserId.equals(context.userId()) || isProjectAdmin(project, context)) return company;
		throw new ForbiddenException("첨부 파일을 변경할 권한이 없습니다.");
	}

	private boolean isProjectAdmin(Project project, AuthContext context) {
		return companyMembershipService.hasActiveRole(project.getCompany().getId(), context, CompanyMemberRole.COMPANY_ADMIN)
				|| projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), context.userId())
						.filter(member -> member.getStatus() == ProjectMemberStatus.ACTIVE && member.getRole() == ProjectMemberRole.PROJECT_ADMIN)
						.isPresent();
	}

	private Company requireDirectMessageRead(DirectMessage directMessage, AuthContext context) {
		if (!directMessage.getRoom().includes(context.userId())) throw new ForbiddenException("DM 첨부 파일 접근 권한이 없습니다.");
		Company company = directMessage.getRoom().getCompany();
		if (!companyMembershipService.isActiveMember(company.getId(), context.userId())) throw new ForbiddenException("DM 첨부 파일 접근 권한이 없습니다.");
		return company;
	}

	private Company requireDirectMessageMutation(DirectMessage directMessage, AuthContext context) {
		Company company = requireDirectMessageRead(directMessage, context);
		if (directMessage.getSender().getId().equals(context.userId())
				|| companyMembershipService.hasActiveRole(company.getId(), context, CompanyMemberRole.COMPANY_ADMIN)) return company;
		throw new ForbiddenException("첨부 파일을 변경할 권한이 없습니다.");
	}

	private Company requireAvatarOwner(Long userId, AuthContext context) {
		if (!context.userId().equals(userId)) throw new ForbiddenException("본인 아바타만 변경할 수 있습니다.");
		required(User.class, userId);
		return null;
	}

	private Company requireAvatarRead(Long ownerUserId, AuthContext context) {
		required(User.class, ownerUserId);
		if (context.userId().equals(ownerUserId) || companyMembershipService.sharesActiveCompany(context.userId(), ownerUserId)) return null;
		throw new ForbiddenException("아바타 접근 권한이 없습니다.");
	}

	public Long replaceManagedReference(AttachmentTargetType targetType, Long targetId, Long attachmentId) {
		if (targetType == AttachmentTargetType.USER_AVATAR) {
			User user = required(User.class, targetId);
			Long previousAttachmentId = user.getAvatarAttachmentId();
			user.setAvatarAttachmentId(attachmentId);
			return previousAttachmentId;
		}
		if (targetType == AttachmentTargetType.COMPANY_LOGO) {
			Company company = company(targetId);
			Long previousAttachmentId = company.getLogoAttachmentId();
			company.setLogoAttachmentId(attachmentId);
			return previousAttachmentId;
		}
		return null;
	}

	public void clearManagedReferenceIfCurrent(AttachmentTargetType targetType, Long targetId, Long attachmentId) {
		if (targetType == AttachmentTargetType.USER_AVATAR) {
			User user = required(User.class, targetId);
			if (attachmentId.equals(user.getAvatarAttachmentId())) user.setAvatarAttachmentId(null);
		}
		if (targetType == AttachmentTargetType.COMPANY_LOGO) {
			Company company = company(targetId);
			if (attachmentId.equals(company.getLogoAttachmentId())) company.setLogoAttachmentId(null);
		}
	}

	private Company requireCompanyAdmin(Company company, AuthContext context) {
		if (!companyMembershipService.hasActiveRole(company.getId(), context, CompanyMemberRole.COMPANY_ADMIN)) {
			throw new ForbiddenException("회사 관리자 권한이 필요합니다.");
		}
		return company;
	}

	private Company requireCompanyRead(Company company, AuthContext context) {
		companyMembershipService.requireActiveMember(company.getId(), context);
		return company;
	}

	private Document document(Long id) { return required(Document.class, id); }
	private Issue issue(Long id) { return required(Issue.class, id); }
	private Message message(Long id) { return required(Message.class, id); }
	private ThreadComment comment(Long id) { return required(ThreadComment.class, id); }
	private DirectMessage directMessage(Long id) { return required(DirectMessage.class, id); }
	private Company company(Long id) { return required(Company.class, id); }
	private <T> T required(Class<T> type, Long id) {
		T entity = entityManager.find(type, id);
		if (entity == null) throw new ForbiddenException("첨부 대상에 접근할 수 없습니다.");
		return entity;
	}
}
