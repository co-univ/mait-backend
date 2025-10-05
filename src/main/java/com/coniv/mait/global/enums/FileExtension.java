package com.coniv.mait.global.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileExtension {
	JPG(".jpg"), JPEG(".jpeg"), PNG(".png"), SVG(".svg");

	private final String extension;
}
