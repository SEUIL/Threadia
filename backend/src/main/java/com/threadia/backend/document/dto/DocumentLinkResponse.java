package com.threadia.backend.document.dto;
import com.threadia.backend.document.domain.DocumentIssueLink;
public record DocumentLinkResponse(Long id, Long projectId, Long documentId, Long issueId, Long createdByUserId) { public static DocumentLinkResponse from(DocumentIssueLink link) { return new DocumentLinkResponse(link.getId(), link.getProject().getId(), link.getDocument().getId(), link.getIssue().getId(), link.getCreatedBy().getId()); } }
