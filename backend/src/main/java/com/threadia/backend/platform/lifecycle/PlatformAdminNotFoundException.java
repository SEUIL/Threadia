package com.threadia.backend.platform.lifecycle;

public class PlatformAdminNotFoundException extends RuntimeException {
	public PlatformAdminNotFoundException(Long platformAdminId) {
		super("플랫폼 관리자를 찾을 수 없습니다: " + platformAdminId);
	}
}
