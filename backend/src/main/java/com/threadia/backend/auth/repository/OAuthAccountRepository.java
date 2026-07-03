package com.threadia.backend.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.auth.domain.OAuthAccount;
import com.threadia.backend.auth.domain.OAuthProvider;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

	Optional<OAuthAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

	Optional<OAuthAccount> findByUser_IdAndProvider(Long userId, OAuthProvider provider);
}
