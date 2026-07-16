package com.threadia.backend.directmessage.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DirectMessageRoomCreateRequest(@NotNull @Positive Long otherUserId) { }
