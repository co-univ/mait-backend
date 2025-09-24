package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;
import com.coniv.mait.domain.question.service.dto.ShortQuestionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;

@ExtendWith(MockitoExtension.class)
class ShortQuestionFactoryTest {

	@InjectMocks
	private ShortQuestionFactory shortQuestionFactory;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private ShortAnswerEntityRepository shortAnswerEntityRepository;

	@Mock
	private QuestionSetEntity questionSetEntity;

	@Test
	@DisplayName("getQuestionType - SHORT 타입 반환")
	void getQuestionType() {
		// when
		QuestionType result = shortQuestionFactory.getQuestionType();

		// then
		assertEquals(QuestionType.SHORT, result);
	}

	@Test
	@DisplayName("save - 주관식 문제와 정답 저장 성공")
	void save_Success() {
		// given
		List<ShortAnswerDto> answers = List.of(
			ShortAnswerDto.builder().number(1L).answer("정답1").isMain(true).build(),
			ShortAnswerDto.builder().number(1L).answer("정답2").isMain(false).build()
		);

		ShortQuestionDto questionDto = ShortQuestionDto.builder()
			.content("주관식 문제 내용")
			.explanation("해설")
			.number(1L)
			.shortAnswers(answers)
			.build();

		// when
		QuestionEntity result = shortQuestionFactory.save(questionDto, questionSetEntity);

		// then
		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(ShortQuestionEntity.class);
		verify(questionEntityRepository).save(any(ShortQuestionEntity.class));
		verify(shortAnswerEntityRepository).saveAll(any());
	}

	@Test
	@DisplayName("getQuestion - answerVisible=true일 때 정답 포함하여 반환")
	void getQuestion_AnswerVisible() {
		// given
		ShortQuestionEntity question = mock(ShortQuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		List<ShortAnswerEntity> answers = List.of(
			mock(ShortAnswerEntity.class),
			mock(ShortAnswerEntity.class)
		);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(answers);

		// when
		QuestionDto result = shortQuestionFactory.getQuestion(question, true);

		// then
		assertThat(result).isInstanceOf(ShortQuestionDto.class);
		verify(shortAnswerEntityRepository).findAllByShortQuestionId(1L);
	}

	@Test
	@DisplayName("getQuestion - answerVisible=false일 때 정답 숨김하여 반환")
	void getQuestion_AnswerHidden() {
		// given
		ShortQuestionEntity question = mock(ShortQuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		List<ShortAnswerEntity> answers = List.of(
			mock(ShortAnswerEntity.class),
			mock(ShortAnswerEntity.class)
		);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(answers);

		// when
		QuestionDto result = shortQuestionFactory.getQuestion(question, false);

		// then
		assertThat(result).isInstanceOf(ShortQuestionDto.class);
		verify(shortAnswerEntityRepository).findAllByShortQuestionId(1L);
	}

	@Test
	@DisplayName("createShortAnswers - 각 번호별로 정확히 하나의 메인 답안이 있어야 함")
	void createShortAnswers_ValidMainAnswers() {
		// given
		List<ShortAnswerDto> answers = List.of(
			ShortAnswerDto.builder().number(1L).answer("정답1").isMain(true).build(),
			ShortAnswerDto.builder().number(1L).answer("정답2").isMain(false).build(),
			ShortAnswerDto.builder().number(2L).answer("정답3").isMain(true).build()
		);

		ShortQuestionEntity question = mock(ShortQuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		// when
		List<ShortAnswerEntity> result = shortQuestionFactory.createShortAnswers(answers, question);

		// then
		assertEquals(3, result.size());
	}

	@Test
	@DisplayName("createShortAnswers - 메인 답안이 없으면 예외 발생")
	void createShortAnswers_NoMainAnswer_ThrowsException() {
		// given
		List<ShortAnswerDto> answers = List.of(
			ShortAnswerDto.builder().number(1L).answer("정답1").isMain(false).build(),
			ShortAnswerDto.builder().number(1L).answer("정답2").isMain(false).build() // 메인 답안 없음
		);

		ShortQuestionEntity question = mock(ShortQuestionEntity.class);
		// Mock 설정 제거 - 예외가 먼저 발생해서 getId() 호출되지 않음

		// when & then
		assertThrows(UserParameterException.class,
			() -> shortQuestionFactory.createShortAnswers(answers, question));
	}

	@Test
	@DisplayName("createShortAnswers - 메인 답안이 여러 개면 예외 발생")
	void createShortAnswers_MultipleMainAnswers_ThrowsException() {
		// given
		List<ShortAnswerDto> answers = List.of(
			ShortAnswerDto.builder().number(1L).answer("정답1").isMain(true).build(),
			ShortAnswerDto.builder().number(1L).answer("정답2").isMain(true).build() // 메인 답안 중복
		);

		ShortQuestionEntity question = mock(ShortQuestionEntity.class);
		// Mock 설정 제거 - 예외가 먼저 발생해서 getId() 호출되지 않음

		// when & then
		assertThrows(UserParameterException.class,
			() -> shortQuestionFactory.createShortAnswers(answers, question));
	}

	@Test
	@DisplayName("deleteSubEntities - 하위 엔티티들을 삭제한다")
	void deleteSubEntities() {
		// given
		ShortQuestionEntity question = mock(ShortQuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		// when
		shortQuestionFactory.deleteSubEntities(question);

		// then
		verify(shortAnswerEntityRepository).deleteAllByShortQuestionId(1L);
	}

	@Test
	@DisplayName("createSubEntities - 하위 엔티티들을 생성하여 저장한다")
	void createSubEntities() {
		// given
		List<ShortAnswerDto> answers = List.of(
			ShortAnswerDto.builder().number(1L).answer("정답1").isMain(true).build(),
			ShortAnswerDto.builder().number(2L).answer("정답2").isMain(true).build()
		);

		ShortQuestionDto questionDto = ShortQuestionDto.builder()
			.content("주관식 문제")
			.shortAnswers(answers)
			.build();

		ShortQuestionEntity question = mock(ShortQuestionEntity.class);

		// when
		shortQuestionFactory.createSubEntities(questionDto, question);

		// then
		verify(shortAnswerEntityRepository).saveAll(any(List.class));
	}
}
