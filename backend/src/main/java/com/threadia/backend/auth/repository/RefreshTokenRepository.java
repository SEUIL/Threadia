package com.threadia.backend.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.auth.domain.RefreshToken;

/**
 * refresh_tokens 테이블 접근 창구. login에서 새 토큰을 저장하고,
 * logout/refresh에서 쿠키의 raw token을 해시해 대조 조회하는 데 쓴다.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	// 쿠키에서 꺼낸 raw refresh token을 해시한 값으로 DB row를 찾는다. 원문은 저장하지 않는다.
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	// 특정 사용자의 모든 refresh token을 조회한다. 비밀번호 재설정 성공 시 "전체 revoke"에 쓰인다.
	List<RefreshToken> findByUser_Id(Long userId);
}
