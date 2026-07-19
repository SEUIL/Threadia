package com.threadia.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class LoggingAuthorizationCodeTokenResponseClientTest {

	@Test
	void token_response를_기본_client에_위임한다() {
		OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> delegate = mock();
		OAuth2AuthorizationCodeGrantRequest request = mock();
		OAuth2AccessTokenResponse response = mock();
		when(delegate.getTokenResponse(request)).thenReturn(response);
		LoggingAuthorizationCodeTokenResponseClient client = new LoggingAuthorizationCodeTokenResponseClient(delegate);

		assertThat(client.getTokenResponse(request)).isSameAs(response);
		verify(delegate).getTokenResponse(request);
	}

	@Test
	void allowlisted_provider_error_category를_로그하고_원본_예외를_그대로_재전파한다() {
		OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> delegate = mock();
		OAuth2AuthorizationCodeGrantRequest request = mock();
		OAuth2AuthorizationException exception = new OAuth2AuthorizationException(
				new OAuth2Error("invalid_grant", "authorization code must not be logged", null));
		when(delegate.getTokenResponse(request)).thenThrow(exception);
		LoggingAuthorizationCodeTokenResponseClient client = new LoggingAuthorizationCodeTokenResponseClient(delegate);
		ListAppender<ILoggingEvent> appender = attachAppender();

		try {
			assertThatThrownBy(() -> client.getTokenResponse(request)).isSameAs(exception);
			assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
					.containsExactly(
							"OAuth2 token exchange failed: providerErrorCategory=invalid_grant, causeSimpleClassNames=[]");
			assertThat(appender.list.get(0).getFormattedMessage())
					.doesNotContain("authorization code must not be logged");
		} finally {
			detachAppender(appender);
		}
	}

	@Test
	void allowlist_밖의_provider_error는_other_provider_error만_로그하고_원본_예외를_그대로_재전파한다() {
		OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> delegate = mock();
		OAuth2AuthorizationCodeGrantRequest request = mock();
		OAuth2AuthorizationException exception = new OAuth2AuthorizationException(
				new OAuth2Error("vendor_specific_error", "provider response must not be logged", null));
		when(delegate.getTokenResponse(request)).thenThrow(exception);
		LoggingAuthorizationCodeTokenResponseClient client = new LoggingAuthorizationCodeTokenResponseClient(delegate);
		ListAppender<ILoggingEvent> appender = attachAppender();

		try {
			assertThatThrownBy(() -> client.getTokenResponse(request)).isSameAs(exception);
			assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
					.containsExactly(
							"OAuth2 token exchange failed: providerErrorCategory=other_provider_error, causeSimpleClassNames=[]");
			assertThat(appender.list.get(0).getFormattedMessage())
					.doesNotContain("provider response must not be logged");
		} finally {
			detachAppender(appender);
		}
	}

	@Test
	void nested_cause_class_names를_최대_5개까지만_로그하고_원본_예외를_그대로_재전파한다() {
		OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> delegate = mock();
		OAuth2AuthorizationCodeGrantRequest request = mock();
		OAuth2AuthorizationException exception = new OAuth2AuthorizationException(
				new OAuth2Error("vendor_specific_error", null, null));
		exception.initCause(new TlsHandshakeFailure("tls" ,
				new CertificateValidationFailure("cert", new SocketReadFailure("socket",
						new HttpResponseParsingFailure("http", new JsonDecodingFailure("json",
								new SecretBackoffFailure("secret", null)))))));
		when(delegate.getTokenResponse(request)).thenThrow(exception);
		LoggingAuthorizationCodeTokenResponseClient client = new LoggingAuthorizationCodeTokenResponseClient(delegate);
		ListAppender<ILoggingEvent> appender = attachAppender();

		try {
			assertThatThrownBy(() -> client.getTokenResponse(request)).isSameAs(exception);
			assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).containsExactly(
					"OAuth2 token exchange failed: providerErrorCategory=other_provider_error, causeSimpleClassNames=[TlsHandshakeFailure, CertificateValidationFailure, SocketReadFailure, HttpResponseParsingFailure, JsonDecodingFailure]");
			assertThat(appender.list.get(0).getFormattedMessage()).doesNotContain("tls", "cert", "socket", "http", "json", "secret");
		} finally {
			detachAppender(appender);
		}
	}

	@Test
	void cyclic_cause를_순환_방지로_안전하게_로그하고_원본_예외를_그대로_재전파한다() {
		OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> delegate = mock();
		OAuth2AuthorizationCodeGrantRequest request = mock();
		OAuth2AuthorizationException exception = new OAuth2AuthorizationException(
				new OAuth2Error("invalid_grant", null, null));
		CyclicHandshakeFailure cyclicHandshakeFailure = new CyclicHandshakeFailure("handshake");
		CyclicTransportFailure cyclicTransportFailure = new CyclicTransportFailure("transport");
		cyclicHandshakeFailure.initCause(cyclicTransportFailure);
		cyclicTransportFailure.initCause(cyclicHandshakeFailure);
		exception.initCause(cyclicHandshakeFailure);
		when(delegate.getTokenResponse(request)).thenThrow(exception);
		LoggingAuthorizationCodeTokenResponseClient client = new LoggingAuthorizationCodeTokenResponseClient(delegate);
		ListAppender<ILoggingEvent> appender = attachAppender();

		try {
			assertThatThrownBy(() -> client.getTokenResponse(request)).isSameAs(exception);
			assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).containsExactly(
					"OAuth2 token exchange failed: providerErrorCategory=invalid_grant, causeSimpleClassNames=[CyclicHandshakeFailure, CyclicTransportFailure]");
			assertThat(appender.list.get(0).getFormattedMessage()).doesNotContain("handshake", "transport");
		} finally {
			detachAppender(appender);
		}
	}

	private static ListAppender<ILoggingEvent> attachAppender() {
		Logger logger = (Logger) LoggerFactory.getLogger(LoggingAuthorizationCodeTokenResponseClient.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		return appender;
	}

	private static void detachAppender(ListAppender<ILoggingEvent> appender) {
		Logger logger = (Logger) LoggerFactory.getLogger(LoggingAuthorizationCodeTokenResponseClient.class);
		logger.detachAppender(appender);
		appender.stop();
	}

	private static final class TlsHandshakeFailure extends RuntimeException {
		private TlsHandshakeFailure(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private static final class CertificateValidationFailure extends RuntimeException {
		private CertificateValidationFailure(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private static final class SocketReadFailure extends RuntimeException {
		private SocketReadFailure(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private static final class HttpResponseParsingFailure extends RuntimeException {
		private HttpResponseParsingFailure(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private static final class JsonDecodingFailure extends RuntimeException {
		private JsonDecodingFailure(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private static final class SecretBackoffFailure extends RuntimeException {
		private SecretBackoffFailure(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private static final class CyclicHandshakeFailure extends RuntimeException {
		private CyclicHandshakeFailure(String message) {
			super(message);
		}
	}

	private static final class CyclicTransportFailure extends RuntimeException {
		private CyclicTransportFailure(String message) {
			super(message);
		}
	}
}
