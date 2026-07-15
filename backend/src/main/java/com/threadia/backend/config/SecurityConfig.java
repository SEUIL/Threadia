package com.threadia.backend.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

import com.threadia.backend.auth.controller.OAuth2LoginFailureHandler;
import com.threadia.backend.auth.controller.OAuth2LoginSuccessHandler;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
			OAuth2LoginSuccessHandler successHandler,
			OAuth2LoginFailureHandler failureHandler) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
		if (clientRegistrationRepository.getIfAvailable() != null) {
			http.oauth2Login(oauth2 -> oauth2
					.authorizationEndpoint(authorization -> authorization.baseUri("/oauth2/authorization"))
					.redirectionEndpoint(redirection -> redirection.baseUri("/api/v1/auth/oauth2/callback/*"))
					.successHandler(successHandler)
					.failureHandler(failureHandler));
		}
		return http.build();
	}
}
