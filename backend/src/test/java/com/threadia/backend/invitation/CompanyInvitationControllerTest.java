package com.threadia.backend.invitation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.invitation.dto.CompanyInvitationData;
import com.threadia.backend.invitation.dto.CompanyInvitationRequest;
import com.threadia.backend.invitation.dto.CompanyInvitationsData;
import com.threadia.backend.invitation.dto.InvitationResponse;
import com.threadia.backend.invitation.exception.InvitationConflictException;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CompanyInvitationController.class)
class CompanyInvitationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CompanyInvitationService companyInvitationService;

	@Test
	void createCompanyInvitationReturnsCreatedInvitation() throws Exception {
		InvitationResponse invitation = invitation(10L, 1L, 2L, "MEMBER");
		when(companyInvitationService.create("valid-token", 1L, new CompanyInvitationRequest(2L, CompanyMemberRole.MEMBER)))
				.thenReturn(new CompanyInvitationData(invitation));

		mockMvc.perform(post("/api/v1/companies/1/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"inviteeUserId":2,"role":"MEMBER"}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.invitation.id").value(10))
				.andExpect(jsonPath("$.data.invitation.scopeType").value("COMPANY"))
				.andExpect(jsonPath("$.data.invitation.companyId").value(1))
				.andExpect(jsonPath("$.data.invitation.projectId").isEmpty())
				.andExpect(jsonPath("$.data.invitation.inviteeUserId").value(2))
				.andExpect(jsonPath("$.data.invitation.status").value("PENDING"))
				.andExpect(jsonPath("$.data.invitation.expiresAt").value("2026-07-14T10:15:30Z"))
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void listCompanyInvitationsReturnsInvitations() throws Exception {
		when(companyInvitationService.list("valid-token", 1L))
				.thenReturn(new CompanyInvitationsData(List.of(invitation(10L, 1L, 2L, "MEMBER"))));

		mockMvc.perform(get("/api/v1/companies/1/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.invitations[0].id").value(10))
				.andExpect(jsonPath("$.data.invitations[0].scopeType").value("COMPANY"))
				.andExpect(jsonPath("$.data.invitations[0].projectId").isEmpty())
				.andExpect(jsonPath("$.data.invitations[0].inviteeUserId").value(2))
				.andExpect(jsonPath("$.data.invitations[0].status").value("PENDING"));
	}

	@Test
	void createCompanyInvitationReturnsUnauthorizedWhenAuthorizationHeaderIsMissing() throws Exception {
		mockMvc.perform(post("/api/v1/companies/1/invitations")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"inviteeUserId":2,"role":"MEMBER"}
							"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void createCompanyInvitationReturnsUnauthorizedWhenRequesterIsNotActive() throws Exception {
		when(companyInvitationService.create(any(), any(), any()))
				.thenThrow(new AuthenticationFailedException("인증에 실패했습니다."));

		mockMvc.perform(post("/api/v1/companies/1/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer inactive-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"inviteeUserId":2,"role":"MEMBER"}
							"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void createCompanyInvitationReturnsForbiddenWhenRequesterIsNotCompanyAdmin() throws Exception {
		when(companyInvitationService.create(any(), any(), any()))
				.thenThrow(new ForbiddenException("회사 관리자 권한이 필요합니다."));

		mockMvc.perform(post("/api/v1/companies/1/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer member-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"inviteeUserId":2,"role":"MEMBER"}
							"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void createCompanyInvitationReturnsConflictWhenInvitationIsAlreadyPending() throws Exception {
		when(companyInvitationService.create(any(), any(), any()))
				.thenThrow(InvitationConflictException.alreadyPending());

		mockMvc.perform(post("/api/v1/companies/1/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"inviteeUserId":2,"role":"MEMBER"}
							"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("INVITATION_ALREADY_PENDING"));
	}

	@Test
	void createCompanyInvitationReturnsBadRequestWhenRoleIsUnsupported() throws Exception {
		mockMvc.perform(post("/api/v1/companies/1/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"inviteeUserId":2,"role":"PROJECT_ADMIN"}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void createCompanyInvitationReturnsBadRequestWhenJsonBodyIsNull() throws Exception {
		mockMvc.perform(post("/api/v1/companies/1/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("null"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void createCompanyInvitationReturnsBadRequestWhenBodyIsEmpty() throws Exception {
		mockMvc.perform(post("/api/v1/companies/1/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content(""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void createCompanyInvitationReturnsBadRequestWhenRequiredFieldsAreMissing() throws Exception {
		mockMvc.perform(post("/api/v1/companies/1/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void createCompanyInvitationReturnsUnsupportedMediaTypeWhenContentTypeIsNotJson() throws Exception {
		mockMvc.perform(post("/api/v1/companies/1/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.TEXT_PLAIN)
					.content("inviteeUserId=2&role=MEMBER"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
	}

	@Test
	void createCompanyInvitationReturnsBadRequestWhenCompanyIdIsNotNumber() throws Exception {
		mockMvc.perform(post("/api/v1/companies/not-a-number/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"inviteeUserId":2,"role":"MEMBER"}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void listCompanyInvitationsReturnsBadRequestWhenCompanyIdIsNotNumber() throws Exception {
		mockMvc.perform(get("/api/v1/companies/not-a-number/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void listCompanyInvitationsReturnsUnauthorizedWhenTokenIsInvalid() throws Exception {
		when(companyInvitationService.list("invalid-token", 1L))
				.thenThrow(new JwtAuthenticationException("유효하지 않은 access token입니다."));

		mockMvc.perform(get("/api/v1/companies/1/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	private static InvitationResponse invitation(Long id, Long companyId, Long inviteeUserId, String role) {
		return new InvitationResponse(
				id,
				"COMPANY",
				companyId,
				null,
				inviteeUserId,
				role,
				"PENDING",
				"2026-07-14T10:15:30Z");
	}
}
