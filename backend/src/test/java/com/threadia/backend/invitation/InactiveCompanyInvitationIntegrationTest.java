package com.threadia.backend.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.*;
import com.threadia.backend.company.repository.*;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtTokenService;
import com.threadia.backend.invitation.domain.*;
import com.threadia.backend.invitation.dto.*;
import com.threadia.backend.invitation.repository.InvitationRepository;
import com.threadia.backend.project.domain.*;
import com.threadia.backend.project.repository.*;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:inactive-company-invitations;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop", "threadia.jwt.secret=01234567890123456789012345678901"})
@Transactional
class InactiveCompanyInvitationIntegrationTest {
	@Autowired private CompanyInvitationService companyService; @Autowired private ProjectInvitationService projectService; @Autowired private ReceivedInvitationService receivedService;
	@Autowired private UserRepository users; @Autowired private CompanyRepository companies; @Autowired private CompanyMemberRepository companyMembers; @Autowired private ProjectRepository projects; @Autowired private ProjectMemberRepository projectMembers; @Autowired private InvitationRepository invitations;
	@MockitoBean private JwtTokenService jwt;
	@MockitoBean private PlatformAdminBootstrap platformAdminBootstrap;
	private User admin; private User invitee; private Company company; private Project project; private Invitation companyInvitation; private Invitation projectInvitation;

	@BeforeEach void setUp() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 16, 5, 0); admin = users.save(user("admin@example.com", now)); invitee = users.save(user("invitee@example.com", now)); company = companies.save(new Company("Inactive", "대표", null, "IT", null, CompanyStatus.INACTIVE, admin, now, now)); companyMembers.save(new CompanyMember(company, admin, null, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE, now, now, now)); companyMembers.save(new CompanyMember(company, invitee, null, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE, now, now, now)); project = projects.save(new Project(company, "Blocked", "BLK", null, ProjectStatus.ACTIVE, null, null, admin, null, null, null, now, now)); projectMembers.save(new ProjectMember(project, admin, ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE, now, now, now)); companyInvitation = invitations.save(new Invitation(company, null, admin, invitee, InvitationScopeType.COMPANY, "MEMBER", InvitationStatus.PENDING, now.plusDays(7), now, now)); projectInvitation = invitations.save(new Invitation(company, project.getId(), admin, invitee, InvitationScopeType.PROJECT, "MEMBER", InvitationStatus.PENDING, now.plusDays(7), now, now)); when(jwt.parseAccessToken("admin-token")).thenReturn(new AuthenticatedUser(admin.getId(), admin.getEmail())); when(jwt.parseAccessToken("invitee-token")).thenReturn(new AuthenticatedUser(invitee.getId(), invitee.getEmail()));
	}

	@Test void inactiveCompanyBlocksEveryCompanyInvitationActionAndFiltersMine() {
		assertThatThrownBy(() -> companyService.create("admin-token", company.getId(), new CompanyInvitationRequest(invitee.getId(), CompanyMemberRole.MEMBER))).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> companyService.list("admin-token", company.getId())).isInstanceOf(ForbiddenException.class);
		assertThat(companyService.listMine("invitee-token").invitations()).isEmpty();
		assertThatThrownBy(() -> companyService.accept("invitee-token", companyInvitation.getId())).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> companyService.decline("invitee-token", companyInvitation.getId())).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> companyService.cancel("admin-token", companyInvitation.getId())).isInstanceOf(ForbiddenException.class);
		assertThat(invitations.findById(companyInvitation.getId())).get().extracting(Invitation::getStatus).isEqualTo(InvitationStatus.PENDING);
	}

	@Test void inactiveCompanyBlocksEveryProjectInvitationActionAndReceivedListHidesBothScopes() {
		assertThatThrownBy(() -> projectService.create("admin-token", project.getId(), new ProjectInvitationRequest(invitee.getId(), ProjectMemberRole.MEMBER))).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> projectService.list("admin-token", project.getId())).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> projectService.findInvitableMember("admin-token", project.getId(), invitee.getEmail())).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> projectService.accept("invitee-token", projectInvitation.getId())).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> projectService.decline("invitee-token", projectInvitation.getId())).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> projectService.cancel("admin-token", projectInvitation.getId())).isInstanceOf(ForbiddenException.class);
		assertThat(receivedService.listMine("invitee-token").invitations()).isEmpty();
		assertThat(invitations.findById(projectInvitation.getId())).get().extracting(Invitation::getStatus).isEqualTo(InvitationStatus.PENDING);
		assertThat(projectMembers.count()).isEqualTo(1);
	}
	private static User user(String email, LocalDateTime now) { return new User(email, "hash", email, null, UserStatus.ACTIVE, now, now); }
}
