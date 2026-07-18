package com.threadia.backend.platform.settings;

public class SystemSettingNotFoundException extends RuntimeException {
	public SystemSettingNotFoundException(SystemSettingKey key) {
		super("시스템 설정을 찾을 수 없습니다: " + key);
	}
}
