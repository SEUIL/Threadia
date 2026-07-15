package com.threadia.backend.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.auth.domain.User;

/**
 * users 테이블 접근 창구. signup(중복 체크/저장), login(email 조회),
 * /auth/me(id 조회, {@code JpaRepository.findById} 사용)에서 쓰인다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

	// login/email 정규화 이후의 email로 사용자를 찾는다. signup 중복 체크에도 쓰인다.
	Optional<User> findByEmail(String email);

	// signup 시 이미 가입된 이메일인지 빠르게 확인한다(row 전체 조회 없이 존재 여부만 확인).
	boolean existsByEmail(String email);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update User user
			set user.nickname = case when :nicknameProvided = true then :nickname else user.nickname end,
			    user.avatarUrl = case when :avatarUrlProvided = true then :avatarUrl else user.avatarUrl end,
			    user.updatedAt = :updatedAt
			where user.id = :id
			  and user.status = com.threadia.backend.auth.domain.UserStatus.ACTIVE
			""")
	int updateActiveProfile(
			@Param("id") Long id,
			@Param("nicknameProvided") boolean nicknameProvided,
			@Param("nickname") String nickname,
			@Param("avatarUrlProvided") boolean avatarUrlProvided,
			@Param("avatarUrl") String avatarUrl,
			@Param("updatedAt") java.time.LocalDateTime updatedAt);
}
