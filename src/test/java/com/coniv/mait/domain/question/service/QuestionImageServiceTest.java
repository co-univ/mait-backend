package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.coniv.mait.domain.question.entity.QuestionImageEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionImageEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.s3.service.S3FileUploader;

@ExtendWith(MockitoExtension.class)
class QuestionImageServiceTest {

	private static final Long USER_ID = 1L;

	private static final Long TEAM_ID = 10L;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@InjectMocks
	private QuestionImageService questionImageService;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private S3FileUploader imageUploader;

	@Mock
	private QuestionImageEntityRepository questionImageEntityRepository;

	@Test
	@DisplayName("문제 사진 업데이트 - 성공")
	void unUseImage_Success() throws ExecutionException, InterruptedException {
		// Given
		final Long questionImageId = 1L;

		QuestionImageEntity existingImage = mock(QuestionImageEntity.class);
		when(questionImageEntityRepository.findById(questionImageId)).thenReturn(Optional.of(existingImage));

		// When
		CompletableFuture<Void> future = questionImageService.unUseExistImage(questionImageId);
		future.get();

		// Then
		verify(existingImage).updateUsage(false);
	}

	@Test
	@DisplayName("문제 셋 팀의 MAKER 이상이 아니면 문제 이미지를 업로드할 수 없다")
	void uploadImage_NotTeamManager_ThrowsException() {
		// given
		Long questionSetId = 1L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getTeamId()).thenReturn(TEAM_ID);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		doThrow(new UserRoleException("팀 관리 권한이 없습니다."))
			.when(teamRoleValidator).checkIsTeamManager(TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() -> questionImageService.uploadImage(questionSetId, mock(MultipartFile.class), USER_ID))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("팀 관리 권한이 없습니다.");

		verify(imageUploader, never()).uploadFile(any(), any());
		verify(questionImageEntityRepository, never()).save(any());
	}
}
