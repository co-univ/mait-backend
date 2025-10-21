package com.coniv.mait.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseImageEntity extends BaseTimeEntity {

	@Column(updatable = false)
	private String url;

	@Column(name = "image_key", updatable = false)
	private String imageKey;

	@Column(updatable = false)
	private String bucket;

	@Builder.Default
	private boolean used = true;

	public void updateUsage(boolean used) {
		this.used = used;
	}
}
