package com.threadia.backend.auth.service;

import java.util.Map;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.StringUtils;

import com.threadia.backend.common.auth.EmailNormalizer;

public record GoogleOAuthUserInfo(String providerUserId, String email, String nickname, String avatarUrl) {

	public static GoogleOAuthUserInfo from(OAuth2User user) {
		Map<String, Object> attributes = user.getAttributes();
		String providerUserId = stringAttribute(attributes, "sub");
		String email = EmailNormalizer.normalize(stringAttribute(attributes, "email"));
		String nickname = stringAttribute(attributes, "name");
		String avatarUrl = stringAttribute(attributes, "picture");
		if (!StringUtils.hasText(providerUserId) || !StringUtils.hasText(email)) {
			throw new OAuthLoginException("Google OAuth 사용자 정보가 올바르지 않습니다.");
		}
		if (!isVerifiedEmail(attributes)) {
			throw new OAuthLoginException("검증된 Google 이메일만 OAuth 로그인이 가능합니다.");
		}
		if (!StringUtils.hasText(nickname)) {
			nickname = email;
		}
		return new GoogleOAuthUserInfo(providerUserId, email, nickname, avatarUrl);
	}

	private static boolean isVerifiedEmail(Map<String, Object> attributes) {
		Object value = attributes.get("email_verified");
		return value instanceof Boolean verified && verified;
	}

	private static String stringAttribute(Map<String, Object> attributes, String name) {
		Object value = attributes.get(name);
		return value instanceof String text ? text : null;
	}
}
