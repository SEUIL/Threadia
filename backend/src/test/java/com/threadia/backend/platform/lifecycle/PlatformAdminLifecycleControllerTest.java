package com.threadia.backend.platform.lifecycle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.domain.PlatformAdminStatus;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(PlatformAdminLifecycleController.class)
class PlatformAdminLifecycleControllerTest {
	@Autowired private MockMvc mockMvc;
	@MockitoBean private PlatformAdminLifecycleService lifecycleService;

	@Test
	void platformAdminCanListAndInspectActor() throws Exception {
		PlatformAdminResponse admin = response();
		when(lifecycleService.list("token")).thenReturn(new PlatformAdminsData(List.of(admin)));
		when(lifecycleService.actor("token")).thenReturn(new PlatformActorData(admin));

		mockMvc.perform(get("/api/v1/platform/admins").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.admins[0].status").value("ACTIVE"));
		mockMvc.perform(get("/api/v1/platform/actor").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.platformAdmin.role").value("SUPER_ADMIN"));
	}

	@Test
	void superAdminGrantUsesCreatedResponse() throws Exception {
		when(lifecycleService.grant(eq("token"), any())).thenReturn(response());
		mockMvc.perform(post("/api/v1/platform/admins")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"userId\":2,\"role\":\"PLATFORM_ADMIN\",\"reason\":\"운영 권한 부여\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.userId").value(1));
	}

	@Test
	void lifecycleConflictUsesExplicitErrorCode() throws Exception {
		when(lifecycleService.revoke(eq("token"), eq(10L), any()))
				.thenThrow(new PlatformAdminConflictException("LAST_ACTIVE_SUPER_ADMIN", "마지막 관리자입니다."));

		mockMvc.perform(post("/api/v1/platform/admins/10/revoke")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"reason\":\"권한 정리\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("LAST_ACTIVE_SUPER_ADMIN"));
	}

	private static PlatformAdminResponse response() {
		return new PlatformAdminResponse(10L, 1L, "admin@example.com", "admin", PlatformAdminRole.SUPER_ADMIN,
				PlatformAdminStatus.ACTIVE, "2026-07-18T00:00:00Z", null, null, "2026-07-18T00:00:00Z");
	}
}
