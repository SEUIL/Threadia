package com.threadia.backend.attachment.dto;

import java.net.URI;

public record AttachmentUploadData(AttachmentResponse attachment, URI uploadUrl) { }
