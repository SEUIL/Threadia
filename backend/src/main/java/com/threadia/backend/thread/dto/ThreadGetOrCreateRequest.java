package com.threadia.backend.thread.dto;

import com.threadia.backend.thread.domain.ThreadTargetType;

import jakarta.validation.constraints.NotNull;

public record ThreadGetOrCreateRequest(@NotNull ThreadTargetType targetType, @NotNull Long targetId, String topic) { }
