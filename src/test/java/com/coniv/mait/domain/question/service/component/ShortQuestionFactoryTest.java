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

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;
import com.coniv.mait.domain.question.service.dto.ShortQuestionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.global.util.RandomUtil;

@ExtendWith(MockitoExtension.class)
class ShortQuestionFactoryTest {

	@InjectMocks
	private ShortQuestionFactory shortQuestionFactory;

	@Test
	@DisplayName("주관식 문제 생성 테스트 - 정상적으로 생성")
	void create_Success() {
		// given
		List<ShortAnswerDto> answers = Arrays.asList(
			ShortAnswerDto.builder()
				.number(1L)
				.answer("정답1")
				.isMain(true)
				.build(),
			ShortAnswerDto.builder()
				.number(2L)
				.answer("정답2")
				.isMain(true)
				.build()
		);

		ShortQuestionDto questionDto = ShortQuestionDto.builder()
			.content("주관식 문제 내용")
			.explanation("문제 해설")
			.number(1L)
			.shortAnswers(answers)
			.build();

		QuestionSetEntity questionSet = QuestionSetEntity.of("테스트 세트", QuestionSetCreationType.MANUAL);
		int expectedDisplayDelay = 1234;

		// when
		try (MockedStatic<RandomUtil> mockedRandomUtil = mockStatic(RandomUtil.class)) {
			mockedRandomUtil.when(() -> RandomUtil.getRandomNumber(5000))
				.thenReturn(expectedDisplayDelay);

			ShortQuestionEntity result = shortQuestionFactory.create(questionDto, questionSet);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getId()).isNull();
			assertThat(result.getContent()).isEqualTo("주관식 문제 내용");
			assertThat(result.getExplanation()).isEqualTo("문제 해설");
			assertThat(result.getNumber()).isEqualTo(1L);
			assertThat(result.getDisplayDelayMilliseconds()).isEqualTo(expectedDisplayDelay);
			assertThat(result.getQuestionSet()).isEqualTo(questionSet);
			assertThat(result.getAnswerCount()).isEqualTo(2);

			mockedRandomUtil.verify(() -> RandomUtil.getRandomNumber(5000));
		}
	}

	@Test
	@DisplayName("주관식 정답 리스트 생성 테스트 - 정상적으로 생성")
	void createShortAnswers_Success() {
		// given
		ShortQuestionEntity question = ShortQuestionEntity.builder()
			.content("주관식 문제")
			.explanation("설명")
			.number(1L)
			.displayDelayMilliseconds(1000)
			.questionSet(null)
			.answerCount(3)
			.id(10L)
			.build();

		List<ShortAnswerDto> answerDtos = Arrays.asList(
			ShortAnswerDto.builder()
				.number(1L)
				.answer("정답1")
				.isMain(true)
				.build(),
			ShortAnswerDto.builder()
				.number(1L)
				.answer("정답1-2")
				.isMain(false)
				.build(),
			ShortAnswerDto.builder()
				.number(2L)
				.answer("정답2")
				.isMain(true)
				.build()
		);

		// when
		List<ShortAnswerEntity> result = shortQuestionFactory.createShortAnswers(answerDtos, question);

		// then
		assertThat(result).hasSize(3);
		
		ShortAnswerEntity first = result.get(0);
		assertThat(first.getId()).isNull();
		assertThat(first.getNumber()).isEqualTo(1L);
		assertThat(first.getAnswer()).isEqualTo("정답1");
		assertThat(first.isMain()).isTrue();
		assertThat(first.getShortQuestionId()).isEqualTo(10L);

		ShortAnswerEntity second = result.get(1);
		assertThat(second.getId()).isNull();
		assertThat(second.getNumber()).isEqualTo(1L);
		assertThat(second.getAnswer()).isEqualTo("정답1-2");
		assertThat(second.isMain()).isFalse();
		assertThat(second.getShortQuestionId()).isEqualTo(10L);

		ShortAnswerEntity third = result.get(2);
		assertThat(third.getId()).isNull();
		assertThat(third.getNumber()).isEqualTo(2L);
		assertThat(third.getAnswer()).isEqualTo("정답2");
		assertThat(third.isMain()).isTrue();
		assertThat(third.getShortQuestionId()).isEqualTo(10L);
	}

	@Test
	@DisplayName("주관식 정답 생성 실패 - 메인 답안 없음")
	void createShortAnswers_NoMainAnswer_ThrowsException() {
		// given
		ShortQuestionEntity question = ShortQuestionEntity.builder()
			.content("주관식 문제")
			.explanation("설명")
			.number(1L)
			.displayDelayMilliseconds(1000)
			.questionSet(null)
			.answerCount(2)
			.id(10L)
			.build();

		List<ShortAnswerDto> answersWithoutMain = Arrays.asList(
			ShortAnswerDto.builder()
				.number(1L)
				.answer("정답1")
				.isMain(false)
				.build(),
			ShortAnswerDto.builder()
				.number(1L)
				.answer("정답2")
				.isMain(false)
				.build()
		);

		// when & then
		assertThatThrownBy(() -> shortQuestionFactory.createShortAnswers(answersWithoutMain, question))
			.isInstanceOf(UserParameterException.class)
			.hasMessageContaining("Each short answer number must have exactly one main answer. Number 1 has 0 main answers.");
	}

	@Test
	@DisplayName("주관식 정답 생성 실패 - 메인 답안이 여러 개")
	void createShortAnswers_MultipleMainAnswers_ThrowsException() {
		// given
		ShortQuestionEntity question = ShortQuestionEntity.builder()
			.content("주관식 문제")
			.explanation("설명")
			.number(1L)
			.displayDelayMilliseconds(1000)
			.questionSet(null)
			.answerCount(3)
			.id(10L)
			.build();

		List<ShortAnswerDto> answersWithMultipleMain = Arrays.asList(
			ShortAnswerDto.builder()
				.number(1L)
				.answer("정답1")
				.isMain(true)
				.build(),
			ShortAnswerDto.builder()
				.number(1L)
				.answer("정답2")
				.isMain(true)
				.build(),
			ShortAnswerDto.builder()
				.number(1L)
				.answer("정답3")
				.isMain(false)
				.build()
		);

		// when & then
		assertThatThrownBy(() -> shortQuestionFactory.createShortAnswers(answersWithMultipleMain, question))
			.isInstanceOf(UserParameterException.class)
			.hasMessageContaining("Each short answer number must have exactly one main answer. Number 1 has 2 main answers.");
	}

	@Test
	@DisplayName("주관식 정답 생성 실패 - 다른 번호에서 메인 답안 없음")
	void createShortAnswers_DifferentNumberNoMainAnswer_ThrowsException() {
		// given
		ShortQuestionEntity question = ShortQuestionEntity.builder()
			.content("주관식 문제")
			.explanation("설명")
			.number(1L)
			.displayDelayMilliseconds(1000)
			.questionSet(null)
			.answerCount(3)
			.id(10L)
			.build();

		List<ShortAnswerDto> answers = Arrays.asList(
			ShortAnswerDto.builder()
				.number(1L)
				.answer("정답1")
				.isMain(true)
				.build(),
			ShortAnswerDto.builder()
				.number(2L)
				.answer("정답2")
				.isMain(false)
				.build(),
			ShortAnswerDto.builder()
				.number(2L)
				.answer("정답3")
				.isMain(false)
				.build()
		);

		// when & then
		assertThatThrownBy(() -> shortQuestionFactory.createShortAnswers(answers, question))
			.isInstanceOf(UserParameterException.class)
			.hasMessageContaining("Each short answer number must have exactly one main answer. Number 2 has 0 main answers.");
	}
}
