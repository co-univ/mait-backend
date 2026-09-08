package com.coniv.mait.domain.question.service.component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionImageEntity;
import com.coniv.mait.domain.question.repository.QuestionImageEntityRepository;
import com.coniv.mait.global.component.FileUploader;
import com.coniv.mait.global.component.dto.FileInfo;
import com.coniv.mait.global.s3.dto.FileType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuestionImageCopier {

	private final QuestionImageEntityRepository questionImageEntityRepository;
	private final FileUploader fileUploader;

	public Map<Long, QuestionImageEntity> copyAll(final Collection<Long> sourceImageIds) {
		if (sourceImageIds.isEmpty()) {
			return Map.of();
		}

		List<QuestionImageEntity> sources = questionImageEntityRepository.findAllById(sourceImageIds);

		Map<Long, QuestionImageEntity> copiedBySourceImageId = sources.stream()
			.collect(Collectors.toMap(QuestionImageEntity::getId, this::copy));

		questionImageEntityRepository.saveAll(copiedBySourceImageId.values());

		return copiedBySourceImageId;
	}

	private QuestionImageEntity copy(final QuestionImageEntity source) {
		FileInfo copied = fileUploader.copyFile(source.getFileKey(), FileType.QUESTION_IMAGE);

		return QuestionImageEntity.builder()
			.fileKey(copied.getKey())
			.url(copied.getUrl())
			.bucket(copied.getBucket())
			.build();
	}
}
