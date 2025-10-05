package com.coniv.mait.global.component.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageInfo {

	private String url;

	private String key;

	private String bucket;
}
