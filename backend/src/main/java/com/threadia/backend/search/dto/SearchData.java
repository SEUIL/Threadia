package com.threadia.backend.search.dto;

import java.util.List;

public record SearchData(List<SearchResult> results, String nextCursor) {
}
