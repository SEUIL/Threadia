package com.threadia.backend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.util.StringUtils;

@Configuration
public class GoogleOAuthClientConfig {

	@Bean
	@Conditional(GoogleOAuthClientPresentCondition.class)
	ClientRegistrationRepository clientRegistrationRepository(
			@Value("${GOOGLE_CLIENT_ID:}") String clientId,
			@Value("${GOOGLE_CLIENT_SECRET:}") String clientSecret,
			@Value("${GOOGLE_REDIRECT_URI:{baseUrl}/api/v1/auth/oauth2/callback/{registrationId}}") String redirectUri) {
		ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
				.clientId(clientId)
				.clientSecret(clientSecret)
				.redirectUri(redirectUri)
				.scope(List.of("openid", "email", "profile"))
				.build();
		return new InMemoryClientRegistrationRepository(google);
	}

	public static class GoogleOAuthClientPresentCondition implements Condition {
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return StringUtils.hasText(context.getEnvironment().getProperty("GOOGLE_CLIENT_ID"))
					&& StringUtils.hasText(context.getEnvironment().getProperty("GOOGLE_CLIENT_SECRET"));
		}
	}
}
