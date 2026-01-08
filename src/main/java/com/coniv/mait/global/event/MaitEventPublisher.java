package com.coniv.mait.global.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaitEventPublisher {

	private final ApplicationEventPublisher applicationEventPublisher;

	public void publishEvent(final MaitEvent maitEvent) {
		log.info("[Publish Event] name: {}", maitEvent.getClass().getSimpleName());
		applicationEventPublisher.publishEvent(maitEvent);
	}
}
