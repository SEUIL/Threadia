package com.threadia.backend.platform.audit;

import java.util.List;

public record AuditPageData(List<AuditResponse> audits, String nextCursor, boolean hasNext, int size) {
}
