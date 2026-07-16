package com.threadia.backend.projectchat.dto;

import java.util.List;

public record MessagesData(List<MessageResponse> messages, String nextCursor) {
}
