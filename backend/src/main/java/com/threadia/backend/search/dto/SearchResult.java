package com.threadia.backend.search.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record SearchResult(
		Long id,
		String type,
		String title,
		String snippet,
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC") LocalDateTime sortAt) {
}
