package com.threadia.backend.platform.settings;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import jakarta.persistence.Column;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Version;

class SystemSettingEntityMappingTest {
	@Test
	void mapsJsonValueAndOptimisticVersionToCanonicalColumns() throws Exception {
		Field value = SystemSetting.class.getDeclaredField("value");
		Field version = SystemSetting.class.getDeclaredField("version");
		assertThat(value.getAnnotation(Column.class).name()).isEqualTo("value_json");
		assertThat(value.getAnnotation(JdbcTypeCode.class).value()).isEqualTo(SqlTypes.JSON);
		assertThat(version.getAnnotation(Version.class)).isNotNull();
		assertThat(version.getAnnotation(Column.class).name()).isEmpty();
	}

	@Test
	void repositoryUsesPessimisticWriteLockForReplacement() throws Exception {
		Method method = SystemSettingRepository.class.getMethod("findByKeyForUpdate", SystemSettingKey.class);
		assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
		QueryHint lockTimeoutHint = method.getAnnotation(QueryHints.class).value()[0];
		assertThat(lockTimeoutHint.name()).isEqualTo("jakarta.persistence.lock.timeout");
		assertThat(lockTimeoutHint.value()).isEqualTo("0");
		assertThat(method.getAnnotation(Query.class).value()).contains("setting.key = :key");
	}
}
