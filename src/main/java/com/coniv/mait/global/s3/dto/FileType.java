package com.coniv.mait.global.s3.dto;

import java.util.Map;
import java.util.Set;

import com.coniv.mait.global.enums.FileExtension;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileType {
	QUESTION_IMAGE("문제 이미지", "questions"),
	QUESTION_SET_MATERIAL("문제 세트 자료", "questionsets");

	private final String description;
	private final String directory;

	static final Map<FileType, Set<FileExtension>> EXTENSIONS_BY_TYPE = Map.of(
		QUESTION_IMAGE, Set.of(FileExtension.JPG, FileExtension.JPEG, FileExtension.PNG, FileExtension.SVG),
		QUESTION_SET_MATERIAL, Set.of(FileExtension.PDF, FileExtension.MARK_DOWN, FileExtension.TXT)
	);

	public boolean isSupportedExtension(FileExtension extension) {
		return EXTENSIONS_BY_TYPE.get(this).contains(extension);
	}
}
