package com.threadia.backend.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

// PATCH 요청에서 필드 생략과 avatarUrl: null을 구분한다.
public class ProfileUpdateRequest {

	@Size(min = 2, max = 100)
	private String nickname;

	@Size(max = 500)
	private String avatarUrl;

	@JsonIgnore
	private boolean nicknameProvided;

	@JsonIgnore
	private boolean avatarUrlProvided;

	public String getNickname() {
		return nickname;
	}

	@JsonSetter("nickname")
	public void setNickname(String nickname) {
		this.nicknameProvided = true;
		this.nickname = nickname;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	@JsonSetter("avatarUrl")
	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrlProvided = true;
		this.avatarUrl = avatarUrl;
	}

	@JsonAnySetter
	public void rejectUnknownProperty(String propertyName, Object value) {
		throw new IllegalArgumentException("지원하지 않는 profile 필드입니다: " + propertyName);
	}

	@JsonIgnore
	public boolean hasNickname() {
		return nicknameProvided;
	}

	@JsonIgnore
	public boolean hasAvatarUrl() {
		return avatarUrlProvided;
	}

	@AssertTrue(message = "nickname은 null일 수 없습니다.")
	@JsonIgnore
	public boolean isNicknameValid() {
		return !nicknameProvided || (nickname != null && !nickname.isBlank());
	}

	@AssertTrue(message = "nickname 또는 avatarUrl 중 하나는 필요합니다.")
	@JsonIgnore
	public boolean hasProfileField() {
		return nicknameProvided || avatarUrlProvided;
	}
}
