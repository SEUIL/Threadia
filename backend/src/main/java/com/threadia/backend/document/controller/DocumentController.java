package com.threadia.backend.document.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.document.domain.DocumentType;
import com.threadia.backend.document.dto.DeletedData;
import com.threadia.backend.document.dto.DocumentCreateRequest;
import com.threadia.backend.document.dto.DocumentData;
import com.threadia.backend.document.dto.DocumentGraphData;
import com.threadia.backend.document.dto.DocumentLinkCreateRequest;
import com.threadia.backend.document.dto.DocumentLinkData;
import com.threadia.backend.document.dto.DocumentLinksData;
import com.threadia.backend.document.dto.DocumentUpdateRequest;
import com.threadia.backend.document.dto.DocumentsData;
import com.threadia.backend.document.service.DocumentService;
import com.threadia.backend.issue.dto.IssuesData;
import jakarta.validation.Valid;

@RestController @RequestMapping("/api/v1")
public class DocumentController {
	private static final String BEARER_PREFIX = "Bearer "; private final DocumentService documentService;
	public DocumentController(DocumentService documentService) { this.documentService = documentService; }
	@GetMapping("/projects/{projectId}/documents") public ResponseEntity<ApiResponse<DocumentsData>> list(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long projectId, @RequestParam(required = false) DocumentType type, @RequestParam(required = false) String q, @RequestParam(defaultValue = "false") boolean includeTrashed) { return ResponseEntity.ok(ApiResponse.of(documentService.list(token(auth), projectId, type, q, includeTrashed))); }
	@PostMapping("/projects/{projectId}/documents") public ResponseEntity<ApiResponse<DocumentData>> create(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long projectId, @Valid @RequestBody DocumentCreateRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(documentService.create(token(auth), projectId, request))); }
	@GetMapping("/documents/{documentId}") public ResponseEntity<ApiResponse<DocumentData>> detail(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long documentId) { return ResponseEntity.ok(ApiResponse.of(documentService.detail(token(auth), documentId))); }
	@PatchMapping("/documents/{documentId}") public ResponseEntity<ApiResponse<DocumentData>> update(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long documentId, @Valid @RequestBody DocumentUpdateRequest request) { return ResponseEntity.ok(ApiResponse.of(documentService.update(token(auth), documentId, request))); }
	@PostMapping("/documents/{documentId}/trash") public ResponseEntity<ApiResponse<DocumentData>> trash(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long documentId) { return ResponseEntity.ok(ApiResponse.of(documentService.trash(token(auth), documentId))); }
	@PostMapping("/documents/{documentId}/restore") public ResponseEntity<ApiResponse<DocumentData>> restore(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long documentId) { return ResponseEntity.ok(ApiResponse.of(documentService.restore(token(auth), documentId))); }
	@GetMapping("/projects/{projectId}/documents/graph") public ResponseEntity<ApiResponse<DocumentGraphData>> graph(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long projectId) { return ResponseEntity.ok(ApiResponse.of(documentService.graph(token(auth), projectId))); }
	@GetMapping("/projects/{projectId}/document-issue-links") public ResponseEntity<ApiResponse<DocumentLinksData>> listLinks(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long projectId) { return ResponseEntity.ok(ApiResponse.of(documentService.listLinks(token(auth), projectId))); }
	@PostMapping("/projects/{projectId}/document-issue-links") public ResponseEntity<ApiResponse<DocumentLinkData>> createLink(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long projectId, @Valid @RequestBody DocumentLinkCreateRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(documentService.createLink(token(auth), projectId, request))); }
	@DeleteMapping("/document-issue-links/{linkId}") public ResponseEntity<ApiResponse<DeletedData>> deleteLink(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long linkId) { return ResponseEntity.ok(ApiResponse.of(documentService.deleteLink(token(auth), linkId))); }
	@GetMapping("/issues/{issueId}/documents") public ResponseEntity<ApiResponse<DocumentsData>> documentsForIssue(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long issueId) { return ResponseEntity.ok(ApiResponse.of(documentService.documentsForIssue(token(auth), issueId))); }
	@GetMapping("/documents/{documentId}/issues") public ResponseEntity<ApiResponse<IssuesData>> issuesForDocument(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long documentId) { return ResponseEntity.ok(ApiResponse.of(documentService.issuesForDocument(token(auth), documentId))); }
	private static String token(String header) { if (header == null || !header.startsWith(BEARER_PREFIX)) throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다."); return header.substring(BEARER_PREFIX.length()); }
}
