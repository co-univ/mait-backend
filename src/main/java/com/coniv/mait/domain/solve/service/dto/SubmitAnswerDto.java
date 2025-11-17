package com.coniv.mait.domain.solve.service.dto;

import java.util.List;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.coniv.mait.domain.question.constant.QuestionConstant;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
	@JsonSubTypes.Type(value = ShortQuestionSubmitAnswer.class, name = QuestionConstant.SHORT),
	@JsonSubTypes.Type(value = MultipleQuestionSubmitAnswer.class, name = QuestionConstant.MULTIPLE),
	@JsonSubTypes.Type(value = OrderingQuestionSubmitAnswer.class, name = QuestionConstant.ORDERING),
	@JsonSubTypes.Type(value = FillBlankQuestionSubmitAnswer.class, name = QuestionConstant.FILL_BLANK)
})
public interface SubmitAnswerDto<T> {

	@Schema(enumAsRef = true)
	QuestionType getType();

	List<T> getSubmitAnswers();

	ObjectMapper MAPPER = Jackson2ObjectMapperBuilder.json().build();

	static SubmitAnswerDto<?> fromJson(String json) {
		try {
			return MAPPER.readValue(json, new TypeReference<SubmitAnswerDto<?>>() {
			});
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("제출 답안을 파싱할 수 없습니다.", e);
		}
	}
}
