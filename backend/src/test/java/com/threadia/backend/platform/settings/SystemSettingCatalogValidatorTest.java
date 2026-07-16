package com.threadia.backend.platform.settings;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class SystemSettingCatalogValidatorTest {
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final SystemSettingCatalogValidator validator = new SystemSettingCatalogValidator();

	@Test
	void acceptsTheEightCodeOwnedValueShapes() throws Exception {
		assertThatCode(() -> {
			validator.validate(SystemSettingKey.SUPPORT_CASE_CATEGORIES, json("{\"categories\":[{\"code\":\"ACCOUNT\",\"label\":\"계정\"}]}"));
			validator.validate(SystemSettingKey.REPORT_REASON_CATEGORIES, json("{\"categories\":[{\"code\":\"SPAM\",\"label\":\"스팸\"}]}"));
			validator.validate(SystemSettingKey.ENFORCEMENT_REASON_PRESETS, json("{\"presets\":[{\"code\":\"POLICY\",\"label\":\"정책 위반\"}]}"));
			validator.validate(SystemSettingKey.ENFORCEMENT_DURATION_PRESETS, json("{\"presets\":[{\"code\":\"ONE_DAY\",\"durationDays\":1},{\"code\":\"INDEFINITE\",\"durationDays\":null}]}"));
			validator.validate(SystemSettingKey.REPLY_TEMPLATES, json("{\"templates\":[{\"code\":\"DEFAULT\",\"title\":\"기본\",\"body\":\"답변\"}]}"));
			validator.validate(SystemSettingKey.ANNOUNCEMENT_BANNER, json("{\"enabled\":false,\"message\":null}"));
			validator.validate(SystemSettingKey.SUPPORT_CENTER_STATUS, json("{\"status\":\"OPEN\",\"message\":\"정상 운영\"}"));
			validator.validate(SystemSettingKey.RESULT_NOTIFICATION_POLICY, json("{\"notifyOnPublicReply\":true,\"notifyOnCaseStatusChange\":true}"));
		}).doesNotThrowAnyException();
	}

	@Test
	void rejectsUnknownFieldsAndInvalidTypedValues() throws Exception {
		assertThatThrownBy(() -> validator.validate(SystemSettingKey.ANNOUNCEMENT_BANNER, json("{\"enabled\":true,\"message\":\"공지\",\"unknown\":true}")))
				.isInstanceOf(SystemSettingValidationException.class);
		assertThatThrownBy(() -> validator.validate(SystemSettingKey.ENFORCEMENT_DURATION_PRESETS, json("{\"presets\":[{\"code\":\"ZERO\",\"durationDays\":0}]}")))
				.isInstanceOf(SystemSettingValidationException.class);
		assertThatThrownBy(() -> validator.validate(SystemSettingKey.SUPPORT_CENTER_STATUS, json("{\"status\":\"CLOSED\",\"message\":\"x\"}")))
				.isInstanceOf(SystemSettingValidationException.class);
	}

	private com.fasterxml.jackson.databind.JsonNode json(String value) throws Exception { return objectMapper.readTree(value); }
}
