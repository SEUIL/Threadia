package com.threadia.backend.attachment.exception;

public class AttachmentNotFoundException extends RuntimeException {
	public AttachmentNotFoundException(Long attachmentId) { super("첨부 파일을 찾을 수 없습니다: " + attachmentId); }
}
