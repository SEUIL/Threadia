package com.threadia.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

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

	@Test
	void defaultS3ModeRejectsBlankBucket() {
		StorageProperties properties = productionProperties();
		properties.setBucket(" ");

		assertThatThrownBy(() -> S3StorageConfig.validateProductionConfiguration(properties))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("bucket");
	}

	@Test
	void defaultS3ModeRejectsBlankRegion() {
		StorageProperties properties = productionProperties();
		properties.setRegion(" ");

		assertThatThrownBy(() -> S3StorageConfig.validateProductionConfiguration(properties))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("region");
	}

	@Test
	void defaultS3ModeRejectsNonProductionRegionAndNonDedicatedBucket() {
		StorageProperties properties = productionProperties();
		properties.setRegion("us-east-1");

		assertThatThrownBy(() -> S3StorageConfig.validateProductionConfiguration(properties))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("ap-northeast-2");

		properties.setRegion("ap-northeast-2");
		properties.setBucket("threadia-private");

		assertThatThrownBy(() -> S3StorageConfig.validateProductionConfiguration(properties))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("attachment bucket");
	}

	@Test
	void defaultS3ModeRejectsMinioOnlyConfiguration() {
		StorageProperties properties = productionProperties();
		properties.setEndpoint("http://localhost:9000");

		assertThatThrownBy(() -> S3StorageConfig.validateProductionConfiguration(properties))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("endpoint override");

		properties.setEndpoint(null);
		properties.setPathStyle(true);

		assertThatThrownBy(() -> S3StorageConfig.validateProductionConfiguration(properties))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("path-style");

		properties.setPathStyle(false);
		properties.setAccessKey("local-access-key");

		assertThatThrownBy(() -> S3StorageConfig.validateProductionConfiguration(properties))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("static credential");
	}

	@Test
	void defaultS3ModeAllowsPresignTtlAtConfiguredTenMinuteMaximum() {
		StorageProperties properties = productionProperties();
		properties.setPresignTtl(Duration.ofMinutes(10));

		S3StorageConfig.validateProductionConfiguration(properties);
	}

	@Test
	void defaultS3ModeRejectsNonPositiveAndOverlongPresignTtl() {
		StorageProperties properties = productionProperties();
		properties.setPresignTtl(Duration.ZERO);

		assertThatThrownBy(() -> S3StorageConfig.validateProductionConfiguration(properties))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("양수");

		properties.setPresignTtl(Duration.ofMinutes(10).plusSeconds(1));

		assertThatThrownBy(() -> S3StorageConfig.validateProductionConfiguration(properties))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("10분");
	}

	@Test
	void localProfileAllowsMinioConfiguration() {
		StorageProperties properties = new StorageProperties();
		properties.setEndpoint("http://localhost:9000");
		properties.setPathStyle(true);
		properties.setAccessKey("local-access-key");
		properties.setSecretKey("local-secret-key");
		properties.setPresignTtl(Duration.ofHours(1));

		S3StorageConfig.validateProductionConfiguration(properties, "local");
	}

	private StorageProperties productionProperties() {
		StorageProperties properties = new StorageProperties();
		properties.setBucket("threadia-attachments-940507692158-ap-northeast-2");
		properties.setRegion("ap-northeast-2");
		return properties;
	}
}
