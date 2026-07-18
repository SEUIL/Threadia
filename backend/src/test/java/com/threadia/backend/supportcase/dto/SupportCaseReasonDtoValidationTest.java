package com.threadia.backend.supportcase.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.threadia.backend.supportcase.domain.SupportCaseStatus;
import com.threadia.backend.supportcase.domain.SupportCaseType;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class SupportCaseReasonDtoValidationTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void rawReasonMayExceed500WhenItsTrimmedValueIsExactly500() {
		String reason = "  " + "r".repeat(500) + "  ";
		List<Object> requests = List.of(
				new SupportCaseCreateRequest(
						SupportCaseType.INQUIRY,
						"Help",
						"Need help",
						null,
						null,
						reason),
				new PublicReplyRequest("Public reply", reason),
				new InternalNoteRequest("Internal note", reason),
				new SupportCaseStatusUpdateRequest(SupportCaseStatus.IN_PROGRESS, reason));

		for (Object request : requests) {
			assertThat(validator.validate(request)).isEmpty();
		}
	}
}
