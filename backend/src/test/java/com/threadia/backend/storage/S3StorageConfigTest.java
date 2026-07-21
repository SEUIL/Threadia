package com.threadia.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

class S3StorageConfigTest {
	private final S3StorageConfig config = new S3StorageConfig();

	@Test
	void usesStaticCredentialsOnlyWhenBothStorageCredentialsAreConfigured() {
		StorageProperties properties = new StorageProperties();
		properties.setAccessKey("local-access-key");
		properties.setSecretKey("local-secret-key");

		AwsCredentialsProvider provider = config.credentialsProvider(properties);

		assertThat(provider).isInstanceOf(StaticCredentialsProvider.class);
		assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("local-access-key");
		assertThat(provider.resolveCredentials().secretAccessKey()).isEqualTo("local-secret-key");
	}

	@Test
	void usesDefaultAwsCredentialChainWhenStorageCredentialsAreIncomplete() {
		StorageProperties properties = new StorageProperties();
		properties.setAccessKey("local-access-key");

		assertThat(config.credentialsProvider(properties)).isInstanceOf(DefaultCredentialsProvider.class);
	}
}
