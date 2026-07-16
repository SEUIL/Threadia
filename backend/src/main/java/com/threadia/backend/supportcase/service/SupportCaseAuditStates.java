package com.threadia.backend.supportcase.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.threadia.backend.supportcase.domain.SupportCase;
import com.threadia.backend.supportcase.domain.SupportCaseInternalNote;
import com.threadia.backend.supportcase.domain.SupportCasePublicReply;

final class SupportCaseAuditStates {

	private SupportCaseAuditStates() {
	}

	static Map<String, Object> supportCase(SupportCase supportCase) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("id", supportCase.getId());
		state.put("reporterUserId", supportCase.getReporter().getId());
		state.put("type", supportCase.getType());
		state.put("status", supportCase.getStatus());
		state.put("targetType", supportCase.getTargetType());
		state.put("targetId", supportCase.getTargetId());
		state.put("title", supportCase.getTitle());
		state.put("body", supportCase.getBody());
		state.put("createdAt", supportCase.getCreatedAt().toString());
		state.put("updatedAt", supportCase.getUpdatedAt().toString());
		return state;
	}

	static Map<String, Object> status(SupportCase supportCase) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("status", supportCase.getStatus());
		state.put("updatedAt", supportCase.getUpdatedAt().toString());
		return state;
	}

	static Map<String, Object> publicReply(SupportCasePublicReply reply) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("id", reply.getId());
		state.put("supportCaseId", reply.getSupportCase().getId());
		state.put("authorPlatformAdminId", reply.getAuthor().getId());
		state.put("body", reply.getBody());
		state.put("createdAt", reply.getCreatedAt().toString());
		return state;
	}

	static Map<String, Object> internalNote(SupportCaseInternalNote note) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("id", note.getId());
		state.put("supportCaseId", note.getSupportCase().getId());
		state.put("authorPlatformAdminId", note.getAuthor().getId());
		state.put("body", note.getBody());
		state.put("createdAt", note.getCreatedAt().toString());
		return state;
	}
}
