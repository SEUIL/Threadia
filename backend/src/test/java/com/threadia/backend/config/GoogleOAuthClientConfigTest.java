package com.threadia.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

class GoogleOAuthClientConfigTest {

	private final GoogleOAuthClientConfig config = new GoogleOAuthClientConfig();

	@Test
	void google_registration_uses_client_secret_post_without_changing_other_metadata() {
		ClientRegistrationRepository repository = config.clientRegistrationRepository(
				"client-id",
				"client-secret",
				"{baseUrl}/api/v1/auth/oauth2/callback/{registrationId}");
		assertThat(repository).isInstanceOf(InMemoryClientRegistrationRepository.class);

		ClientRegistration registration = repository.findByRegistrationId("google");
		assertThat(registration.getClientAuthenticationMethod())
				.isEqualTo(ClientAuthenticationMethod.CLIENT_SECRET_POST);
		assertThat(registration.getClientId()).isEqualTo("client-id");
		assertThat(registration.getRedirectUri()).isEqualTo("{baseUrl}/api/v1/auth/oauth2/callback/{registrationId}");
		assertThat(registration.getScopes()).isEqualTo(Set.of("openid", "email", "profile"));
	}
}
