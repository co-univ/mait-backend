package com.coniv.mait.global.component.dto;

import com.coniv.mait.global.enums.FileExtension;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileInfo {

	private String url;

	private String key;

	private String bucket;

	private FileExtension extension;
}
