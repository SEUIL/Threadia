package com.threadia.backend.projectchat.dto;

import jakarta.validation.constraints.NotNull;

public record MarkReadRequest(@NotNull Long lastReadMessageId) {
}
