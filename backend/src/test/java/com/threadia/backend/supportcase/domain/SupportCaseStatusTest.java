package com.threadia.backend.supportcase.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SupportCaseStatusTest {

	@ParameterizedTest(name = "{0} -> {1} allowed={2}")
	@MethodSource("allTransitions")
	void enforcesApprovedTransitionTable(
			SupportCaseStatus from,
			SupportCaseStatus to,
			boolean allowed) {
		assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
	}

	private static Stream<Arguments> allTransitions() {
		return Arrays.stream(SupportCaseStatus.values())
				.flatMap(from -> Arrays.stream(SupportCaseStatus.values())
						.map(to -> Arguments.of(from, to, allowedTargets(from).contains(to))));
	}

	private static Set<SupportCaseStatus> allowedTargets(SupportCaseStatus from) {
		return switch (from) {
			case OPEN -> Set.of(
					SupportCaseStatus.IN_PROGRESS,
					SupportCaseStatus.REJECTED,
					SupportCaseStatus.CLOSED);
			case IN_PROGRESS -> Set.of(
					SupportCaseStatus.RESOLVED,
					SupportCaseStatus.REJECTED,
					SupportCaseStatus.CLOSED);
			case RESOLVED, REJECTED -> Set.of(SupportCaseStatus.CLOSED);
			case CLOSED -> Set.of();
		};
	}
}
