package com.coniv.mait.global.component;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.coniv.mait.global.component.dto.ImageInfo;
import com.coniv.mait.global.enums.FileExtension;

public interface ImageUploader {

	ImageInfo uploadImage(MultipartFile file, String directory);

	static String generateKey(String directory, FileExtension extension) {
		return directory + "/" + UUID.randomUUID() + extension.getExtension();
	}
}
