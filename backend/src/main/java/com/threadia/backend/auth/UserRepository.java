package com.threadia.backend.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.auth.domain.User;

public interface
UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);
}
