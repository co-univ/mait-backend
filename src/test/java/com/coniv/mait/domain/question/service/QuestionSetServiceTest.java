package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
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

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.domain.question.enums.QuestionValidationResult;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionSetServiceTest {

	@InjectMocks
	private QuestionSetService questionSetService;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private QuestionService questionService;

	@Mock
	private com.coniv.mait.domain.question.service.component.QuestionChecker questionChecker;

	// Todo: 생성 관련 feature가 최종 완성 시에 수정
	// @Test
	// @DisplayName("문제 셋 생성 테스트")
	// void createQuestionSetTest() {
	// 	// given
	// 	String subject = "Sample Subject";
	// 	var creationType = QuestionSetCreationType.MANUAL;
	// 	final Long questionSetId = 1L;
	//
	// 	// when
	// 	QuestionSetDto questionSetDto = questionSetService.createQuestionSet(subject, creationType);
	//
	// 	// then
	// 	assertThat(questionSetDto).isNotNull();
	// 	assertThat(questionSetDto.getSubject()).isEqualTo(subject);
	// 	verify(questionSetEntityRepository).save(any());
	// 	verify(questionService).createDefaultQuestion(any());
	// }

	@Test
	@DisplayName("문제 셋 목록 조회 테스트")
	void getQuestionSetsTest() {
		// given
		final Long teamId = 1L;
		final LocalDateTime now = LocalDateTime.now();
		final DeliveryMode mode = DeliveryMode.LIVE_TIME;
		QuestionSetEntity older = mock(QuestionSetEntity.class);
		QuestionSetEntity newer = mock(QuestionSetEntity.class);

		when(older.getId()).thenReturn(1L);
		when(newer.getId()).thenReturn(2L);
		when(older.getModifiedAt()).thenReturn(now.minusDays(1));
		when(newer.getModifiedAt()).thenReturn(now.plusDays(1));

		when(questionSetEntityRepository.findAllByTeamIdAndDeliveryMode(teamId, mode))
			.thenReturn(List.of(older, newer));

		// when
		List<QuestionSetDto> result = questionSetService.getQuestionSets(teamId, mode);

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getId()).isEqualTo(2L); // 최신 것이 먼저
		assertThat(result.get(1).getId()).isEqualTo(1L);

		verify(questionSetEntityRepository, times(1)).findAllByTeamIdAndDeliveryMode(teamId, mode);
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
		when(questionEntityRepository.countByQuestionSetId(questionSetId))
			.thenReturn(5L); // 예시로 5개의 문제를 가진다고 가정

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

	@Test
	@DisplayName("문제 셋 완료 처리 테스트")
	void completeQuestionSetTest() {
		// given
		final Long questionSetId = 1L;
		final String originalSubject = "원래 주제";
		final String newTitle = "변경할 제목";
		final String newSubject = "변경할 주제";
		final DeliveryMode newMode = DeliveryMode.REVIEW;
		final String levelDescription = "난이도 설명";
		final QuestionSetVisibility newVisibility = QuestionSetVisibility.GROUP;

		QuestionSetEntity questionSetEntity = QuestionSetEntity.builder()
			.subject(originalSubject)
			.title("원래 제목")
			.deliveryMode(DeliveryMode.LIVE_TIME)
			.visibility(QuestionSetVisibility.GROUP)
			.build();

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));

		// when
		QuestionSetDto result = questionSetService.completeQuestionSet(
			questionSetId,
			newTitle,
			newSubject,
			newMode,
			levelDescription,
			newVisibility
		);

		// then
		verify(questionSetEntityRepository, times(1)).findById(questionSetId);

		assertThat(questionSetEntity.getTitle()).isEqualTo(newTitle);
		assertThat(questionSetEntity.getSubject()).isEqualTo(newSubject);
		assertThat(questionSetEntity.getDeliveryMode()).isEqualTo(newMode);
		assertThat(questionSetEntity.getLevelDescription()).isEqualTo(levelDescription);
		assertThat(questionSetEntity.getVisibility()).isEqualTo(newVisibility);

		assertThat(result).isNotNull();
		assertThat(result.getTitle()).isEqualTo(newTitle);
		assertThat(result.getSubject()).isEqualTo(newSubject);
		assertThat(result.getDeliveryMode()).isEqualTo(newMode);
		assertThat(result.getLevelDescription()).isEqualTo(levelDescription);
		assertThat(result.getVisibility()).isEqualTo(newVisibility);
	}

	@Test
	@DisplayName("문제 셋 완료 처리 테스트 - 실패 (존재하지 않는 문제셋)")
	void completeQuestionSetTest_Fail_NotFound() {
		// given
		final Long questionSetId = 999L;
		when(questionSetEntityRepository.findById(questionSetId))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> questionSetService.completeQuestionSet(
			questionSetId,
			"제목",
			"주제",
			DeliveryMode.LIVE_TIME,
			"설명",
			QuestionSetVisibility.GROUP
		))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("Question set not found");

		verify(questionSetEntityRepository, times(1)).findById(questionSetId);
	}

	@Test
	@DisplayName("문제 셋 제목 수정 테스트")
	void updateQuestionSetFieldTest() {
		// given
		final Long questionSetId = 1L;
		final String originalTitle = "원래 제목";
		final String newTitle = "새로운 제목";

		QuestionSetEntity questionSetEntity = QuestionSetEntity.builder()
			.subject("주제")
			.title(originalTitle)
			.deliveryMode(DeliveryMode.LIVE_TIME)
			.visibility(QuestionSetVisibility.GROUP)
			.build();

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));

		// when
		questionSetService.updateQuestionSetField(questionSetId, newTitle);

		// then
		verify(questionSetEntityRepository, times(1)).findById(questionSetId);
		assertThat(questionSetEntity.getTitle()).isEqualTo(newTitle);
	}

	@Test
	@DisplayName("문제 셋 검증 테스트 - 모든 문제가 유효한 경우 빈 리스트 반환")
	void validateQuestionSet_AllValid_ReturnsEmptyList() {
		// given
		final Long questionSetId = 1L;
		final QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntity.getId()).thenReturn(questionSetId);

		QuestionEntity question1 = mock(QuestionEntity.class);
		QuestionEntity question2 = mock(QuestionEntity.class);

		List<QuestionEntity> questions = List.of(question1, question2);

		QuestionValidateDto validDto1 = QuestionValidateDto.builder()
			.questionId(1L)
			.number(1L)
			.valid(true)
			.build();

		QuestionValidateDto validDto2 = QuestionValidateDto.builder()
			.questionId(2L)
			.number(2L)
			.valid(true)
			.build();

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId)).thenReturn(questions);
		when(questionChecker.validateQuestion(question1)).thenReturn(validDto1);
		when(questionChecker.validateQuestion(question2)).thenReturn(validDto2);

		// when
		List<QuestionValidateDto> result = questionSetService.validateQuestionSet(questionSetId);

		// then
		assertThat(result).isEmpty();
		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionEntityRepository).findAllByQuestionSetId(questionSetId);
		verify(questionChecker).validateQuestion(question1);
		verify(questionChecker).validateQuestion(question2);
	}

	@Test
	@DisplayName("문제 셋 검증 테스트 - 일부 문제가 유효하지 않은 경우 해당 문제만 반환")
	void validateQuestionSet_SomeInvalid_ReturnsInvalidQuestions() {
		// given
		final Long questionSetId = 1L;
		final QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntity.getId()).thenReturn(questionSetId);

		QuestionEntity question1 = mock(QuestionEntity.class);
		QuestionEntity question2 = mock(QuestionEntity.class);
		QuestionEntity question3 = mock(QuestionEntity.class);

		List<QuestionEntity> questions = List.of(question1, question2, question3);

		QuestionValidateDto validDto = QuestionValidateDto.builder()
			.questionId(1L)
			.number(1L)
			.valid(true)
			.build();

		QuestionValidateDto invalidDto1 = QuestionValidateDto.builder()
			.questionId(2L)
			.number(2L)
			.valid(false)
			.reason(QuestionValidationResult.EMPTY_CONTENT)
			.build();

		QuestionValidateDto invalidDto2 = QuestionValidateDto.builder()
			.questionId(3L)
			.number(3L)
			.valid(false)
			.reason(QuestionValidationResult.INVALID_CHOICE_COUNT)
			.build();

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId)).thenReturn(questions);
		when(questionChecker.validateQuestion(question1)).thenReturn(validDto);
		when(questionChecker.validateQuestion(question2)).thenReturn(invalidDto1);
		when(questionChecker.validateQuestion(question3)).thenReturn(invalidDto2);

		// when
		List<QuestionValidateDto> result = questionSetService.validateQuestionSet(questionSetId);

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getQuestionId()).isEqualTo(2L);
		assertThat(result.get(0).isValid()).isFalse();
		assertThat(result.get(0).getReason()).isEqualTo(QuestionValidationResult.EMPTY_CONTENT);
		assertThat(result.get(1).getQuestionId()).isEqualTo(3L);
		assertThat(result.get(1).isValid()).isFalse();
		assertThat(result.get(1).getReason()).isEqualTo(QuestionValidationResult.INVALID_CHOICE_COUNT);

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionEntityRepository).findAllByQuestionSetId(questionSetId);
		verify(questionChecker).validateQuestion(question1);
		verify(questionChecker).validateQuestion(question2);
		verify(questionChecker).validateQuestion(question3);
	}
}
