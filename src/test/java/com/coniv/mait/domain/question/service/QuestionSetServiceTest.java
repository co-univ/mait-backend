package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

@ExtendWith(MockitoExtension.class)
class QuestionSetServiceTest {

	@InjectMocks
	private QuestionSetService questionSetService;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Test
	@DisplayName("문제 셋 생성 테스트")
	void createQuestionSetTest() {
		// given
		String subject = "Sample Subject";
		var creationType = QuestionSetCreationType.MANUAL;

		// when
		QuestionSetDto questionSetDto = questionSetService.createQuestionSet(subject, creationType);

		// then
		assertThat(questionSetDto).isNotNull();
		assertThat(questionSetDto.getSubject()).isEqualTo(subject);
		verify(questionSetEntityRepository).save(any());
	}

	@Test
	@DisplayName("문제 셋 목록 조회 테스트")
	void getQuestionSetsTest() {
		// given
		final Long teamId = 1L;
		final LocalDateTime now = LocalDateTime.now();
		QuestionSetEntity older = mock(QuestionSetEntity.class);
		QuestionSetEntity newer = mock(QuestionSetEntity.class);

		when(older.getId()).thenReturn(1L);
		when(newer.getId()).thenReturn(2L);
		when(older.getCreatedAt()).thenReturn(now.minusDays(1));
		when(newer.getCreatedAt()).thenReturn(now.plusDays(1));

		when(questionSetEntityRepository.findAllByTeamId(teamId))
			.thenReturn(List.of(older, newer));

		// when
		List<QuestionSetDto> result = questionSetService.getQuestionSets(teamId);

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getId()).isEqualTo(2L); // 최신 것이 먼저
		assertThat(result.get(1).getId()).isEqualTo(1L);

		verify(questionSetEntityRepository, times(1)).findAllByTeamId(teamId);
	}

	@Test
	@DisplayName("문제 셋 단건 조회 테스트 - 성공")
	void getQuestionSetTest_Success() {
		// given
		final Long questionSetId = 1L;
		final QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntity.getId()).thenReturn(questionSetId);
		when(questionSetEntity.getSubject()).thenReturn("Test Subject");

		when(questionSetEntityRepository.findById(questionSetId))
			.thenReturn(Optional.of(questionSetEntity));

		// when
		QuestionSetDto result = questionSetService.getQuestionSet(questionSetId);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(questionSetId);
		assertThat(result.getSubject()).isEqualTo("Test Subject");

		verify(questionSetEntityRepository, times(1)).findById(questionSetId);
	}

	@Test
	@DisplayName("문제 셋 단건 조회 테스트 - 실패 (ID를 찾을 수 없음)")
	void getQuestionSetTest_Fail_NotFound() {
		// given
		final Long questionSetId = 1L;
		when(questionSetEntityRepository.findById(questionSetId))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> questionSetService.getQuestionSet(questionSetId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Question set not found");

		verify(questionSetEntityRepository, times(1)).findById(questionSetId);
	}
}
