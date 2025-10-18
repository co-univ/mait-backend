package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionValidationResult;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;

@ExtendWith(MockitoExtension.class)
class MultipleQuestionValidatorTest {

    @InjectMocks
    private MultipleQuestionValidator multipleQuestionValidator;

    @Mock
    private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

    @Test
    @DisplayName("getQuestionType - MULTIPLE 타입 반환")
    void getQuestionType() {
        // when
        QuestionType result = multipleQuestionValidator.getQuestionType();

        // then
        assertThat(result).isEqualTo(QuestionType.MULTIPLE);
    }

    @Test
    @DisplayName("validate - 유효한 객관식 문제 검증 성공")
    void validate_ValidQuestion_ReturnsValid() {
        // given
        MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);
        when(question.getId()).thenReturn(1L);
        when(question.getNumber()).thenReturn(1L);
        when(question.getContent()).thenReturn("객관식 문제 내용");

        MultipleChoiceEntity choice1 = mock(MultipleChoiceEntity.class);
        when(choice1.isCorrect()).thenReturn(true);
        when(choice1.getContent()).thenReturn("정답");

        MultipleChoiceEntity choice2 = mock(MultipleChoiceEntity.class);
        when(choice2.getContent()).thenReturn("오답1");

        MultipleChoiceEntity choice3 = mock(MultipleChoiceEntity.class);
        when(choice3.getContent()).thenReturn("오답2");

        List<MultipleChoiceEntity> choices = List.of(choice1, choice2, choice3);
        when(multipleChoiceEntityRepository.findAllByQuestionId(1L)).thenReturn(choices);

        // when
        QuestionValidateDto result = multipleQuestionValidator.validate(question);

