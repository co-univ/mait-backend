package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;
import com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;

@ExtendWith(MockitoExtension.class)
class FillBlankQuestionFactoryTest {

	@InjectMocks
	private FillBlankQuestionFactory fillBlankQuestionFactory;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@Mock
	private QuestionSetEntity questionSetEntity;

	@Test
	@DisplayName("getQuestionType - FILL_BLANK 타입 반환")
	void getQuestionType() {
		// when
		QuestionType result = fillBlankQuestionFactory.getQuestionType();

		// then
		assertEquals(QuestionType.FILL_BLANK, result);
	}

	@Test
	@DisplayName("save - 빈칸채우기 문제와 정답 저장 성공")
	void save_Success() {
		// given
		List<FillBlankAnswerDto> answers = List.of(
			FillBlankAnswerDto.builder().number(1L).answer("정답1").isMain(true).build(),
			FillBlankAnswerDto.builder().number(1L).answer("정답2").isMain(false).build(),
			FillBlankAnswerDto.builder().number(2L).answer("정답3").isMain(true).build()
		);

		FillBlankQuestionDto questionDto = FillBlankQuestionDto.builder()
			.content("빈칸에 들어갈 적절한 단어는 ___입니다.")
			.explanation("해설")
			.number(1L)
			.fillBlankAnswers(answers)
			.build();

		// when
		fillBlankQuestionFactory.save(questionDto, questionSetEntity);

		// then
		verify(questionEntityRepository).save(any(FillBlankQuestionEntity.class));
		verify(fillBlankAnswerEntityRepository).saveAll(any());
	}

	@Test
	@DisplayName("getQuestion - answerVisible=true일 때 정답 포함하여 반환")
	void getQuestion_AnswerVisible() {
		// given
		FillBlankQuestionEntity question = mock(FillBlankQuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		List<FillBlankAnswerEntity> answers = List.of(
			mock(FillBlankAnswerEntity.class),
			mock(FillBlankAnswerEntity.class)
		);
		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(1L)).thenReturn(answers);

		// when
		QuestionDto result = fillBlankQuestionFactory.getQuestion(question, true);

		// then
		assertThat(result).isInstanceOf(FillBlankQuestionDto.class);
		verify(fillBlankAnswerEntityRepository).findAllByFillBlankQuestionId(1L);
	}

	@Test
	@DisplayName("getQuestion - answerVisible=false일 때 정답 숨김하여 반환")
	void getQuestion_AnswerHidden() {
		// given
		FillBlankQuestionEntity question = mock(FillBlankQuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		List<FillBlankAnswerEntity> answers = List.of(
			mock(FillBlankAnswerEntity.class),
			mock(FillBlankAnswerEntity.class)
		);
		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(1L)).thenReturn(answers);

		// when
		QuestionDto result = fillBlankQuestionFactory.getQuestion(question, false);

		// then
		assertThat(result).isInstanceOf(FillBlankQuestionDto.class);
		verify(fillBlankAnswerEntityRepository).findAllByFillBlankQuestionId(1L);
	}

	@Test
	@DisplayName("createFillBlankAnswers - 각 빈칸 번호별로 정확히 하나의 메인 답안이 있어야 함")
	void createFillBlankAnswers_ValidMainAnswers() {
		// given
		List<FillBlankAnswerDto> answers = List.of(
			FillBlankAnswerDto.builder().number(1L).answer("정답1").isMain(true).build(),
			FillBlankAnswerDto.builder().number(1L).answer("정답2").isMain(false).build(),
			FillBlankAnswerDto.builder().number(2L).answer("정답3").isMain(true).build()
		);

		FillBlankQuestionEntity question = mock(FillBlankQuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		// when
		List<FillBlankAnswerEntity> result = fillBlankQuestionFactory.createFillBlankAnswers(answers, question);

		// then
		assertEquals(3, result.size());
	}

	@Test
	@DisplayName("createFillBlankAnswers - 메인 답안이 없으면 예외 발생")
	void createFillBlankAnswers_NoMainAnswer_ThrowsException() {
		// given
		List<FillBlankAnswerDto> answers = List.of(
			FillBlankAnswerDto.builder().number(1L).answer("정답1").isMain(false).build(),
			FillBlankAnswerDto.builder().number(1L).answer("정답2").isMain(false).build() // 메인 답안 없음
		);

		FillBlankQuestionEntity question = mock(FillBlankQuestionEntity.class);
		// Mock 설정 제거 - 예외가 먼저 발생해서 getId() 호출되지 않음

		// when & then
		assertThrows(UserParameterException.class,
			() -> fillBlankQuestionFactory.createFillBlankAnswers(answers, question));
	}

	@Test
	@DisplayName("createFillBlankAnswers - 메인 답안이 여러 개면 예외 발생")
	void createFillBlankAnswers_MultipleMainAnswers_ThrowsException() {
		// given
		List<FillBlankAnswerDto> answers = List.of(
			FillBlankAnswerDto.builder().number(1L).answer("정답1").isMain(true).build(),
			FillBlankAnswerDto.builder().number(1L).answer("정답2").isMain(true).build() // 메인 답안 중복
		);

		FillBlankQuestionEntity question = mock(FillBlankQuestionEntity.class);
		// Mock 설정 제거 - 예외가 먼저 발생해서 getId() 호출되지 않음

		// when & then
		assertThrows(UserParameterException.class,
			() -> fillBlankQuestionFactory.createFillBlankAnswers(answers, question));
	}

	@Test
	@DisplayName("deleteSubEntities - 하위 엔티티들을 삭제한다")
	void deleteSubEntities() {
		// given
		FillBlankQuestionEntity question = mock(FillBlankQuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		// when
		fillBlankQuestionFactory.deleteSubEntities(question);

		// then
		verify(fillBlankAnswerEntityRepository).deleteAllByFillBlankQuestionId(1L);
	}

	@Test
	@DisplayName("createSubEntities - 하위 엔티티들을 생성하여 저장한다")
	void createSubEntities() {
		// given
		List<FillBlankAnswerDto> answers = List.of(
			FillBlankAnswerDto.builder().number(1L).answer("정답1").isMain(true).build(),
			FillBlankAnswerDto.builder().number(2L).answer("정답2").isMain(true).build()
		);

		FillBlankQuestionDto questionDto = FillBlankQuestionDto.builder()
			.content("빈칸채우기 문제")
			.fillBlankAnswers(answers)
			.build();

		FillBlankQuestionEntity question = mock(FillBlankQuestionEntity.class);

		// when
		fillBlankQuestionFactory.createSubEntities(questionDto, question);

		// then
		verify(fillBlankAnswerEntityRepository).saveAll(any(List.class));
	}
}
