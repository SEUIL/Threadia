package com.threadia.backend.storage;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "threadia.storage.s3")
public class StorageProperties {

	private String region = "ap-northeast-2";
	private String bucket;
	private String endpoint;
	private String accessKey;
	private String secretKey;
	private boolean pathStyle = false;
	private Duration presignTtl = Duration.ofMinutes(10);

	public String getRegion() { return region; }
	public void setRegion(String region) { this.region = region; }
	public String getBucket() { return bucket; }
	public void setBucket(String bucket) { this.bucket = bucket; }
	public String getEndpoint() { return endpoint; }
	public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
	public String getAccessKey() { return accessKey; }
	public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
	public String getSecretKey() { return secretKey; }
	public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
	public boolean isPathStyle() { return pathStyle; }
	public void setPathStyle(boolean pathStyle) { this.pathStyle = pathStyle; }
	public Duration getPresignTtl() { return presignTtl; }
	public void setPresignTtl(Duration presignTtl) { this.presignTtl = presignTtl; }
}