        // then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getQuestionId()).isEqualTo(1L);
        assertThat(result.getNumber()).isEqualTo(1L);
        assertThat(result.getReason()).isNull();
        verify(multipleChoiceEntityRepository).findAllByQuestionId(1L);
    }

    @Test
    @DisplayName("validate - 문제 내용이 null이면 EMPTY_CONTENT 반환")
    void validate_NullContent_ReturnsEmptyContent() {
        // given
        MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);
        when(question.getId()).thenReturn(1L);
        when(question.getNumber()).thenReturn(1L);
        when(question.getContent()).thenReturn(null);

        // when
        QuestionValidateDto result = multipleQuestionValidator.validate(question);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getQuestionId()).isEqualTo(1L);
        assertThat(result.getNumber()).isEqualTo(1L);
        assertThat(result.getReason()).isEqualTo(QuestionValidationResult.EMPTY_CONTENT);
        verify(multipleChoiceEntityRepository, never()).findAllByQuestionId(anyLong());
    }

    @Test
    @DisplayName("validate - 문제 내용이 빈 문자열이면 EMPTY_CONTENT 반환")
    void validate_BlankContent_ReturnsEmptyContent() {
        // given
        MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);
        when(question.getId()).thenReturn(1L);
        when(question.getNumber()).thenReturn(1L);
        when(question.getContent()).thenReturn("   ");

        // when
        QuestionValidateDto result = multipleQuestionValidator.validate(question);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getQuestionId()).isEqualTo(1L);
        assertThat(result.getNumber()).isEqualTo(1L);
        assertThat(result.getReason()).isEqualTo(QuestionValidationResult.EMPTY_CONTENT);
        verify(multipleChoiceEntityRepository, never()).findAllByQuestionId(anyLong());
    }

    @Test
    @DisplayName("validate - 선택지가 1개만 있으면 INVALID_CHOICE_COUNT 반환")
    void validate_OneChoice_ReturnsInvalidChoiceCount() {
        // given
        MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);
        when(question.getId()).thenReturn(1L);
        when(question.getNumber()).thenReturn(1L);
        when(question.getContent()).thenReturn("객관식 문제 내용");

        MultipleChoiceEntity choice = mock(MultipleChoiceEntity.class);

        List<MultipleChoiceEntity> choices = List.of(choice);
        when(multipleChoiceEntityRepository.findAllByQuestionId(1L)).thenReturn(choices);

        // when
        QuestionValidateDto result = multipleQuestionValidator.validate(question);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getQuestionId()).isEqualTo(1L);
        assertThat(result.getNumber()).isEqualTo(1L);
        assertThat(result.getReason()).isEqualTo(QuestionValidationResult.INVALID_CHOICE_COUNT);
        verify(multipleChoiceEntityRepository).findAllByQuestionId(1L);
    }

    @Test
    @DisplayName("validate - 선택지가 9개 이상이면 INVALID_CHOICE_COUNT 반환")
    void validate_NineChoices_ReturnsInvalidChoiceCount() {
        // given
        MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);
        when(question.getId()).thenReturn(1L);
        when(question.getNumber()).thenReturn(1L);
        when(question.getContent()).thenReturn("객관식 문제 내용");

        List<MultipleChoiceEntity> choices = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            MultipleChoiceEntity choice = mock(MultipleChoiceEntity.class);
            choices.add(choice);
        }

        when(multipleChoiceEntityRepository.findAllByQuestionId(1L)).thenReturn(choices);

        // when
        QuestionValidateDto result = multipleQuestionValidator.validate(question);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getQuestionId()).isEqualTo(1L);
        assertThat(result.getNumber()).isEqualTo(1L);
        assertThat(result.getReason()).isEqualTo(QuestionValidationResult.INVALID_CHOICE_COUNT);
        verify(multipleChoiceEntityRepository).findAllByQuestionId(1L);
    }

    @Test
    @DisplayName("validate - 정답이 없으면 NO_CORRECT_CHOICE 반환")
    void validate_NoCorrectChoice_ReturnsNoCorrectChoice() {
        // given
        MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);
        when(question.getId()).thenReturn(1L);
        when(question.getNumber()).thenReturn(1L);
        when(question.getContent()).thenReturn("객관식 문제 내용");

        MultipleChoiceEntity choice1 = mock(MultipleChoiceEntity.class);
        when(choice1.isCorrect()).thenReturn(false);

        MultipleChoiceEntity choice2 = mock(MultipleChoiceEntity.class);
        when(choice2.isCorrect()).thenReturn(false);

        List<MultipleChoiceEntity> choices = List.of(choice1, choice2);
        when(multipleChoiceEntityRepository.findAllByQuestionId(1L)).thenReturn(choices);

        // when
        QuestionValidateDto result = multipleQuestionValidator.validate(question);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getQuestionId()).isEqualTo(1L);
        assertThat(result.getNumber()).isEqualTo(1L);
        assertThat(result.getReason()).isEqualTo(QuestionValidationResult.NO_CORRECT_CHOICE);
        verify(multipleChoiceEntityRepository).findAllByQuestionId(1L);
    }

    @Test
    @DisplayName("validate - 선택지 내용이 빈 문자열이면 EMPTY_CHOICE_CONTENT 반환")
    void validate_BlankChoiceContent_ReturnsEmptyChoiceContent() {
        // given
        MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);
        when(question.getId()).thenReturn(1L);
        when(question.getNumber()).thenReturn(1L);
        when(question.getContent()).thenReturn("객관식 문제 내용");

        MultipleChoiceEntity choice1 = mock(MultipleChoiceEntity.class);
        when(choice1.isCorrect()).thenReturn(true);
        when(choice1.getContent()).thenReturn("정답");

        MultipleChoiceEntity choice2 = mock(MultipleChoiceEntity.class);
        when(choice2.getContent()).thenReturn("   ");

        List<MultipleChoiceEntity> choices = List.of(choice1, choice2);
        when(multipleChoiceEntityRepository.findAllByQuestionId(1L)).thenReturn(choices);

        // when
        QuestionValidateDto result = multipleQuestionValidator.validate(question);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getQuestionId()).isEqualTo(1L);
        assertThat(result.getNumber()).isEqualTo(1L);
        assertThat(result.getReason()).isEqualTo(QuestionValidationResult.EMPTY_CHOICE_CONTENT);
        verify(multipleChoiceEntityRepository).findAllByQuestionId(1L);
    }

    @Test
    @DisplayName("validate - 선택지가 정확히 2개이고 정답이 있으면 유효함")
    void validate_TwoChoicesWithCorrect_ReturnsValid() {
        // given
        MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);
        when(question.getId()).thenReturn(1L);
        when(question.getNumber()).thenReturn(1L);
        when(question.getContent()).thenReturn("객관식 문제 내용");

        MultipleChoiceEntity choice1 = mock(MultipleChoiceEntity.class);
        when(choice1.isCorrect()).thenReturn(true);
        when(choice1.getContent()).thenReturn("정답");

        MultipleChoiceEntity choice2 = mock(MultipleChoiceEntity.class);
        when(choice2.getContent()).thenReturn("오답");

        List<MultipleChoiceEntity> choices = List.of(choice1, choice2);
        when(multipleChoiceEntityRepository.findAllByQuestionId(1L)).thenReturn(choices);

        // when
        QuestionValidateDto result = multipleQuestionValidator.validate(question);

        // then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getQuestionId()).isEqualTo(1L);
        assertThat(result.getNumber()).isEqualTo(1L);
        assertThat(result.getReason()).isNull();
        verify(multipleChoiceEntityRepository).findAllByQuestionId(1L);
    }

    @Test
    @DisplayName("validate - 선택지가 정확히 8개이고 정답이 있으면 유효함")
    void validate_EightChoicesWithCorrect_ReturnsValid() {
        // given
        MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);
        when(question.getId()).thenReturn(1L);
        when(question.getNumber()).thenReturn(1L);
        when(question.getContent()).thenReturn("객관식 문제 내용");

        List<MultipleChoiceEntity> choices = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            MultipleChoiceEntity choice = mock(MultipleChoiceEntity.class);
            if (i == 0) {
                when(choice.isCorrect()).thenReturn(true);
            }
            when(choice.getContent()).thenReturn("선택지" + (i + 1));
            choices.add(choice);
        }

        when(multipleChoiceEntityRepository.findAllByQuestionId(1L)).thenReturn(choices);

        // when
        QuestionValidateDto result = multipleQuestionValidator.validate(question);

        // then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getQuestionId()).isEqualTo(1L);
        assertThat(result.getNumber()).isEqualTo(1L);
        assertThat(result.getReason()).isNull();
        verify(multipleChoiceEntityRepository).findAllByQuestionId(1L);
    }
}
