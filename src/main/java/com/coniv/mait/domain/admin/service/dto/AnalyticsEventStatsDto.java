package com.coniv.mait.domain.admin.service.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.coniv.mait.domain.admin.entity.AnalyticsEventEntity;

public record AnalyticsEventStatsDto(String featureKey, List<AnalyticsEventStepCountDto> stepCounts) {

	public static AnalyticsEventStatsDto of(final String featureKey, final List<AnalyticsEventEntity> events) {
		Map<String, Map<Integer, Long>> countByEventNameAndStep = new TreeMap<>();
		for (AnalyticsEventEntity event : events) {
			countByEventNameAndStep
				.computeIfAbsent(event.getEventName(), key -> new TreeMap<>())
				.merge(event.getStep(), 1L, Long::sum);
		}

		List<AnalyticsEventStepCountDto> stepCounts = new ArrayList<>();
		countByEventNameAndStep.forEach((eventName, countByStep) ->
			countByStep.forEach((step, count) ->
				stepCounts.add(new AnalyticsEventStepCountDto(eventName, step, count))));
		return new AnalyticsEventStatsDto(featureKey, stepCounts);
	}
}
