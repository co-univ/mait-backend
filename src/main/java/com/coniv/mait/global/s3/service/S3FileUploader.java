package com.coniv.mait.global.s3.service;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.coniv.mait.global.component.FileUploader;
import com.coniv.mait.global.component.dto.FileInfo;
import com.coniv.mait.global.config.property.S3Property;
import com.coniv.mait.global.enums.FileExtension;
import com.coniv.mait.global.exception.code.S3ExceptionCode;
import com.coniv.mait.global.exception.custom.S3FileException;
import com.coniv.mait.global.s3.dto.FileType;
import com.coniv.mait.global.util.FileUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3FileUploader implements FileUploader {

	private final S3Property s3Property;

	private final S3Client s3Client;

	@Override
	public FileInfo uploadFile(final MultipartFile file, final FileType type) {
		final String originalFilename = file.getOriginalFilename();
		final FileExtension extension = FileUtil.getFileExtension(originalFilename);

		validateFileExtensions(type, extension);

		final String key = FileUploader.generateKey(type.getDirectory(), extension);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(s3Property.getBucket())
			.key(key)
			.contentType(file.getContentType())
			.contentLength(file.getSize())
			.build();

		putObject(file, putObjectRequest);

		return FileInfo.builder()
			.bucket(s3Property.getBucket())
			.key(key)
			.url(toUrl(key))
			.extension(extension)
			.build();
	}

	@Override
	public FileInfo copyFile(final String sourceKey, final FileType type) {
		final FileExtension extension = FileUtil.getFileExtension(sourceKey);
		final String destinationKey = FileUploader.generateKey(type.getDirectory(), extension);

		copyObject(sourceKey, destinationKey);

		return FileInfo.builder()
			.bucket(s3Property.getBucket())
			.key(destinationKey)
			.url(toUrl(destinationKey))
			.extension(extension)
			.build();
	}

	private void copyObject(final String sourceKey, final String destinationKey) {
		try {
			s3Client.copyObject(CopyObjectRequest.builder()
				.sourceBucket(s3Property.getBucket())
				.sourceKey(sourceKey)
				.destinationBucket(s3Property.getBucket())
				.destinationKey(destinationKey)
				.build());
		} catch (SdkException exception) {
			log.error("[S3 복사 과정에서의 에러] bucket: {}, sourceKey: {}, destinationKey: {}",
				s3Property.getBucket(), sourceKey, destinationKey, exception);
			throw new S3FileException(S3ExceptionCode.COPY, s3Property.getBucket(), sourceKey);
		}
	}

	private String toUrl(final String key) {
		return s3Client.utilities()
			.getUrl(GetUrlRequest.builder()
				.bucket(s3Property.getBucket())
				.key(key)
				.build()
			).toExternalForm();
	}

	private void validateFileExtensions(FileType type, FileExtension extension) {
		if (!type.isSupportedExtension(extension)) {
			throw new S3FileException(S3ExceptionCode.INVALID_TYPE, type.name(), extension.getExtension());
		}
	}

	private void putObject(MultipartFile file, PutObjectRequest putObjectRequest) {
		try {
			s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
		} catch (IOException e) {
			log.error("[S3 업로드 과정에서의 에러] bucket: {}, key: {}", putObjectRequest.bucket(), putObjectRequest.key());
			throw new S3FileException(S3ExceptionCode.PUT, putObjectRequest.bucket(), putObjectRequest.key());
		}
	}
}
