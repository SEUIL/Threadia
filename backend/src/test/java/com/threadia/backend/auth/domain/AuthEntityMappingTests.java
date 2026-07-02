package com.threadia.backend.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

class AuthEntityMappingTests {

	@Test
	void authEntitiesMapToSchemaTables() {
		assertTable(User.class, "users");
		assertTable(OAuthAccount.class, "oauth_accounts");
		assertTable(PasswordResetToken.class, "password_reset_tokens");
		assertTable(RefreshToken.class, "refresh_tokens");
	}

	@Test
	void usersMapStatusAsStringEnumWithoutRole() {
		assertThat(fieldNames(User.class)).doesNotContain("role");
		assertStringEnum(User.class, "status");
		assertNamedUniqueConstraint(User.class, "uq_users_email", "email");
		assertThat(new User().getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(UserStatus.values()).containsExactly(UserStatus.ACTIVE, UserStatus.SUSPENDED, UserStatus.DELETED);
	}

	@Test
	void usersAllowNullablePasswordHashForOAuthOnlyAccounts() {
		Column passwordHash = column(User.class, "passwordHash");

		assertThat(passwordHash.name()).isEqualTo("password_hash");
		assertThat(passwordHash.nullable()).isTrue();
	}

	@Test
	void oauthAccountsMapProviderAsStringEnumAndUserRelation() {
		assertStringEnum(OAuthAccount.class, "provider");
		assertNamedUniqueConstraint(OAuthAccount.class, "uq_oauth_provider_user", "provider", "provider_user_id");
		assertNamedUniqueConstraint(OAuthAccount.class, "uq_oauth_user_provider", "user_id", "provider");
		assertRequiredLazyUserRelation(OAuthAccount.class, "fk_oauth_user");
		assertThat(OAuthProvider.values()).containsExactly(OAuthProvider.GOOGLE, OAuthProvider.KAKAO, OAuthProvider.NAVER);
	}

	@Test
	void passwordResetTokensStoreOnlyTokenHash() {
		assertThat(fieldNames(PasswordResetToken.class)).contains("tokenHash").doesNotContain("token");
		assertNamedUniqueConstraint(PasswordResetToken.class, "uq_prt_token_hash", "token_hash");
		assertRequiredLazyUserRelation(PasswordResetToken.class, "fk_prt_user");
		assertDateTimeColumn(PasswordResetToken.class, "expiresAt", "expires_at");
		assertDateTimeColumn(PasswordResetToken.class, "usedAt", "used_at");
	}

	@Test
	void refreshTokensStoreOnlyTokenHashAndLifecycleTimestamps() {
		assertThat(fieldNames(RefreshToken.class)).contains("tokenHash").doesNotContain("token");
		assertNamedUniqueConstraint(RefreshToken.class, "uq_rt_token_hash", "token_hash");
		assertRequiredLazyUserRelation(RefreshToken.class, "fk_rt_user");
		assertDateTimeColumn(RefreshToken.class, "expiresAt", "expires_at");
		assertDateTimeColumn(RefreshToken.class, "rotatedAt", "rotated_at");
		assertDateTimeColumn(RefreshToken.class, "revokedAt", "revoked_at");
	}

	private static void assertTable(Class<?> entityType, String tableName) {
		assertThat(entityType.getAnnotation(Entity.class)).isNotNull();
		assertThat(entityType.getAnnotation(Table.class).name()).isEqualTo(tableName);
	}

	private static void assertStringEnum(Class<?> entityType, String fieldName) {
		Enumerated enumerated = field(entityType, fieldName).getAnnotation(Enumerated.class);

		assertThat(enumerated).isNotNull();
		assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
	}

	private static void assertRequiredLazyUserRelation(Class<?> entityType, String foreignKeyName) {
		Field user = field(entityType, "user");
		ManyToOne manyToOne = user.getAnnotation(ManyToOne.class);
		JoinColumn joinColumn = user.getAnnotation(JoinColumn.class);

		assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
		assertThat(manyToOne.optional()).isFalse();
		assertThat(joinColumn.name()).isEqualTo("user_id");
		assertThat(joinColumn.nullable()).isFalse();
		assertThat(joinColumn.foreignKey().name()).isEqualTo(foreignKeyName);
	}

	private static void assertDateTimeColumn(Class<?> entityType, String fieldName, String columnName) {
		Field dateTime = field(entityType, fieldName);
		Column column = dateTime.getAnnotation(Column.class);

		assertThat(dateTime.getType()).isEqualTo(LocalDateTime.class);
		assertThat(column.name()).isEqualTo(columnName);
		assertThat(column.columnDefinition()).isEqualTo("DATETIME");
	}

	private static void assertNamedUniqueConstraint(Class<?> entityType, String constraintName, String... columnNames) {
		UniqueConstraint[] uniqueConstraints = entityType.getAnnotation(Table.class).uniqueConstraints();

		assertThat(uniqueConstraints)
				.anySatisfy(constraint -> {
					assertThat(constraint.name()).isEqualTo(constraintName);
					assertThat(constraint.columnNames()).containsExactly(columnNames);
				});
	}

	private static Column column(Class<?> entityType, String fieldName) {
		return field(entityType, fieldName).getAnnotation(Column.class);
	}

	private static Field field(Class<?> entityType, String fieldName) {
		try {
			return entityType.getDeclaredField(fieldName);
		} catch (NoSuchFieldException exception) {
			throw new AssertionError("Missing field " + entityType.getSimpleName() + "." + fieldName, exception);
		}
	}

	private static String[] fieldNames(Class<?> entityType) {
		return Arrays.stream(entityType.getDeclaredFields()).map(Field::getName).toArray(String[]::new);
	}
}
