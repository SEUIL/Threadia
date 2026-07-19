package com.threadia.backend.notification.dto;

public record NotificationReadAllRequest(String type, Long companyId, Long projectId) {
}
