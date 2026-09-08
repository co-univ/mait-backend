package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionImageEntity;
import com.coniv.mait.domain.question.repository.QuestionImageEntityRepository;
import com.coniv.mait.global.component.FileUploader;
import com.coniv.mait.global.component.dto.FileInfo;
import com.coniv.mait.global.enums.FileExtension;
import com.coniv.mait.global.s3.dto.FileType;

@ExtendWith(MockitoExtension.class)
class QuestionImageCopierTest {

	@Mock
	private QuestionImageEntityRepository questionImageEntityRepository;

	@Mock
	private FileUploader fileUploader;

	@InjectMocks
	private QuestionImageCopier questionImageCopier;

	@Test
	@DisplayName("이미지 복제 - 복제할 이미지가 없으면 조회도 복사도 하지 않는다")
	void copyAll_emptyIds_doesNothing() {
		// when
		Map<Long, QuestionImageEntity> result = questionImageCopier.copyAll(List.of());

		// then
		assertThat(result).isEmpty();
		verifyNoInteractions(questionImageEntityRepository, fileUploader);
	}

	@Test
	@DisplayName("이미지 복제 - 원본 파일 키로 S3 복사를 요청하고 신규 엔티티를 저장한다")
	void copyAll_copiesObjectAndSavesNewEntity() {
		// given
		QuestionImageEntity source = image(1L, "questions/origin.png");
		doReturn(List.of(source)).when(questionImageEntityRepository).findAllById(List.of(1L));
		doReturn(fileInfo("questions/copied.png")).when(fileUploader)
			.copyFile("questions/origin.png", FileType.QUESTION_IMAGE);

		// when
		Map<Long, QuestionImageEntity> result = questionImageCopier.copyAll(List.of(1L));

		// then
		assertThat(result).containsOnlyKeys(1L);
		QuestionImageEntity copied = result.get(1L);
		assertThat(copied.getFileKey()).isEqualTo("questions/copied.png");
		assertThat(copied.getUrl()).isEqualTo("https://bucket/questions/copied.png");
		assertThat(copied.getBucket()).isEqualTo("mait-bucket");
		assertThat(copied.isUsed()).isTrue();

		ArgumentCaptor<Collection<QuestionImageEntity>> savedCaptor = ArgumentCaptor.captor();
		verify(questionImageEntityRepository).saveAll(savedCaptor.capture());
		assertThat(savedCaptor.getValue()).containsExactly(copied);
	}

	@Test
	@DisplayName("이미지 복제 - 원본 엔티티는 그대로 두고 복제본만 새로 만든다")
	void copyAll_doesNotModifySource() {
		// given
		QuestionImageEntity source = image(1L, "questions/origin.png");
		doReturn(List.of(source)).when(questionImageEntityRepository).findAllById(List.of(1L));
		doReturn(fileInfo("questions/copied.png")).when(fileUploader).copyFile(anyString(), any(FileType.class));

		// when
		QuestionImageEntity copied = questionImageCopier.copyAll(List.of(1L)).get(1L);

		// then
		assertThat(source.getFileKey()).isEqualTo("questions/origin.png");
		assertThat(source.isUsed()).isTrue();
		assertThat(copied).isNotSameAs(source);
	}

	@Test
	@DisplayName("이미지 복제 - 여러 이미지를 원본 id 기준으로 매핑한다")
	void copyAll_multipleImages_mappedBySourceId() {
		// given
		doReturn(List.of(image(1L, "questions/a.png"), image(2L, "questions/b.png")))
			.when(questionImageEntityRepository).findAllById(List.of(1L, 2L));
		doReturn(fileInfo("questions/a-copied.png")).when(fileUploader)
			.copyFile("questions/a.png", FileType.QUESTION_IMAGE);
		doReturn(fileInfo("questions/b-copied.png")).when(fileUploader)
			.copyFile("questions/b.png", FileType.QUESTION_IMAGE);

		// when
		Map<Long, QuestionImageEntity> result = questionImageCopier.copyAll(List.of(1L, 2L));

		// then
		assertThat(result).containsOnlyKeys(1L, 2L);
		assertThat(result.get(1L).getFileKey()).isEqualTo("questions/a-copied.png");
		assertThat(result.get(2L).getFileKey()).isEqualTo("questions/b-copied.png");
	}

	private QuestionImageEntity image(final Long id, final String fileKey) {
		return QuestionImageEntity.builder()
			.id(id)
			.fileKey(fileKey)
			.url("https://bucket/" + fileKey)
			.bucket("mait-bucket")
			.build();
	}

	private FileInfo fileInfo(final String key) {
		return FileInfo.builder()
			.key(key)
			.url("https://bucket/" + key)
			.bucket("mait-bucket")
			.extension(FileExtension.PNG)
			.build();
	}
}
