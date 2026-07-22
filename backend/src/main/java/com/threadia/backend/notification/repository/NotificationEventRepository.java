package com.threadia.backend.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.notification.domain.NotificationEvent;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {
}
