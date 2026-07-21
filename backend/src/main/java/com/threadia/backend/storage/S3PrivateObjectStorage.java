package com.threadia.backend.storage;

import java.net.URI;
import java.time.Duration;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
public class S3PrivateObjectStorage implements PrivateObjectStorage {
	private final S3Client client;
	private final S3Presigner presigner;
	private final StorageProperties properties;

	public S3PrivateObjectStorage(S3Client client, S3Presigner presigner, StorageProperties properties) {
		this.client = client;
		this.presigner = presigner;
		this.properties = properties;
	}

	@Override
	public URI presignUpload(String key, String contentType, String checksumSha256) {
		PutObjectRequest request = PutObjectRequest.builder().bucket(properties.getBucket()).key(key)
				.contentType(contentType).checksumSHA256(checksumSha256).build();
		return URI.create(presigner.presignPutObject(PutObjectPresignRequest.builder().signatureDuration(properties.getPresignTtl())
				.putObjectRequest(request).build()).url().toString());
	}

	@Override
	public URI presignDownload(String key, String responseContentDisposition) {
		GetObjectRequest request = GetObjectRequest.builder().bucket(properties.getBucket()).key(key)
				.responseContentDisposition(responseContentDisposition).build();
		return URI.create(presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(properties.getPresignTtl())
				.getObjectRequest(request).build()).url().toString());
	}

	@Override
	public ObjectMetadata head(String key) {
		HeadObjectResponse response = client.headObject(HeadObjectRequest.builder().bucket(properties.getBucket()).key(key)
				.checksumMode(ChecksumMode.ENABLED).build());
		return new ObjectMetadata(response.contentLength(), response.contentType(), response.checksumSHA256());
	}

	@Override
	public void delete(String key) {
		client.deleteObject(DeleteObjectRequest.builder().bucket(properties.getBucket()).key(key).build());
	}
}
