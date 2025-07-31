package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;
import com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.global.util.RandomUtil;

@ExtendWith(MockitoExtension.class)
class FillBlankQuestionFactoryTest {

	@InjectMocks
	private FillBlankQuestionFactory fillBlankQuestionFactory;

	@Test
	@DisplayName("빈칸 문제 생성 테스트 - 정상적으로 생성")
	void create_Success() {
		// given
		FillBlankQuestionDto questionDto = FillBlankQuestionDto.builder()
			.content("빈칸에 들어갈 적절한 단어는 ___입니다.")
			.explanation("테스트 설명")
			.number(1L)
			.build();

		QuestionSetEntity questionSet = QuestionSetEntity.of("테스트 과목", QuestionSetCreationType.MANUAL);

		int expectedDisplayDelay = 3000;

		// when
		try (MockedStatic<RandomUtil> mockedRandomUtil = mockStatic(RandomUtil.class)) {
			mockedRandomUtil.when(() -> RandomUtil.getRandomNumber(5000))
				.thenReturn(expectedDisplayDelay);

			FillBlankQuestionEntity result = fillBlankQuestionFactory.create(questionDto, questionSet);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getId()).isNull(); // ID는 Factory에서 설정하지 않음
			assertThat(result.getContent()).isEqualTo("빈칸에 들어갈 적절한 단어는 ___입니다.");
			assertThat(result.getExplanation()).isEqualTo("테스트 설명");
			assertThat(result.getNumber()).isEqualTo(1L);
			assertThat(result.getDisplayDelayMilliseconds()).isEqualTo(expectedDisplayDelay);
			assertThat(result.getQuestionSet()).isEqualTo(questionSet);

			mockedRandomUtil.verify(() -> RandomUtil.getRandomNumber(5000));
		}
	}

	@Test
	@DisplayName("빈칸 문제 답변 리스트 생성 테스트 - 정상적으로 생성")
	void createFillBlankAnswers_Success() {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("테스트 과목", QuestionSetCreationType.MANUAL);

		FillBlankQuestionEntity question = FillBlankQuestionEntity.builder()
			.content("빈칸에 들어갈 적절한 단어는 ___입니다.")
			.explanation("설명")
			.number(1L)
			.displayDelayMilliseconds(1000)
			.questionSet(questionSet)
			.id(1L) // 답변 생성 시 필요한 question ID
			.build();

		List<FillBlankAnswerDto> answerDtos = Arrays.asList(
			FillBlankAnswerDto.builder()
				.number(1L)
				.answer("정답")
				.isMain(true)
				.build(),
			FillBlankAnswerDto.builder()
				.number(2L)
				.answer("정답2")
				.isMain(false)
				.build(),
			FillBlankAnswerDto.builder()
				.number(3L)
				.answer("정답3")
				.isMain(false)
				.build()
		);

		// when
		List<FillBlankAnswerEntity> result = fillBlankQuestionFactory.createFillBlankAnswers(answerDtos, question);

		// then
		assertThat(result).hasSize(3);

		FillBlankAnswerEntity firstAnswer = result.get(0);
		assertThat(firstAnswer.getId()).isNull(); // ID는 Factory에서 설정하지 않음
		assertThat(firstAnswer.getNumber()).isEqualTo(1L);
		assertThat(firstAnswer.getAnswer()).isEqualTo("정답");
		assertThat(firstAnswer.isMain()).isTrue();
		assertThat(firstAnswer.getFillBlankQuestionId()).isEqualTo(1L);

		FillBlankAnswerEntity secondAnswer = result.get(1);
		assertThat(secondAnswer.getId()).isNull(); // ID는 Factory에서 설정하지 않음
		assertThat(secondAnswer.getNumber()).isEqualTo(2L);
		assertThat(secondAnswer.getAnswer()).isEqualTo("정답2");
		assertThat(secondAnswer.isMain()).isFalse();
		assertThat(secondAnswer.getFillBlankQuestionId()).isEqualTo(1L);

		FillBlankAnswerEntity thirdAnswer = result.get(2);
		assertThat(thirdAnswer.getId()).isNull(); // ID는 Factory에서 설정하지 않음
		assertThat(thirdAnswer.getNumber()).isEqualTo(3L);
		assertThat(thirdAnswer.getAnswer()).isEqualTo("정답3");
		assertThat(thirdAnswer.isMain()).isFalse();
		assertThat(thirdAnswer.getFillBlankQuestionId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("빈칸 문제 답변 리스트 생성 테스트 - 빈 리스트")
	void createFillBlankAnswers_EmptyList() {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("테스트 과목", QuestionSetCreationType.MANUAL);

		FillBlankQuestionEntity question = FillBlankQuestionEntity.builder()
			.content("빈칸에 들어갈 적절한 단어는 ___입니다.")
			.explanation("설명")
			.number(1L)
			.displayDelayMilliseconds(1000)
			.questionSet(questionSet)
			.id(1L)
			.build();

		List<FillBlankAnswerDto> emptyAnswers = Arrays.asList();

		// when
		List<FillBlankAnswerEntity> result = fillBlankQuestionFactory.createFillBlankAnswers(emptyAnswers, question);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("빈칸 문제 답변 생성 실패 - 중복된 답변 번호")
	void createFillBlankAnswers_DuplicateNumber_ThrowsException() {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("테스트 과목", QuestionSetCreationType.MANUAL);

		FillBlankQuestionEntity question = FillBlankQuestionEntity.builder()
			.content("빈칸에 들어갈 적절한 단어는 ___입니다.")
			.explanation("설명")
			.number(1L)
			.displayDelayMilliseconds(1000)
			.questionSet(questionSet)
			.id(1L)
			.build();

		List<FillBlankAnswerDto> duplicateAnswers = Arrays.asList(
			FillBlankAnswerDto.builder()
				.number(1L)
				.answer("정답1")
				.isMain(true)
				.build(),
			FillBlankAnswerDto.builder()
				.number(1L) // 중복된 번호
				.answer("정답2")
				.isMain(false)
				.build()
		);

		// when & then
		assertThatThrownBy(() -> fillBlankQuestionFactory.createFillBlankAnswers(duplicateAnswers, question))
			.isInstanceOf(UserParameterException.class)
			.hasMessage("Fill blank answer numbers must be unique.");
	}

	@Test
	@DisplayName("빈칸 문제 답변 생성 테스트 - 하나의 답변만 있는 경우")
	void createFillBlankAnswers_SingleAnswer() {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("테스트 과목", QuestionSetCreationType.MANUAL);

		FillBlankQuestionEntity question = FillBlankQuestionEntity.builder()
			.content("빈칸에 들어갈 적절한 단어는 ___입니다.")
			.explanation("설명")
			.number(1L)
			.displayDelayMilliseconds(1000)
			.questionSet(questionSet)
			.id(1L)
			.build();

		List<FillBlankAnswerDto> singleAnswer = Arrays.asList(
			FillBlankAnswerDto.builder()
				.number(1L)
				.answer("정답")
				.isMain(true)
				.build()
		);

		// when
		List<FillBlankAnswerEntity> result = fillBlankQuestionFactory.createFillBlankAnswers(singleAnswer, question);

		// then
		assertThat(result).hasSize(1);
		FillBlankAnswerEntity answer = result.get(0);
		assertThat(answer.getNumber()).isEqualTo(1L);
		assertThat(answer.getAnswer()).isEqualTo("정답");
		assertThat(answer.isMain()).isTrue();
		assertThat(answer.getFillBlankQuestionId()).isEqualTo(1L);
	}
}