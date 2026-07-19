package com.threadia.backend.notification.service;

import com.threadia.backend.notification.domain.NotificationType;

record NotificationFilters(
		Long companyId,
		Long projectId,
		NotificationType type,
		boolean unreadOnly) {

	String companyBinding() { return companyId == null ? "-" : companyId.toString(); }
	String projectBinding() { return projectId == null ? "-" : projectId.toString(); }
	String typeBinding() { return type == null ? "-" : type.name(); }
	String unreadBinding() { return unreadOnly ? "1" : "0"; }
}
