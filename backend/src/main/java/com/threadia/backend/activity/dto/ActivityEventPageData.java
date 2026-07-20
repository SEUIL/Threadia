package com.threadia.backend.activity.dto;

import java.util.List;

public record ActivityEventPageData(List<ActivityEventResponse> events, int page, int size, long total) {
}
