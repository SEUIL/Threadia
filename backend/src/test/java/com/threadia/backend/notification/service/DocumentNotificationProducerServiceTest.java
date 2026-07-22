package com.threadia.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.thread.service.ThreadService;

class DocumentNotificationProducerServiceTest {

	@Test
	void producerRequiresCallerTransactionAndDoesNotExposeThreadNotificationPath() throws Exception {
		Method documentUpdated = DocumentNotificationProducerService.class.getDeclaredMethod(
				"documentUpdated",
				com.threadia.backend.document.domain.Document.class,
				com.threadia.backend.auth.domain.User.class,
				java.time.LocalDateTime.class);

		assertThat(documentUpdated.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.MANDATORY);
		assertThat(Arrays.stream(DocumentNotificationProducerService.class.getDeclaredMethods())
				.filter(method -> Modifier.isPublic(method.getModifiers()))
				.map(Method::getName))
				.containsExactly("documentUpdated");
		assertThat(Arrays.stream(ThreadService.class.getDeclaredFields())
				.map(field -> field.getType().getName()))
				.noneMatch(typeName -> typeName.contains("notification"));
	}
}
