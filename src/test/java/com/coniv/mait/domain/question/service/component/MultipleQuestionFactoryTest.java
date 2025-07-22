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

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.global.util.RandomUtil;

@ExtendWith(MockitoExtension.class)
class MultipleQuestionFactoryTest {

	@InjectMocks
	private MultipleQuestionFactory multipleQuestionFactory;

	@Test
	@DisplayName("객관식 문제 생성 테스트 - 정상적으로 생성")
	void create_Success() {
		// given
		List<MultipleChoiceDto> choices = Arrays.asList(
			MultipleChoiceDto.builder()
				.id(1L)
				.number(1)
				.content("선택지 1")
				.isCorrect(true)
				.build(),
			MultipleChoiceDto.builder()
				.id(2L)
				.number(2)
				.content("선택지 2")
				.isCorrect(false)
				.build(),
			MultipleChoiceDto.builder()
				.id(3L)
				.number(3)
				.content("선택지 3")
				.isCorrect(true)
				.build()
		);

		MultipleQuestionDto questionDto = MultipleQuestionDto.builder()
			.id(1L)
			.content("테스트 문제")
			.explanation("테스트 설명")
			.number(1L)
			.choices(choices)
			.build();

		QuestionSetEntity questionSet = QuestionSetEntity.of("테스트 과목", QuestionSetCreationType.MANUAL);

		int expectedDisplayDelay = 3000;

		// when
		try (MockedStatic<RandomUtil> mockedRandomUtil = mockStatic(RandomUtil.class)) {
			mockedRandomUtil.when(() -> RandomUtil.getRandomNumber(5000))
				.thenReturn(expectedDisplayDelay);

			MultipleQuestionEntity result = multipleQuestionFactory.create(questionDto, questionSet);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getId()).isEqualTo(1L);
			assertThat(result.getContent()).isEqualTo("테스트 문제");
			assertThat(result.getExplanation()).isEqualTo("테스트 설명");
			assertThat(result.getNumber()).isEqualTo(1L);
			assertThat(result.getDisplayDelayMilliseconds()).isEqualTo(expectedDisplayDelay);
			assertThat(result.getQuestionSet()).isEqualTo(questionSet);
			assertThat(result.getAnswerCount()).isEqualTo(2); // 정답인 선택지가 2개

			mockedRandomUtil.verify(() -> RandomUtil.getRandomNumber(5000));
		}
	}

	@Test
	@DisplayName("객관식 선택지 리스트 생성 테스트 - 정상적으로 생성")
	void createChoices_Success() {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("테스트 과목", QuestionSetCreationType.MANUAL);

		MultipleQuestionEntity question = MultipleQuestionEntity.builder()
			.id(1L)
			.content("테스트 문제")
			.explanation("설명")
			.number(1L)
			.displayDelayMilliseconds(1000)
			.questionSet(questionSet)
			.answerCount(1)
			.build();

		List<MultipleChoiceDto> choiceDtos = Arrays.asList(
			MultipleChoiceDto.builder()
				.id(1L)
				.number(1)
				.content("첫 번째 선택지")
				.isCorrect(true)
				.build(),
			MultipleChoiceDto.builder()
				.id(2L)
				.number(2)
				.content("두 번째 선택지")
				.isCorrect(false)
				.build(),
			MultipleChoiceDto.builder()
				.id(3L)
				.number(3)
				.content("세 번째 선택지")
				.isCorrect(false)
				.build()
		);

		// when
		List<MultipleChoiceEntity> result = multipleQuestionFactory.createChoices(choiceDtos, question);

		// then
		assertThat(result).hasSize(3);

		MultipleChoiceEntity firstChoice = result.get(0);
		assertThat(firstChoice.getId()).isEqualTo(1L);
		assertThat(firstChoice.getNumber()).isEqualTo(1);
		assertThat(firstChoice.getContent()).isEqualTo("첫 번째 선택지");
		assertThat(firstChoice.isCorrect()).isTrue();
		assertThat(firstChoice.getQuestion()).isEqualTo(question);

		MultipleChoiceEntity secondChoice = result.get(1);
		assertThat(secondChoice.getId()).isEqualTo(2L);
		assertThat(secondChoice.getNumber()).isEqualTo(2);
		assertThat(secondChoice.getContent()).isEqualTo("두 번째 선택지");
		assertThat(secondChoice.isCorrect()).isFalse();
		assertThat(secondChoice.getQuestion()).isEqualTo(question);

		MultipleChoiceEntity thirdChoice = result.get(2);
		assertThat(thirdChoice.getId()).isEqualTo(3L);
		assertThat(thirdChoice.getNumber()).isEqualTo(3);
		assertThat(thirdChoice.getContent()).isEqualTo("세 번째 선택지");
		assertThat(thirdChoice.isCorrect()).isFalse();
		assertThat(thirdChoice.getQuestion()).isEqualTo(question);
	}

	@Test
	@DisplayName("객관식 선택지 리스트 생성 테스트 - 빈 리스트")
	void createChoices_EmptyList() {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("테스트 과목", QuestionSetCreationType.MANUAL);

		MultipleQuestionEntity question = MultipleQuestionEntity.builder()
			.id(1L)
			.content("테스트 문제")
			.explanation("설명")
			.number(1L)
			.displayDelayMilliseconds(1000)
			.questionSet(questionSet)
			.answerCount(0)
			.build();

		List<MultipleChoiceDto> emptyChoices = Arrays.asList();

		// when
		List<MultipleChoiceEntity> result = multipleQuestionFactory.createChoices(emptyChoices, question);

		// then
		assertThat(result).isEmpty();
	}

}
