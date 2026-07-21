package com.threadia.backend.attachment.dto;

import com.threadia.backend.attachment.domain.AttachmentTargetType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AttachmentUploadRequest(
		@NotNull AttachmentTargetType targetType,
		@NotNull @Min(1) Long targetId,
		@NotBlank @Pattern(regexp = "^[^/:\\\\\\p{Cc}]{1,255}$", message = "파일명은 경로 또는 제어 문자를 포함할 수 없습니다.") String filename,
		@NotBlank @Pattern(regexp = "^[A-Za-z0-9!#$&^_.+-]+/[A-Za-z0-9!#$&^_.+-]+$", message = "Content-Type 형식이 올바르지 않습니다.") String contentType,
		@Min(1) @Max(104857600) long contentLength,
		@NotBlank @Pattern(regexp = "^[A-Za-z0-9+/]{43}=$", message = "SHA-256 checksum은 Base64 형식이어야 합니다.") String checksumSha256) { }
