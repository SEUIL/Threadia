package com.threadia.backend.config.cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RefreshTokenCookieProviderImplTest {

	@Test
	void prod_profile에서는_secure_false이면_생성에_실패한다() {
		assertThatThrownBy(() -> new RefreshTokenCookieProviderImpl(false, 14, "prod"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void prod_profile에서도_secure_true이면_쿠키를_생성한다() {
		RefreshTokenCookieProviderImpl provider = new RefreshTokenCookieProviderImpl(true, 14, "prod");

		assertThat(provider.create("raw-token").isSecure()).isTrue();
	}

	@Test
	void local_profile에서는_secure_false를_허용한다() {
		RefreshTokenCookieProviderImpl provider = new RefreshTokenCookieProviderImpl(false, 14, "local");

		assertThat(provider.create("raw-token").isSecure()).isFalse();
	}
}
