package com.threadia.backend.sprint.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.sprint.dto.*;
import com.threadia.backend.sprint.service.SprintService;
import jakarta.validation.Valid;

@RestController @RequestMapping("/api/v1")
public class SprintController {
	private final SprintService service; public SprintController(SprintService service) { this.service=service; }
	@GetMapping("/projects/{projectId}/sprints") public ResponseEntity<ApiResponse<SprintsData>> list(@RequestHeader(value="Authorization",required=false) String authorization,@PathVariable Long projectId) { return ResponseEntity.ok(ApiResponse.of(service.list(token(authorization),projectId))); }
	@PostMapping("/projects/{projectId}/sprints") public ResponseEntity<ApiResponse<SprintData>> create(@RequestHeader(value="Authorization",required=false) String authorization,@PathVariable Long projectId,@Valid @RequestBody SprintCreateRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.create(token(authorization),projectId,request))); }
	@GetMapping("/sprints/{id}") public ResponseEntity<ApiResponse<SprintData>> detail(@RequestHeader(value="Authorization",required=false) String authorization,@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.of(service.detail(token(authorization),id))); }
	@PatchMapping("/sprints/{id}") public ResponseEntity<ApiResponse<SprintData>> update(@RequestHeader(value="Authorization",required=false) String authorization,@PathVariable Long id,@Valid @RequestBody SprintUpdateRequest request) { return ResponseEntity.ok(ApiResponse.of(service.update(token(authorization),id,request))); }
	@PostMapping("/sprints/{id}/start") public ResponseEntity<ApiResponse<SprintData>> start(@RequestHeader(value="Authorization",required=false) String authorization,@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.of(service.start(token(authorization),id))); }
	@PostMapping("/sprints/{id}/complete") public ResponseEntity<ApiResponse<SprintData>> complete(@RequestHeader(value="Authorization",required=false) String authorization,@PathVariable Long id,@RequestBody(required=false) SprintCompleteRequest request) { return ResponseEntity.ok(ApiResponse.of(service.complete(token(authorization),id,request==null?new SprintCompleteRequest(null):request))); }
	@PostMapping("/sprints/{id}/issues") public ResponseEntity<ApiResponse<SprintData>> assign(@RequestHeader(value="Authorization",required=false) String authorization,@PathVariable Long id,@Valid @RequestBody SprintIssueAssignmentRequest request) { return ResponseEntity.ok(ApiResponse.of(service.assignIssues(token(authorization),id,request))); }
	private static String token(String authorization) { if(authorization==null||!authorization.startsWith("Bearer ")) throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다."); return authorization.substring(7); }
}
