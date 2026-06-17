package com.coniv.mait.domain.statistic.service.dto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionAnswerDistributionDto {

	private final Long questionSetId;
	private final QuestionDto question;
	private final long totalSubmitCount;
	private final long submittedUserCount;
	private final long correctUserCount;
	private final Double correctRate;
	private final List<SubmittedAnswerGroupDto> groups;

	public static QuestionAnswerDistributionDto of(final Long questionSetId, final QuestionDto question,
		final List<AnswerSubmitRecordEntity> records, final boolean firstSubmitOnly) {
		// 정답률/유저 카운트는 항상 유저당 최초 제출 기준 (기존 오답률 통계와 동일 기준)
		Map<Long, AnswerSubmitRecordEntity> firstSubmitByUser = records.stream()
			.collect(Collectors.groupingBy(
				AnswerSubmitRecordEntity::getUserId,
				Collectors.collectingAndThen(Collectors.toList(), QuestionAnswerDistributionDto::pickEarliest)));

		long submittedUserCount = firstSubmitByUser.size();
		long correctUserCount = firstSubmitByUser.values().stream()
			.filter(AnswerSubmitRecordEntity::isCorrect)
			.count();

		// firstSubmitOnly=true 면 유저당 최초 제출만, 아니면 전체 제출 기록을 분포 대상으로 삼는다.
		List<AnswerSubmitRecordEntity> targetRecords = firstSubmitOnly
			? new ArrayList<>(firstSubmitByUser.values())
			: records;

		Map<String, List<AnswerSubmitRecordEntity>> recordsByAnswer = targetRecords.stream()
			.collect(Collectors.groupingBy(
				AnswerSubmitRecordEntity::getSubmittedAnswer,
				LinkedHashMap::new,
				Collectors.toList()));

		List<SubmittedAnswerGroupDto> groups = recordsByAnswer.entrySet().stream()
			.map(entry -> SubmittedAnswerGroupDto.of(entry.getKey(), entry.getValue()))
			.sorted(Comparator.comparingLong(SubmittedAnswerGroupDto::getSubmitCount).reversed())
			.toList();

		return QuestionAnswerDistributionDto.builder()
			.questionSetId(questionSetId)
			.question(question)
			.totalSubmitCount(targetRecords.size())
			.submittedUserCount(submittedUserCount)
			.correctUserCount(correctUserCount)
			.correctRate(calculateCorrectRate(submittedUserCount, correctUserCount))
			.groups(groups)
			.build();
	}

	private static AnswerSubmitRecordEntity pickEarliest(final List<AnswerSubmitRecordEntity> records) {
		return records.stream()
			.min(Comparator.comparing(AnswerSubmitRecordEntity::getSubmitOrder,
				Comparator.nullsFirst(Comparator.naturalOrder())))
			.orElseThrow();
	}

	private static Double calculateCorrectRate(final long submittedUserCount, final long correctUserCount) {
		if (submittedUserCount == 0) {
			return null;
		}
		// 소수점 첫째 자리까지 반올림
		return Math.round((double)correctUserCount / submittedUserCount * 1000) / 10.0;
	}
}
