package com.threadia.backend.invitation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.common.exception.UserNotFoundException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtTokenService;
import com.threadia.backend.invitation.domain.Invitation;
import com.threadia.backend.invitation.domain.InvitationScopeType;
import com.threadia.backend.invitation.domain.InvitationStatus;
import com.threadia.backend.invitation.dto.CompanyInvitationData;
import com.threadia.backend.invitation.dto.CompanyInvitationRequest;
import com.threadia.backend.invitation.dto.CompanyInvitationsData;
import com.threadia.backend.invitation.dto.InvitationActionData;
import com.threadia.backend.invitation.dto.InvitationResponse;
import com.threadia.backend.invitation.exception.CompanyNotFoundException;
import com.threadia.backend.invitation.exception.InvitationConflictException;
import com.threadia.backend.invitation.exception.InvitationNotFoundException;
import com.threadia.backend.invitation.repository.InvitationRepository;

@Service
public class CompanyInvitationService {

	private static final ZoneOffset UTC = ZoneOffset.UTC;
	private static final int EXPIRY_DAYS = 7;

	private final JwtTokenService jwtTokenService;
	private final UserRepository userRepository;
	private final CompanyRepository companyRepository;
	private final CompanyMemberRepository companyMemberRepository;
	private final InvitationRepository invitationRepository;
	private final Clock clock;

	@Autowired
	public CompanyInvitationService(
			JwtTokenService jwtTokenService,
			UserRepository userRepository,
			CompanyRepository companyRepository,
			CompanyMemberRepository companyMemberRepository,
			InvitationRepository invitationRepository) {
		this(jwtTokenService, userRepository, companyRepository, companyMemberRepository, invitationRepository, Clock.systemUTC());
	}

	CompanyInvitationService(
			JwtTokenService jwtTokenService,
			UserRepository userRepository,
			CompanyRepository companyRepository,
			CompanyMemberRepository companyMemberRepository,
			InvitationRepository invitationRepository,
			Clock clock) {
		this.jwtTokenService = jwtTokenService;
		this.userRepository = userRepository;
		this.companyRepository = companyRepository;
		this.companyMemberRepository = companyMemberRepository;
		this.invitationRepository = invitationRepository;
		this.clock = clock;
	}

	@Transactional
	public CompanyInvitationData create(String accessToken, Long companyId, CompanyInvitationRequest request) {
		User requester = activeRequester(accessToken);
		assertCompanyAdmin(companyId, requester.getId());
		Company company = companyForUpdate(companyId);
		User invitee = activeInvitee(request.inviteeUserId());
		LocalDateTime now = LocalDateTime.now(clock);
		assertInviteeCanBeInvited(companyId, invitee.getId(), now);
		Invitation invitation = new Invitation(
				company,
				null,
				requester,
				invitee,
				InvitationScopeType.COMPANY,
				request.role().name(),
				InvitationStatus.PENDING,
				now.plusDays(EXPIRY_DAYS),
				now,
				now);
		Invitation savedInvitation = invitationRepository.save(invitation);
		return new CompanyInvitationData(toResponse(savedInvitation));
	}

	@Transactional
	public CompanyInvitationsData list(String accessToken, Long companyId) {
		User requester = activeRequester(accessToken);
		assertCompanyAdmin(companyId, requester.getId());
		company(companyId);
		List<InvitationResponse> invitations = invitationRepository
				.findByCompany_IdAndScopeTypeOrderByCreatedAtDesc(companyId, InvitationScopeType.COMPANY)
				.stream()
				.map(this::expirePendingIfNeeded)
				.map(CompanyInvitationService::toResponse)
				.toList();
		return new CompanyInvitationsData(invitations);
	}

	@Transactional
	public CompanyInvitationsData listMine(String accessToken) {
		User requester = activeRequester(accessToken);
		List<InvitationResponse> invitations = invitationRepository
				.findByInvitee_IdAndScopeTypeOrderByCreatedAtDesc(requester.getId(), InvitationScopeType.COMPANY)
				.stream()
				.map(this::expirePendingIfNeeded)
				.map(CompanyInvitationService::toResponse)
				.toList();
		return new CompanyInvitationsData(invitations);
	}

	@Transactional(noRollbackFor = InvitationConflictException.class)
	public InvitationActionData accept(String accessToken, Long invitationId) {
		User requester = activeRequester(accessToken);
		Invitation invitation = invitationForUpdate(invitationId);
		assertCompanyInvitee(invitation, requester.getId());
		assertPending(invitation);
		LocalDateTime now = LocalDateTime.now(clock);
		expirePendingIfNeeded(invitation, now);
		assertPending(invitation);
		Optional<CompanyMember> existingMember = companyMemberRepository
				.findByCompany_IdAndUser_Id(invitation.getCompany().getId(), requester.getId());
		if (existingMember.filter(member -> member.getStatus() == CompanyMemberStatus.ACTIVE).isPresent()) {
			throw InvitationConflictException.alreadyCompanyMember();
		}
		if (existingMember.filter(member -> member.getStatus() == CompanyMemberStatus.SUSPENDED).isPresent()) {
			throw InvitationConflictException.suspendedCompanyMember();
		}
		CompanyMemberRole role = CompanyMemberRole.valueOf(invitation.getRole());
		boolean membershipCreated = existingMember.isEmpty();
		if (membershipCreated) {
			CompanyMember member = new CompanyMember(
					invitation.getCompany(),
					requester,
					null,
					role,
					CompanyMemberStatus.ACTIVE,
					now,
					now,
					now);
			companyMemberRepository.save(member);
		} else {
			existingMember.get().activate(role, now);
		}
		invitation.accept(now);
		return new InvitationActionData(invitation.getId(), invitation.getStatus().name(), membershipCreated);
	}

