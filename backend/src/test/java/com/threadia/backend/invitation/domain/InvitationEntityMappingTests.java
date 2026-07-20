package com.threadia.backend.invitation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.domain.CompanyStatusConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

class InvitationEntityMappingTests {

	@Test
	void sprint15InvitationEntitiesMapToSchemaTables() {
		assertTable(Company.class, "companies");
		assertTable(CompanyMember.class, "company_members");
		assertTable(Invitation.class, "invitations");
	}

	@Test
	void companiesMapCreatedByUserAndStatusWithoutUserRole() {
		assertRequiredLazyRelation(Company.class, "createdBy", User.class, "created_by_user_id", "fk_companies_created_by");
		assertVarcharEnum(Company.class, "status", CompanyStatusConverter.class);
		assertThat(CompanyStatus.values()).containsExactly(CompanyStatus.ACTIVE, CompanyStatus.INACTIVE);
		assertThat(newInstance(Company.class).getStatus()).isEqualTo(CompanyStatus.ACTIVE);
	}

	@Test
	void companyMembersMapCompanyUserRoleAndStatus() {
		assertNamedUniqueConstraint(CompanyMember.class, "uq_cm_company_user", "company_id", "user_id");
		assertRequiredLazyRelation(CompanyMember.class, "company", Company.class, "company_id", "fk_cm_company");
		assertRequiredLazyRelation(CompanyMember.class, "user", User.class, "user_id", "fk_cm_user");
		assertStringEnum(CompanyMember.class, "role");
		assertStringEnum(CompanyMember.class, "status");
		assertDateTimeColumn(CompanyMember.class, "joinedAt", "joined_at");
		assertThat(CompanyMemberRole.values()).containsExactly(CompanyMemberRole.COMPANY_ADMIN, CompanyMemberRole.MEMBER);
		assertThat(CompanyMemberStatus.values())
				.containsExactly(CompanyMemberStatus.ACTIVE, CompanyMemberStatus.SUSPENDED, CompanyMemberStatus.LEFT);
		assertThat(newInstance(CompanyMember.class).getStatus()).isEqualTo(CompanyMemberStatus.ACTIVE);
	}

	@Test
	void invitationsMapCompanyUsersScopeStatusAndLifecycleTimestamps() {
		assertThat(fieldNames(Invitation.class)).contains("projectId");
		assertThat(column(Invitation.class, "projectId").name()).isEqualTo("project_id");
		assertRequiredLazyRelation(Invitation.class, "company", Company.class, "company_id", "fk_inv_company");
		assertRequiredLazyRelation(Invitation.class, "inviter", User.class, "inviter_user_id", "fk_inv_inviter");
		assertRequiredLazyRelation(Invitation.class, "invitee", User.class, "invitee_user_id", "fk_inv_invitee");
		assertStringEnum(Invitation.class, "scopeType");
		assertStringEnum(Invitation.class, "status");
		assertDateTimeColumn(Invitation.class, "expiresAt", "expires_at");
		assertDateTimeColumn(Invitation.class, "acceptedAt", "accepted_at");
		assertDateTimeColumn(Invitation.class, "declinedAt", "declined_at");
		assertDateTimeColumn(Invitation.class, "canceledAt", "canceled_at");
		assertThat(InvitationStatus.values()).containsExactly(
				InvitationStatus.PENDING,
				InvitationStatus.ACCEPTED,
				InvitationStatus.DECLINED,
				InvitationStatus.CANCELED,
				InvitationStatus.EXPIRED);
		assertThat(new Invitation().getStatus()).isEqualTo(InvitationStatus.PENDING);
	}

	@Test
	void invitationScopeKeepsProjectAsFutureSchemaValueWhileSprint15UsesCompanyScope() {
		assertThat(InvitationScopeType.values()).containsExactly(InvitationScopeType.COMPANY, InvitationScopeType.PROJECT);
		assertThat(InvitationScopeType.COMPANY.name()).isEqualTo("COMPANY");
		assertThat(InvitationScopeType.PROJECT.name()).isEqualTo("PROJECT");
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

	private static void assertVarcharEnum(Class<?> entityType, String fieldName, Class<?> converterType) {
		Convert convert = field(entityType, fieldName).getAnnotation(Convert.class);

		assertThat(convert).isNotNull();
		assertThat(convert.converter()).isEqualTo(converterType);
	}

	private static void assertRequiredLazyRelation(
			Class<?> entityType,
			String fieldName,
			Class<?> targetType,
			String columnName,
			String foreignKeyName) {
		Field relation = field(entityType, fieldName);
		ManyToOne manyToOne = relation.getAnnotation(ManyToOne.class);
		JoinColumn joinColumn = relation.getAnnotation(JoinColumn.class);

		assertThat(relation.getType()).isEqualTo(targetType);
		assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
		assertThat(manyToOne.optional()).isFalse();
		assertThat(joinColumn.name()).isEqualTo(columnName);
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

	private static <T> T newInstance(Class<T> entityType) {
		try {
			var constructor = entityType.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError("Cannot instantiate " + entityType.getSimpleName(), exception);
		}
	}

	private static String[] fieldNames(Class<?> entityType) {
		return Arrays.stream(entityType.getDeclaredFields()).map(Field::getName).toArray(String[]::new);
	}
}
