package com.threadia.backend.directmessage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.directmessage.dto.DirectMessageData;
import com.threadia.backend.directmessage.dto.DirectMessageMarkReadRequest;
import com.threadia.backend.directmessage.dto.DirectMessageReadData;
import com.threadia.backend.directmessage.dto.DirectMessageRoomCreateRequest;
import com.threadia.backend.directmessage.dto.DirectMessageRoomCreationResult;
import com.threadia.backend.directmessage.dto.DirectMessageRoomData;
import com.threadia.backend.directmessage.dto.DirectMessageRoomsData;
import com.threadia.backend.directmessage.dto.DirectMessageSendRequest;
import com.threadia.backend.directmessage.dto.DirectMessageUnreadCountData;
import com.threadia.backend.directmessage.dto.DirectMessagesData;
import com.threadia.backend.directmessage.service.DirectMessageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class DirectMessageController {
	private static final String BEARER_PREFIX = "Bearer ";
	private final DirectMessageService directMessageService;
	public DirectMessageController(DirectMessageService directMessageService) { this.directMessageService = directMessageService; }
	@GetMapping("/companies/{companyId}/direct-messages/rooms")
	public ResponseEntity<ApiResponse<DirectMessageRoomsData>> listRooms(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long companyId) {
		return ResponseEntity.ok(ApiResponse.of(directMessageService.listRooms(token(authorization), companyId)));
	}
	@PostMapping("/companies/{companyId}/direct-messages/rooms")
	public ResponseEntity<ApiResponse<DirectMessageRoomData>> getOrCreateRoom(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long companyId, @Valid @RequestBody DirectMessageRoomCreateRequest request) {
		DirectMessageRoomCreationResult result = directMessageService.getOrCreateRoom(token(authorization), companyId, request);
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(ApiResponse.of(result.data()));
	}
	@GetMapping("/direct-messages/rooms/{roomId}/messages")
	public ResponseEntity<ApiResponse<DirectMessagesData>> listMessages(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long roomId) {
		return ResponseEntity.ok(ApiResponse.of(directMessageService.listMessages(token(authorization), roomId)));
	}
	@PostMapping("/direct-messages/rooms/{roomId}/messages")
	public ResponseEntity<ApiResponse<DirectMessageData>> send(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long roomId, @Valid @RequestBody DirectMessageSendRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(directMessageService.send(token(authorization), roomId, request)));
	}
	@PostMapping("/direct-messages/rooms/{roomId}/mark-read")
	public ResponseEntity<ApiResponse<DirectMessageReadData>> markRead(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long roomId, @Valid @RequestBody DirectMessageMarkReadRequest request) {
		return ResponseEntity.ok(ApiResponse.of(directMessageService.markRead(token(authorization), roomId, request)));
	}
	@GetMapping("/direct-messages/unread-count")
	public ResponseEntity<ApiResponse<DirectMessageUnreadCountData>> unreadCount(@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(ApiResponse.of(directMessageService.unreadCount(token(authorization))));
	}
	private static String token(String authorization) {
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		return authorization.substring(BEARER_PREFIX.length());
	}
}
