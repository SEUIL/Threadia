package com.threadia.backend.platform.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyStatus;

class PlatformCompanyCursorCodecTest {
	private static final String SECRET = "01234567890123456789012345678901";

	@Test
	void bindsCursorToRequesterAndNormalizedFiltersWithoutCompanyName() throws Exception {
		PlatformCompanyCursorCodec codec = new PlatformCompanyCursorCodec(SECRET);
		PlatformCompanyFilters filters = new PlatformCompanyFilters(CompanyStatus.ACTIVE, "threadia");
		Company company = company(9L);

		String cursor = codec.encode(1L, filters, company);

		assertThat(cursor).doesNotContain("Threadia", "threadia", "admin@example.com");
		assertThat(codec.decode(cursor, 1L, filters).id()).isEqualTo(9L);
		assertThatThrownBy(() -> codec.decode(cursor + "x", 1L, filters)).isInstanceOf(PlatformCompanyValidationException.class);
		assertThatThrownBy(() -> codec.decode(cursor, 2L, filters)).isInstanceOf(PlatformCompanyValidationException.class);
		assertThatThrownBy(() -> codec.decode(cursor, 1L, new PlatformCompanyFilters(CompanyStatus.INACTIVE, "threadia")))
				.isInstanceOf(PlatformCompanyValidationException.class);
		assertThatThrownBy(() -> codec.decode(cursor, 1L, new PlatformCompanyFilters(CompanyStatus.ACTIVE, "different")))
				.isInstanceOf(PlatformCompanyValidationException.class);
	}

	@Test
	void rejectsWrongDomainAndMalformedCursor() throws Exception {
		PlatformCompanyCursorCodec codec = new PlatformCompanyCursorCodec(SECRET);
		PlatformCompanyFilters filters = new PlatformCompanyFilters(null, null);
		String wrongDomain = signedCursor("AUDIT:1:1:-:-:9");

		assertThatThrownBy(() -> codec.decode(wrongDomain, 1L, filters)).isInstanceOf(PlatformCompanyValidationException.class);
		assertThatThrownBy(() -> codec.decode("malformed", 1L, filters)).isInstanceOf(PlatformCompanyValidationException.class);
	}

	private static Company company(Long id) throws Exception {
		LocalDateTime now = LocalDateTime.of(2026, 7, 20, 0, 0);
		User user = new User("admin@example.com", "hash", "admin", null, UserStatus.ACTIVE, now, now);
		setId(user, 1L);
		Company company = new Company("Threadia", "대표", null, "IT", null, CompanyStatus.ACTIVE, user, now, now);
		setId(company, id);
		return company;
	}

	private static String signedCursor(String payload) throws GeneralSecurityException {
		byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return base64(payloadBytes) + "." + base64(mac.doFinal(payloadBytes));
	}

	private static String base64(byte[] value) {
		return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	private static void setId(Object target, Long id) throws Exception { Field field = target.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(target, id); }
}
