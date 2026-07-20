package com.threadia.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * frontend(Next.js, 기본 http://localhost:3000)가 refresh token HttpOnly 쿠키를 주고받으려면
 * (axios withCredentials: true) allowCredentials(true) + 명시적 origin이 필요하다.
 * allowCredentials(true)와 allowedOrigins("*")는 브라우저가 함께 허용하지 않으므로
 * 와일드카드 대신 allowedOriginPatterns로 운영/로컬 origin을 명시한다.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	@Value("${threadia.cors.allowed-origin:http://localhost:3000}")
	private String allowedOrigin;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOriginPatterns(allowedOrigin)
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.allowCredentials(true);
	}
}