	@Transactional(noRollbackFor = InvitationConflictException.class)
	public InvitationActionData decline(String accessToken, Long invitationId) {
		User requester = activeRequester(accessToken);
		Invitation invitation = invitationForUpdate(invitationId);
		assertCompanyInvitee(invitation, requester.getId());
		assertPending(invitation);
		LocalDateTime now = LocalDateTime.now(clock);
		expirePendingIfNeeded(invitation, now);
		assertPending(invitation);
		invitation.decline(now);
		return new InvitationActionData(invitation.getId(), invitation.getStatus().name(), false);
	}

	@Transactional(noRollbackFor = InvitationConflictException.class)
	public InvitationActionData cancel(String accessToken, Long invitationId) {
		User requester = activeRequester(accessToken);
		Invitation invitation = invitationForUpdate(invitationId);
		assertCompanyInvitation(invitation);
		assertCompanyAdmin(invitation.getCompany().getId(), requester.getId());
		assertPending(invitation);
		LocalDateTime now = LocalDateTime.now(clock);
		expirePendingIfNeeded(invitation, now);
		assertPending(invitation);
		invitation.cancel(now);
		return new InvitationActionData(invitation.getId(), invitation.getStatus().name(), false);
	}

	private User activeRequester(String accessToken) {
		AuthenticatedUser authUser = jwtTokenService.parseAccessToken(accessToken);
		return userRepository.findById(authUser.userId())
				.filter(user -> user.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new AuthenticationFailedException("인증에 실패했습니다."));
	}

	private Company company(Long companyId) {
		return companyRepository.findById(companyId)
				.orElseThrow(() -> new CompanyNotFoundException(companyId));
	}

	private Company companyForUpdate(Long companyId) {
		return companyRepository.findByIdForUpdate(companyId)
				.orElseThrow(() -> new CompanyNotFoundException(companyId));
	}

	private void assertCompanyAdmin(Long companyId, Long requesterUserId) {
		boolean companyAdmin = companyMemberRepository.existsByCompany_IdAndUser_IdAndRoleAndStatus(
				companyId,
				requesterUserId,
				CompanyMemberRole.COMPANY_ADMIN,
				CompanyMemberStatus.ACTIVE);
		if (!companyAdmin) {
			throw new ForbiddenException("회사 관리자 권한이 필요합니다.");
		}
	}

	private Invitation invitationForUpdate(Long invitationId) {
		return invitationRepository.findByIdForUpdate(invitationId)
				.orElseThrow(() -> new InvitationNotFoundException(invitationId));
	}

	private void assertCompanyInvitee(Invitation invitation, Long requesterUserId) {
		assertCompanyInvitation(invitation);
		if (!invitation.getInvitee().getId().equals(requesterUserId)) {
			throw new ForbiddenException("초대 대상자만 처리할 수 있습니다.");
		}
	}

	private void assertCompanyInvitation(Invitation invitation) {
		if (invitation.getScopeType() != InvitationScopeType.COMPANY) {
			throw new ForbiddenException("회사 초대만 처리할 수 있습니다.");
		}
	}

	private void assertPending(Invitation invitation) {
		if (invitation.getStatus() == InvitationStatus.EXPIRED) {
			throw InvitationConflictException.expired();
		}
		if (invitation.getStatus() == InvitationStatus.CANCELED) {
			throw InvitationConflictException.canceled();
		}
		if (invitation.getStatus() != InvitationStatus.PENDING) {
			throw InvitationConflictException.invalidStatus();
		}
	}

	private Invitation expirePendingIfNeeded(Invitation invitation) {
		return expirePendingIfNeeded(invitation, LocalDateTime.now(clock));
	}

	private Invitation expirePendingIfNeeded(Invitation invitation, LocalDateTime now) {
		if (invitation.getStatus() == InvitationStatus.PENDING && !invitation.getExpiresAt().isAfter(now)) {
			invitation.expire(now);
		}
		return invitation;
	}

	private User activeInvitee(Long inviteeUserId) {
		return userRepository.findById(inviteeUserId)
				.filter(user -> user.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new UserNotFoundException(String.valueOf(inviteeUserId)));
	}

	private void assertInviteeCanBeInvited(Long companyId, Long inviteeUserId, LocalDateTime now) {
		if (companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(
				companyId,
				inviteeUserId,
				CompanyMemberStatus.ACTIVE)) {
			throw InvitationConflictException.alreadyCompanyMember();
		}
		boolean hasActivePendingInvitation = invitationRepository
				.findAllByCompany_IdAndInvitee_IdAndScopeTypeAndStatus(
						companyId,
						inviteeUserId,
						InvitationScopeType.COMPANY,
						InvitationStatus.PENDING)
				.stream()
				.map(invitation -> expirePendingIfNeeded(invitation, now))
				.anyMatch(invitation -> invitation.getStatus() == InvitationStatus.PENDING);
		if (hasActivePendingInvitation) {
			throw InvitationConflictException.alreadyPending();
		}
	}

	private static InvitationResponse toResponse(Invitation invitation) {
		return new InvitationResponse(
				invitation.getId(),
				invitation.getScopeType().name(),
				invitation.getCompany().getId(),
				invitation.getProjectId(),
				invitation.getInvitee().getId(),
				invitation.getRole(),
				invitation.getStatus().name(),
				formatUtc(invitation.getExpiresAt()));
	}

	private static String formatUtc(LocalDateTime value) {
		return value.atOffset(UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
