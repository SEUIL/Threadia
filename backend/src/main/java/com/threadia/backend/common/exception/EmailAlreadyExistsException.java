package com.threadia.backend.common.exception;

/**
 * signup 시 이미 가입된(정규화된) email로 다시 가입을 시도할 때 AuthService가 던진다. 409 CONFLICT로 매핑된다.
 *
 * <p>email은 log 확인용으로만 보관하고 클라이언트 응답 메시지에는 넣지 않는다(getMessage()는 일반 문구로 고정).
 * signup이라는 endpoint 특성상 계정 존재 여부 자체는 이미 노출되지만, 원문 email을 응답에 반복하지 않는 것으로
 * 불필요한 개인정보 echo를 줄인다.
 */
public class EmailAlreadyExistsException extends RuntimeException {

	private final String email;

	public EmailAlreadyExistsException(String email) {
		super("이미 가입된 이메일입니다.");
		this.email = email;
	}

	// 응답 body에는 노출하지 않고, 서버 log 등 내부 진단 용도로만 사용한다.
	public String getEmail() {
		return email;
	}
}
