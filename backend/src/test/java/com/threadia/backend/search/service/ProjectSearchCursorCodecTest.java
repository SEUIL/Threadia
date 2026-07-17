package com.threadia.backend.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.threadia.backend.project.exception.ProjectValidationException;
import com.threadia.backend.search.repository.ProjectSearchRepository.SearchRow;

class ProjectSearchCursorCodecTest {

	private static final String SECRET = "test-only-project-chat-cursor-hmac-secret";

	@Test
	void signsAndVerifiesVersionedUserProjectQueryAndKeysetPayload() {
		ProjectSearchCursorCodec codec = new ProjectSearchCursorCodec(SECRET);
		SearchRow row = row(80L, "PROJECT_MESSAGE", "2026-07-17T10:00:00");

		String cursor = codec.encode(1L, 20L, "release notes", row);
		var decoded = codec.decode(cursor, 1L, 20L, "release notes");

		assertThat(cursor).contains(".");
		assertThat(decoded.sortAt()).isEqualTo(row.sortAt());
		assertThat(decoded.sourceType()).isEqualTo("PROJECT_MESSAGE");
		assertThat(decoded.sourceId()).isEqualTo(80L);
	}

	@Test
	void rejectsMalformedTamperedAndOtherScopeCursors() {
		ProjectSearchCursorCodec codec = new ProjectSearchCursorCodec(SECRET);
		String cursor = codec.encode(1L, 20L, "release", row(80L, "ISSUE", "2026-07-17T10:00:00"));
		String tampered = cursor.substring(0, cursor.length() - 1) + (cursor.endsWith("A") ? "B" : "A");

		assertThatThrownBy(() -> codec.decode("not-a-cursor", 1L, 20L, "release")).isInstanceOf(ProjectValidationException.class);
		assertThatThrownBy(() -> codec.decode(tampered, 1L, 20L, "release")).isInstanceOf(ProjectValidationException.class);
		assertThatThrownBy(() -> codec.decode(cursor, 2L, 20L, "release")).isInstanceOf(ProjectValidationException.class);
		assertThatThrownBy(() -> codec.decode(cursor, 1L, 21L, "release")).isInstanceOf(ProjectValidationException.class);
		assertThatThrownBy(() -> codec.decode(cursor, 1L, 20L, "other")).isInstanceOf(ProjectValidationException.class);
	}

	private static SearchRow row(Long id, String type, String sortAt) {
		return new SearchRow(id, type, "결과", "내용", LocalDateTime.parse(sortAt));
	}
}
