package com.threadia.backend.common;

// signup 시 이미 가입된(정규화된) email로 다시 가입을 시도할 때 던진다. 409 CONFLICT로 매핑된다.
public class EmailAlreadyExistsException extends RuntimeException {

	public EmailAlreadyExistsException(String email) {
		super("이미 가입된 이메일입니다: " + email);
	}
}
