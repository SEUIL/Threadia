package com.threadia.backend.supportcase.dto;

import java.util.List;

public record SupportCaseDetailData(
		SupportCaseResponse supportCase,
		List<PublicReplyResponse> publicReplies) {
}
