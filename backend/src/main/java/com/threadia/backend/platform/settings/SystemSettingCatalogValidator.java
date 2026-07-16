package com.threadia.backend.platform.settings;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class SystemSettingCatalogValidator {
	private static final int MAX_COLLECTION_SIZE = 50;
	private static final int MAX_CODE_LENGTH = 64;
	private static final int MAX_LABEL_LENGTH = 200;
	private static final int MAX_MESSAGE_LENGTH = 1_000;
	private static final int MAX_TEMPLATE_BODY_LENGTH = 5_000;

	public void validate(SystemSettingKey key, JsonNode value) {
		requireObject(value, "value");
		switch (key) {
			case SUPPORT_CASE_CATEGORIES, REPORT_REASON_CATEGORIES -> validateCodeLabelList(value, "categories");
			case ENFORCEMENT_REASON_PRESETS -> validateCodeLabelList(value, "presets");
			case ENFORCEMENT_DURATION_PRESETS -> validateDurationPresets(value);
			case REPLY_TEMPLATES -> validateReplyTemplates(value);
			case ANNOUNCEMENT_BANNER -> validateBanner(value);
			case SUPPORT_CENTER_STATUS -> validateSupportCenterStatus(value);
			case RESULT_NOTIFICATION_POLICY -> validateResultNotificationPolicy(value);
		}
	}

	private void validateCodeLabelList(JsonNode value, String field) {
		requireOnlyFields(value, Set.of(field));
		JsonNode entries = requireArray(value, field);
		requireCollectionSize(entries, field);
		Set<String> codes = new HashSet<>();
		for (JsonNode entry : entries) {
			requireObject(entry, field + " entry");
			requireOnlyFields(entry, Set.of("code", "label"));
			requireUniqueCode(codes, requireTextWithin(entry, "code", MAX_CODE_LENGTH));
			requireTextWithin(entry, "label", MAX_LABEL_LENGTH);
		}
	}

	private void validateDurationPresets(JsonNode value) {
		requireOnlyFields(value, Set.of("presets"));
		JsonNode presets = requireArray(value, "presets");
		requireCollectionSize(presets, "presets");
		Set<String> codes = new HashSet<>();
		for (JsonNode preset : presets) {
			requireObject(preset, "preset");
			requireOnlyFields(preset, Set.of("code", "durationDays"));
			requireUniqueCode(codes, requireTextWithin(preset, "code", MAX_CODE_LENGTH));
			JsonNode durationDays = required(preset, "durationDays");
			if (!durationDays.isNull() && (!durationDays.isIntegralNumber() || durationDays.longValue() <= 0)) {
				throw invalid("durationDays는 null 또는 양의 정수여야 합니다.");
			}
		}
	}

	private void validateReplyTemplates(JsonNode value) {
		requireOnlyFields(value, Set.of("templates"));
		JsonNode templates = requireArray(value, "templates");
		requireCollectionSize(templates, "templates");
		Set<String> codes = new HashSet<>();
		for (JsonNode template : templates) {
			requireObject(template, "template");
			requireOnlyFields(template, Set.of("code", "title", "body"));
			requireUniqueCode(codes, requireTextWithin(template, "code", MAX_CODE_LENGTH));
			requireTextWithin(template, "title", MAX_LABEL_LENGTH);
			requireTextWithin(template, "body", MAX_TEMPLATE_BODY_LENGTH);
		}
	}

	private void validateBanner(JsonNode value) {
		requireOnlyFields(value, Set.of("enabled", "message"));
		requireBoolean(value, "enabled");
		JsonNode message = required(value, "message");
		if (!message.isNull() && (!message.isTextual() || message.textValue().trim().isEmpty() || message.textValue().length() > MAX_MESSAGE_LENGTH)) {
			throw invalid("message는 null 또는 공백이 아닌 문자열이어야 합니다.");
		}
	}

	private void validateSupportCenterStatus(JsonNode value) {
		requireOnlyFields(value, Set.of("status", "message"));
		String status = requireNonBlankText(value, "status");
		if (!Set.of("OPEN", "MAINTENANCE", "DELAYED").contains(status)) {
			throw invalid("status는 OPEN, MAINTENANCE, DELAYED 중 하나여야 합니다.");
		}
		requireTextWithin(value, "message", MAX_MESSAGE_LENGTH);
	}

	private void validateResultNotificationPolicy(JsonNode value) {
		requireOnlyFields(value, Set.of("notifyOnPublicReply", "notifyOnCaseStatusChange"));
		requireBoolean(value, "notifyOnPublicReply");
		requireBoolean(value, "notifyOnCaseStatusChange");
	}

	private static void requireObject(JsonNode value, String name) {
		if (value == null || !value.isObject()) {
			throw invalid(name + "는 JSON object여야 합니다.");
		}
	}

	private static JsonNode requireArray(JsonNode object, String field) {
		JsonNode value = required(object, field);
		if (!value.isArray()) {
			throw invalid(field + "는 array여야 합니다.");
		}
		return value;
	}

	private static String requireNonBlankText(JsonNode object, String field) {
		JsonNode value = required(object, field);
		if (!value.isTextual() || value.textValue().trim().isEmpty()) {
			throw invalid(field + "는 공백이 아닌 문자열이어야 합니다.");
		}
		return value.textValue();
	}

	private static String requireTextWithin(JsonNode object, String field, int maxLength) {
		String text = requireNonBlankText(object, field);
		if (text.length() > maxLength) {
			throw invalid(field + "는 " + maxLength + "자 이하여야 합니다.");
		}
		return text;
	}

	private static void requireCollectionSize(JsonNode array, String field) {
		if (array.size() > MAX_COLLECTION_SIZE) {
			throw invalid(field + "는 " + MAX_COLLECTION_SIZE + "개 이하여야 합니다.");
		}
	}

	private static void requireUniqueCode(Set<String> codes, String code) {
		if (!codes.add(code)) {
			throw invalid("code는 중복될 수 없습니다.");
		}
	}

	private static void requireBoolean(JsonNode object, String field) {
		if (!required(object, field).isBoolean()) {
			throw invalid(field + "는 boolean이어야 합니다.");
		}
	}

	private static JsonNode required(JsonNode object, String field) {
		JsonNode value = object.get(field);
		if (value == null) {
			throw invalid(field + "는 필수입니다.");
		}
		return value;
	}

	private static void requireOnlyFields(JsonNode object, Set<String> allowedFields) {
		object.fieldNames().forEachRemaining(field -> {
			if (!allowedFields.contains(field)) {
				throw invalid("허용되지 않은 JSON field입니다: " + field);
			}
		});
		for (String field : allowedFields) {
			required(object, field);
		}
	}

	private static SystemSettingValidationException invalid(String message) {
		return new SystemSettingValidationException(message);
	}
}
