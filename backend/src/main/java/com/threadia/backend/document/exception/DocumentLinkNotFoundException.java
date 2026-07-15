package com.threadia.backend.document.exception;
public class DocumentLinkNotFoundException extends RuntimeException { public DocumentLinkNotFoundException(Long id) { super("문서-이슈 연결을 찾을 수 없습니다: " + id); } }
