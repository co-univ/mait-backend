package com.coniv.mait.global.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileExtension {
	JPG(".jpg"), JPEG(".jpeg"), PNG(".png"), SVG(".svg"),
	PDF(".pdf"), MARK_DOWN(".md"), TXT(".txt");

	private final String extension;
}
