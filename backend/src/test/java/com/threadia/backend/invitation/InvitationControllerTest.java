package com.threadia.backend.invitation;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.invitation.dto.CompanyInvitationsData;
import com.threadia.backend.invitation.dto.InvitationActionData;
import com.threadia.backend.invitation.dto.InvitationResponse;
import com.threadia.backend.invitation.exception.InvitationConflictException;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(InvitationController.class)
class InvitationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReceivedInvitationService receivedInvitationService;

	@MockitoBean
	private InvitationActionService invitationActionService;

	@Test
	void listMyInvitationsReturnsReceivedInvitations() throws Exception {
		when(receivedInvitationService.listMine("valid-token"))
				.thenReturn(new CompanyInvitationsData(List.of(invitation(10L, "COMPANY", "PENDING"), invitation(11L, "PROJECT", "PENDING"))));

		mockMvc.perform(get("/api/v1/users/me/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.invitations[0].id").value(10))
				.andExpect(jsonPath("$.data.invitations[1].scopeType").value("PROJECT"))
				.andExpect(jsonPath("$.data.invitations[1].projectId").value(10))
				.andExpect(jsonPath("$.data.invitations[0].companyName").value("Threadia"))
				.andExpect(jsonPath("$.data.invitations[0].inviterUserId").value(1))
				.andExpect(jsonPath("$.data.invitations[0].inviterNickname").value("admin"))
				.andExpect(jsonPath("$.data.invitations[0].inviteeEmail").value("member@example.com"))
				.andExpect(jsonPath("$.data.invitations[0].inviteeNickname").value("member"))
				.andExpect(jsonPath("$.data.invitations[0].status").value("PENDING"));
	}

	@Test
	void acceptInvitationReturnsAcceptedStatus() throws Exception {
		when(invitationActionService.accept("valid-token", 10L))
				.thenReturn(new InvitationActionData(10L, "ACCEPTED", true));

		mockMvc.perform(post("/api/v1/invitations/10/accept")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.invitationId").value(10))
				.andExpect(jsonPath("$.data.status").value("ACCEPTED"))
				.andExpect(jsonPath("$.data.membershipCreated").value(true));
	}

	@Test
	void declineInvitationReturnsDeclinedStatus() throws Exception {
		when(invitationActionService.decline("valid-token", 10L))
				.thenReturn(new InvitationActionData(10L, "DECLINED", false));

		mockMvc.perform(post("/api/v1/invitations/10/decline")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.invitationId").value(10))
				.andExpect(jsonPath("$.data.status").value("DECLINED"));
	}

	@Test
	void cancelInvitationReturnsCanceledStatus() throws Exception {
		when(invitationActionService.cancel("valid-token", 10L))
				.thenReturn(new InvitationActionData(10L, "CANCELED", false));

		mockMvc.perform(post("/api/v1/invitations/10/cancel")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.invitationId").value(10))
				.andExpect(jsonPath("$.data.status").value("CANCELED"));
	}

	@Test
	void acceptInvitationReturnsConflictWhenInvitationIsExpired() throws Exception {
		when(invitationActionService.accept("valid-token", 10L))
				.thenThrow(InvitationConflictException.expired());

		mockMvc.perform(post("/api/v1/invitations/10/accept")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("INVITATION_EXPIRED"));
	}

	@Test
	void acceptInvitationReturnsForbiddenWhenRequesterIsNotInvitee() throws Exception {
		when(invitationActionService.accept("valid-token", 10L))
				.thenThrow(new ForbiddenException("초대 대상자만 처리할 수 있습니다."));

		mockMvc.perform(post("/api/v1/invitations/10/accept")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void listMyInvitationsReturnsUnauthorizedWhenAuthorizationHeaderIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/users/me/invitations"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void acceptInvitationReturnsBadRequestWhenInvitationIdIsNotNumber() throws Exception {
		mockMvc.perform(post("/api/v1/invitations/not-a-number/accept")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	private static InvitationResponse invitation(Long id, String scopeType, String status) {
		return new InvitationResponse(
				id,
				scopeType,
				1L,
				"Threadia",
				"PROJECT".equals(scopeType) ? 10L : null,
				"PROJECT".equals(scopeType) ? "Threadia 웹" : null,
				1L,
				"admin",
				2L,
				"member@example.com",
				"member",
				"MEMBER",
				status,
				"2026-07-14T10:15:30Z");
	}
}
