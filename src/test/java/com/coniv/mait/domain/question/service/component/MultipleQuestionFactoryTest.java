package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;

@ExtendWith(MockitoExtension.class)
class MultipleQuestionFactoryTest {

	@InjectMocks
	private MultipleQuestionFactory multipleQuestionFactory;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Mock
	private QuestionSetEntity questionSetEntity;

	@Test
	@DisplayName("getQuestionType - MULTIPLE 타입 반환")
	void getQuestionType() {
		// when
		QuestionType result = multipleQuestionFactory.getQuestionType();

		// then
		assertEquals(QuestionType.MULTIPLE, result);
	}

	@Test
	@DisplayName("save - 객관식 문제와 선택지 저장 성공")
	void save_Success() {
		// given
		List<MultipleChoiceDto> choices = List.of(
			MultipleChoiceDto.builder().number(1).content("선택지 1").isCorrect(true).build(),
			MultipleChoiceDto.builder().number(2).content("선택지 2").isCorrect(false).build(),
			MultipleChoiceDto.builder().number(3).content("선택지 3").isCorrect(false).build()
		);

		MultipleQuestionDto questionDto = MultipleQuestionDto.builder()
			.content("객관식 문제 내용")
			.explanation("해설")
			.number(1L)
			.choices(choices)
			.build();

		// when
		QuestionEntity result = multipleQuestionFactory.save(questionDto, questionSetEntity);

		// then
		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(MultipleQuestionEntity.class);
		assertThat(((MultipleQuestionEntity)result).getAnswerCount()).isEqualTo(1);
		verify(questionEntityRepository).save(any(MultipleQuestionEntity.class));
		verify(multipleChoiceEntityRepository).saveAll(any());
	}

	@Test
	@DisplayName("getQuestion - answerVisible=true일 때 정답 포함하여 반환")
	void getQuestion_AnswerVisible() {
		// given
		MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		List<MultipleChoiceEntity> choices = List.of(
			mock(MultipleChoiceEntity.class),
			mock(MultipleChoiceEntity.class)
		);
		when(multipleChoiceEntityRepository.findAllByQuestionId(1L)).thenReturn(choices);

		// when
		QuestionDto result = multipleQuestionFactory.getQuestion(question, true);

		// then
		assertThat(result).isInstanceOf(MultipleQuestionDto.class);
		verify(multipleChoiceEntityRepository).findAllByQuestionId(1L);
	}

	@Test
	@DisplayName("getQuestion - answerVisible=false일 때 정답 숨김하여 반환")
	void getQuestion_AnswerHidden() {
		// given
		MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		List<MultipleChoiceEntity> choices = List.of(
			mock(MultipleChoiceEntity.class),
			mock(MultipleChoiceEntity.class)
		);
		when(multipleChoiceEntityRepository.findAllByQuestionId(1L)).thenReturn(choices);

		// when
		QuestionDto result = multipleQuestionFactory.getQuestion(question, false);

		// then
		assertThat(result).isInstanceOf(MultipleQuestionDto.class);
		verify(multipleChoiceEntityRepository).findAllByQuestionId(1L);
	}

	@Test
	@DisplayName("createChoices - 중복된 선택지 번호 시 예외 발생")
	void createChoices_DuplicateNumber_ThrowsException() {
		// given
		List<MultipleChoiceDto> choices = List.of(
			MultipleChoiceDto.builder().number(1).content("선택지 1").isCorrect(true).build(),
			MultipleChoiceDto.builder().number(1).content("선택지 2").isCorrect(false).build() // 중복 번호
		);

		MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);

		// when & then
		assertThrows(UserParameterException.class,
			() -> multipleQuestionFactory.createChoices(choices, question));
	}

	@Test
	@DisplayName("createChoices - 정상적인 선택지 생성")
	void createChoices_Success() {
		// given
		List<MultipleChoiceDto> choices = List.of(
			MultipleChoiceDto.builder().number(1).content("선택지 1").isCorrect(true).build(),
			MultipleChoiceDto.builder().number(2).content("선택지 2").isCorrect(false).build()
		);

		MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);

		// when
		List<MultipleChoiceEntity> result = multipleQuestionFactory.createChoices(choices, question);

		// then
		assertEquals(2, result.size());
	}

	@Test
	@DisplayName("deleteSubEntities - 하위 엔티티들을 삭제한다")
	void deleteSubEntities() {
		// given
		MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		// when
		multipleQuestionFactory.deleteSubEntities(question);

		// then
		verify(multipleChoiceEntityRepository).deleteBulkAllByQuestionId(1L);
	}

	@Test
	@DisplayName("createSubEntities - 하위 엔티티들을 생성하여 저장한다")
	void createSubEntities() {
		// given
		List<MultipleChoiceDto> choices = List.of(
			MultipleChoiceDto.builder().number(1).content("선택지 1").isCorrect(true).build(),
			MultipleChoiceDto.builder().number(2).content("선택지 2").isCorrect(false).build()
		);

		MultipleQuestionDto questionDto = MultipleQuestionDto.builder()
			.content("문제 내용")
			.choices(choices)
			.build();

		MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);

		// when
		multipleQuestionFactory.createSubEntities(questionDto, question);

		// then
		verify(multipleChoiceEntityRepository).saveAll(any(List.class));
		verify(question).updateAnswerCount(1);
	}

	@Test
	@DisplayName("복제 시 원본의 잘못된 정답 개수 대신 문제별 선택지에서 재계산한다")
	void copySubEntities_RecalculatesAnswerCounts() {
		// given
		MultipleQuestionEntity source = MultipleQuestionEntity.builder().id(1L).answerCount(7).build();
		MultipleQuestionEntity incorrectSource = MultipleQuestionEntity.builder().id(2L).answerCount(4).build();
		MultipleQuestionEntity emptySource = MultipleQuestionEntity.builder().id(3L).answerCount(4).build();
		MultipleQuestionEntity copied = multipleQuestionFactory.copyQuestion(source, questionSetEntity);
		MultipleQuestionEntity incorrectCopy = multipleQuestionFactory.copyQuestion(incorrectSource, questionSetEntity);
		MultipleQuestionEntity emptyCopy = multipleQuestionFactory.copyQuestion(emptySource, questionSetEntity);
		when(multipleChoiceEntityRepository.findAllByQuestionIdIn(anyList())).thenReturn(List.of(
			MultipleChoiceEntity.builder().question(source).number(1).isCorrect(true).build(),
			MultipleChoiceEntity.builder().question(source).number(2).isCorrect(false).build(),
			MultipleChoiceEntity.builder().question(source).number(3).isCorrect(true).build(),
			MultipleChoiceEntity.builder().question(incorrectSource).number(1).isCorrect(false).build()
		));

		// when
		multipleQuestionFactory.copySubEntities(Map.of(1L, copied, 2L, incorrectCopy, 3L, emptyCopy));

		// then
		assertThat(copied.getAnswerCount()).isEqualTo(2);
		assertThat(incorrectCopy.getAnswerCount()).isZero();
		assertThat(emptyCopy.getAnswerCount()).isZero();
		assertThat(source.getAnswerCount()).isEqualTo(7);
		verify(multipleChoiceEntityRepository).saveAll(argThat(choices -> {
			List<MultipleChoiceEntity> savedChoices = (List<MultipleChoiceEntity>)choices;
			return savedChoices.size() == 4
				&& savedChoices.stream().filter(MultipleChoiceEntity::isCorrect)
				.allMatch(choice -> choice.getQuestion() == copied);
		}));
	}
}
