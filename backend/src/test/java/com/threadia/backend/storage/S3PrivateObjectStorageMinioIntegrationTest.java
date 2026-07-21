package com.threadia.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Testcontainers(disabledWithoutDocker = true)
class S3PrivateObjectStorageMinioIntegrationTest {
	private static final String ACCESS_KEY = "threadia-test-access";
	private static final String SECRET_KEY = "threadia-test-secret";
	private static final String BUCKET = "threadia-private-test";

	@Container
	static final GenericContainer<?> minio = new GenericContainer<>("minio/minio:RELEASE.2025-04-22T22-12-26Z")
			.withEnv("MINIO_ROOT_USER", ACCESS_KEY)
			.withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
			.withCommand("server", "/data")
			.withExposedPorts(9000);

	private static S3Client client;
	private static S3Presigner presigner;
	private static S3PrivateObjectStorage storage;

	@BeforeAll
	static void setUp() {
		URI endpoint = URI.create("http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
		StaticCredentialsProvider credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY));
		S3Configuration s3Configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build();
		client = S3Client.builder().endpointOverride(endpoint).region(Region.US_EAST_1).credentialsProvider(credentials).serviceConfiguration(s3Configuration).build();
		presigner = S3Presigner.builder().endpointOverride(endpoint).region(Region.US_EAST_1).credentialsProvider(credentials).serviceConfiguration(s3Configuration).build();
		client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
		StorageProperties properties = new StorageProperties();
		properties.setBucket(BUCKET);
		properties.setRegion("us-east-1");
		properties.setEndpoint(endpoint.toString());
		properties.setPathStyle(true);
		properties.setPresignTtl(Duration.ofMinutes(2));
		storage = new S3PrivateObjectStorage(client, presigner, properties);
	}

	@AfterAll
	static void tearDown() {
		if (presigner != null) presigner.close();
		if (client != null) client.close();
	}

	@Test
	void presignedUploadHeadVerificationAndPrivateDownloadUseS3CompatibleMinio() throws Exception {
		byte[] payload = "private attachment payload".getBytes(StandardCharsets.UTF_8);
		String checksum = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(payload));
		String key = "companies/1/attachments/document/test-object";

		HttpRequest upload = HttpRequest.newBuilder(storage.presignUpload(key, "text/plain", checksum))
				.header("Content-Type", "text/plain")
				.header("x-amz-checksum-sha256", checksum)
				.PUT(HttpRequest.BodyPublishers.ofByteArray(payload)).build();
		HttpResponse<Void> uploadResponse = HttpClient.newHttpClient().send(upload, HttpResponse.BodyHandlers.discarding());
		assertThat(uploadResponse.statusCode()).isBetween(200, 299);

		PrivateObjectStorage.ObjectMetadata metadata = storage.head(key);
		assertThat(metadata.size()).isEqualTo(payload.length);
		assertThat(metadata.contentType()).isEqualTo("text/plain");
		assertThat(metadata.checksumSha256()).isEqualTo(checksum);

		HttpResponse<byte[]> downloadResponse = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(storage.presignDownload(key, "attachment; filename=\"private.txt\"")).GET().build(),
				HttpResponse.BodyHandlers.ofByteArray());
		assertThat(downloadResponse.statusCode()).isEqualTo(200);
		assertThat(downloadResponse.body()).isEqualTo(payload);
	}

	@Test
	void presignedUploadRejectsPayloadWhoseBytesDoNotMatchSignedChecksum() throws Exception {
		byte[] payload = "tampered payload".getBytes(StandardCharsets.UTF_8);
		String signedChecksum = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256")
				.digest("expected payload".getBytes(StandardCharsets.UTF_8)));
		String key = "companies/2/attachments/document/checksum-mismatch";

		HttpResponse<Void> response = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(storage.presignUpload(key, "text/plain", signedChecksum))
						.header("Content-Type", "text/plain")
						.header("x-amz-checksum-sha256", signedChecksum)
						.PUT(HttpRequest.BodyPublishers.ofByteArray(payload)).build(),
				HttpResponse.BodyHandlers.discarding());

		assertThat(response.statusCode() < 200 || response.statusCode() > 299).isTrue();
		assertThatThrownBy(() -> storage.head(key)).isInstanceOf(S3Exception.class);
	}
}
