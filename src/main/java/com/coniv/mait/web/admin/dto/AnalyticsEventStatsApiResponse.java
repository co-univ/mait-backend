package com.coniv.mait.web.admin.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.coniv.mait.domain.admin.service.dto.AnalyticsEventStatsDto;
import com.coniv.mait.domain.admin.service.dto.AnalyticsEventStepCountDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "feature별 이벤트 통계 응답. event_name별 발생 수와 step 분포를 담는다.")
public record AnalyticsEventStatsApiResponse(
	@Schema(description = "feature 식별 키", example = "onboarding", requiredMode = Schema.RequiredMode.REQUIRED)
	String featureKey,

	@Schema(description = "전체 이벤트 발생 수", requiredMode = Schema.RequiredMode.REQUIRED)
	long totalCount,

	@Schema(description = "event_name별 통계 목록 (event_name 오름차순)", requiredMode = Schema.RequiredMode.REQUIRED)
	List<EventStat> events
) {
	public static AnalyticsEventStatsApiResponse from(final AnalyticsEventStatsDto stats) {
		Map<String, List<StepCount>> stepsByEvent = new LinkedHashMap<>();
		long totalCount = 0;
		for (AnalyticsEventStepCountDto row : stats.stepCounts()) {
			stepsByEvent.computeIfAbsent(row.eventName(), key -> new ArrayList<>())
				.add(new StepCount(row.step(), row.count()));
			totalCount += row.count();
		}

		List<EventStat> events = stepsByEvent.entrySet().stream()
			.map(entry -> new EventStat(
				entry.getKey(),
				entry.getValue().stream().mapToLong(StepCount::count).sum(),
				entry.getValue()))
			.toList();

		return new AnalyticsEventStatsApiResponse(stats.featureKey(), totalCount, events);
	}

	@Schema(description = "단일 event_name에 대한 통계")
	public record EventStat(
		@Schema(description = "이벤트 이름", example = "player_set_list_exit", requiredMode = Schema.RequiredMode.REQUIRED)
		String eventName,

		@Schema(description = "해당 이벤트 총 발생 수", requiredMode = Schema.RequiredMode.REQUIRED)
		long count,

		@Schema(description = "step별 발생 수 분포 (step 오름차순)", requiredMode = Schema.RequiredMode.REQUIRED)
		List<StepCount> steps
	) {
	}

	@Schema(description = "특정 step의 발생 수")
	public record StepCount(
		@Schema(description = "step 값", requiredMode = Schema.RequiredMode.REQUIRED)
		int step,

		@Schema(description = "해당 step 발생 수", requiredMode = Schema.RequiredMode.REQUIRED)
		long count
	) {
	}
}
