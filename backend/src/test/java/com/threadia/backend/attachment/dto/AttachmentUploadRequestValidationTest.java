package com.threadia.backend.attachment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.threadia.backend.attachment.domain.AttachmentTargetType;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class AttachmentUploadRequestValidationTest {
	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void rejectsControlCharactersAndPathSeparatorsInFilename() {
		assertThat(violations("bad\r\nname.txt")).isNotEmpty();
		assertThat(violations("bad\u0000name.txt")).isNotEmpty();
		assertThat(violations("bad\u0085name.txt")).isNotEmpty();
		assertThat(violations("nested/file.txt")).isNotEmpty();
		assertThat(violations("nested\\file.txt")).isNotEmpty();
		assertThat(violations("C:drive.txt")).isNotEmpty();
	}

	@Test
	void acceptsRegularFilename() {
		assertThat(violations("보고서 2026.pdf")).isEmpty();
	}

	private Set<?> violations(String filename) {
		return validator.validate(new AttachmentUploadRequest(AttachmentTargetType.DOCUMENT, 1L, filename, "application/pdf", 1L,
				"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="));
	}
}
