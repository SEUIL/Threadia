package com.threadia.backend.invitation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
import com.threadia.backend.invitation.dto.InvitationResponse;
import com.threadia.backend.invitation.exception.CompanyNotFoundException;
import com.threadia.backend.invitation.exception.InvitationConflictException;
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
		assertInviteeCanBeInvited(companyId, invitee.getId());
		LocalDateTime now = LocalDateTime.now(clock);
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

	@Transactional(readOnly = true)
	public CompanyInvitationsData list(String accessToken, Long companyId) {
		User requester = activeRequester(accessToken);
		assertCompanyAdmin(companyId, requester.getId());
		company(companyId);
		List<InvitationResponse> invitations = invitationRepository
				.findByCompany_IdAndScopeTypeOrderByCreatedAtDesc(companyId, InvitationScopeType.COMPANY)
				.stream()
				.map(CompanyInvitationService::toResponse)
				.toList();
		return new CompanyInvitationsData(invitations);
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

	private User activeInvitee(Long inviteeUserId) {
		return userRepository.findById(inviteeUserId)
				.filter(user -> user.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new UserNotFoundException(String.valueOf(inviteeUserId)));
	}

	private void assertInviteeCanBeInvited(Long companyId, Long inviteeUserId) {
		if (companyMemberRepository.existsByCompany_IdAndUser_Id(companyId, inviteeUserId)) {
			throw InvitationConflictException.alreadyCompanyMember();
		}
		if (invitationRepository.existsByCompany_IdAndInvitee_IdAndScopeTypeAndStatus(
				companyId,
				inviteeUserId,
				InvitationScopeType.COMPANY,
				InvitationStatus.PENDING)) {
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
