package com.threadia.backend.storage;

import java.net.URI;

public interface PrivateObjectStorage {
	URI presignUpload(String key, String contentType, String checksumSha256);
	URI presignDownload(String key, String responseContentDisposition);
	ObjectMetadata head(String key);
	void delete(String key);

	record ObjectMetadata(long size, String contentType, String checksumSha256) { }
}
