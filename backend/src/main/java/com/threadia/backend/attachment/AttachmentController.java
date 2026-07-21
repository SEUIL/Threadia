package com.threadia.backend.attachment;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.attachment.domain.AttachmentTargetType;
import com.threadia.backend.attachment.dto.AttachmentDownloadData;
import com.threadia.backend.attachment.dto.AttachmentResponse;
import com.threadia.backend.attachment.dto.AttachmentUploadData;
import com.threadia.backend.attachment.dto.AttachmentUploadRequest;
import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {
	private static final String BEARER_PREFIX = "Bearer ";
	private final AttachmentService attachmentService;
	public AttachmentController(AttachmentService attachmentService) { this.attachmentService = attachmentService; }

	@PostMapping("/uploads")
	public ResponseEntity<ApiResponse<AttachmentUploadData>> initiate(@RequestHeader(value = "Authorization", required = false) String authorization, @Valid @RequestBody AttachmentUploadRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(attachmentService.initiate(token(authorization), request)));
	}
	@PostMapping("/{attachmentId}/complete")
	public ResponseEntity<ApiResponse<AttachmentResponse>> complete(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long attachmentId) {
		return ResponseEntity.ok(ApiResponse.of(attachmentService.complete(token(authorization), attachmentId)));
	}
	@GetMapping
	public ResponseEntity<ApiResponse<List<AttachmentResponse>>> list(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam AttachmentTargetType targetType, @RequestParam Long targetId) {
		return ResponseEntity.ok(ApiResponse.of(attachmentService.list(token(authorization), targetType, targetId)));
	}
	@GetMapping("/{attachmentId}/download")
	public ResponseEntity<ApiResponse<AttachmentDownloadData>> download(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long attachmentId) {
		return ResponseEntity.ok(ApiResponse.of(attachmentService.download(token(authorization), attachmentId)));
	}
	@DeleteMapping("/{attachmentId}")
	public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long attachmentId) {
		attachmentService.delete(token(authorization), attachmentId);
		return ResponseEntity.noContent().build();
	}
	private static String token(String authorization) { if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다."); return authorization.substring(BEARER_PREFIX.length()); }
}
