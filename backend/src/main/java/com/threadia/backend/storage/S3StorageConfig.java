package com.threadia.backend.storage;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class S3StorageConfig {
	private static final String PRODUCTION_REGION = "ap-northeast-2";
	private static final String PRODUCTION_BUCKET_PATTERN = "threadia-attachments-\\d{12}-ap-northeast-2";
	private static final Duration MAX_PRODUCTION_PRESIGN_TTL = Duration.ofMinutes(10);

	@Bean
	S3Client s3Client(StorageProperties properties, Environment environment) {
		validateProductionConfiguration(properties, environment.getActiveProfiles());
		S3ClientBuilder builder = S3Client.builder().region(Region.of(properties.getRegion()))
				.credentialsProvider(credentialsProvider(properties))
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(properties.isPathStyle()).build());
		if (StringUtils.hasText(properties.getEndpoint())) builder.endpointOverride(URI.create(properties.getEndpoint()));
		return builder.build();
	}

	@Bean
	S3Presigner s3Presigner(StorageProperties properties, Environment environment) {
		validateProductionConfiguration(properties, environment.getActiveProfiles());
		S3Presigner.Builder builder = S3Presigner.builder().region(Region.of(properties.getRegion()))
				.credentialsProvider(credentialsProvider(properties))
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(properties.isPathStyle()).build());
		if (StringUtils.hasText(properties.getEndpoint())) builder.endpointOverride(URI.create(properties.getEndpoint()));
		return builder.build();
	}

	AwsCredentialsProvider credentialsProvider(StorageProperties properties) {
		if (StringUtils.hasText(properties.getAccessKey()) && StringUtils.hasText(properties.getSecretKey())) {
			return StaticCredentialsProvider.create(AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
		}
		return DefaultCredentialsProvider.builder().build();
	}

	static void validateProductionConfiguration(StorageProperties properties, String... activeProfiles) {
		if (Arrays.stream(activeProfiles).anyMatch("local"::equalsIgnoreCase)) {
			return;
		}
		if (!StringUtils.hasText(properties.getBucket())) {
			throw new IllegalStateException("production S3 storage에는 bucket 설정이 필요합니다.");
		}
		if (!StringUtils.hasText(properties.getRegion())) {
			throw new IllegalStateException("production S3 storage에는 region 설정이 필요합니다.");
		}
		if (!PRODUCTION_REGION.equals(properties.getRegion())) {
			throw new IllegalStateException("production S3 storage는 ap-northeast-2 region만 사용할 수 있습니다.");
		}
		if (!properties.getBucket().matches(PRODUCTION_BUCKET_PATTERN)) {
			throw new IllegalStateException("production S3 storage는 전용 attachment bucket만 사용할 수 있습니다.");
		}
		if (properties.isPathStyle()) {
			throw new IllegalStateException("production S3 storage에서는 path-style access를 사용할 수 없습니다.");
		}
		if (StringUtils.hasText(properties.getEndpoint())) {
			throw new IllegalStateException("production S3 storage에서는 endpoint override를 사용할 수 없습니다.");
		}
		if (StringUtils.hasText(properties.getAccessKey()) || StringUtils.hasText(properties.getSecretKey())) {
			throw new IllegalStateException("production S3 storage에서는 static credential을 사용할 수 없습니다.");
		}
		if (properties.getPresignTtl() == null || properties.getPresignTtl().isZero() || properties.getPresignTtl().isNegative()) {
			throw new IllegalStateException("production S3 storage의 presign TTL은 양수여야 합니다.");
		}
		if (properties.getPresignTtl().compareTo(MAX_PRODUCTION_PRESIGN_TTL) > 0) {
			throw new IllegalStateException("production S3 storage의 presign TTL은 10분을 초과할 수 없습니다.");
		}
	}
}
