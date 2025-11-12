package com.coniv.mait.global.util;

import org.springframework.util.StringUtils;

import com.coniv.mait.global.enums.FileExtension;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtil {

	public static FileExtension getFileExtension(String fileName) {
		if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
			throw new IllegalArgumentException("Invalid file name: " + fileName);
		}
		String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
		return FileExtension.fromExtension(extension);
	}
}
