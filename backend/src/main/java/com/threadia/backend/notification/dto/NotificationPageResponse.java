package com.threadia.backend.notification.dto;

import java.util.List;
import java.util.UUID;

import com.threadia.backend.common.response.Meta;

public record NotificationPageResponse(
		List<NotificationResponse> data,
		NotificationPage page,
		Meta meta) {

	public static NotificationPageResponse from(NotificationPageData pageData) {
		return new NotificationPageResponse(
				pageData.notifications(),
				new NotificationPage(pageData.nextCursor(), pageData.hasNext(), pageData.size()),
				new Meta("req_" + UUID.randomUUID()));
	}
}
