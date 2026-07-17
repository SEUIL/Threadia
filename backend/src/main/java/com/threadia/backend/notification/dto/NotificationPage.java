package com.threadia.backend.notification.dto;

public record NotificationPage(String cursor, boolean hasNext, int size) {
}
