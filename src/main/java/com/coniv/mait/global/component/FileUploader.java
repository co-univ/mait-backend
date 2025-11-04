package com.coniv.mait.global.component;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.coniv.mait.global.component.dto.FileInfo;
import com.coniv.mait.global.enums.FileExtension;
import com.coniv.mait.global.s3.dto.FileType;

public interface FileUploader {

	FileInfo uploadFile(MultipartFile file, FileType type);

	static String generateKey(String directory, FileExtension extension) {
		return directory + "/" + UUID.randomUUID() + extension.getExtension();
	}
}
