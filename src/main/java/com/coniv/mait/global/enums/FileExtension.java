package com.coniv.mait.global.enums;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileExtension {
	JPG(".jpg"), JPEG(".jpeg"), PNG(".png"), SVG(".svg"),
	PDF(".pdf"), MARK_DOWN(".md"), TXT(".txt");

	private final String extension;

	public static FileExtension fromExtension(String ext) {
		String normalizedExt = ext.startsWith(".") ? ext : "." + ext;
		return Arrays.stream(values())
			.filter(fileExtension -> fileExtension.extension.equalsIgnoreCase(normalizedExt))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unsupported file extension: " + ext));
	}
}
