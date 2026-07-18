package com.threadia.backend.supportcase.domain;

public enum SupportCaseStatus {
	OPEN,
	IN_PROGRESS,
	RESOLVED,
	REJECTED,
	CLOSED;

	public boolean canTransitionTo(SupportCaseStatus next) {
		return switch (this) {
			case OPEN -> next == IN_PROGRESS || next == REJECTED || next == CLOSED;
			case IN_PROGRESS -> next == RESOLVED || next == REJECTED || next == CLOSED;
			case RESOLVED, REJECTED -> next == CLOSED;
			case CLOSED -> false;
		};
	}
}
