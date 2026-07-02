package com.coniv.mait.domain.admin.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
	name = "analytics_event",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_analytics_event",
		columnNames = {"feature_key", "event_name", "user_id", "session_id"}
	),
	indexes = @Index(name = "idx_analytics_event_name_time", columnList = "event_name, occurred_at")
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalyticsEventEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "feature_key", nullable = false, length = 100)
	private String featureKey;

	@Column(name = "event_name", nullable = false, length = 100)
	private String eventName;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "session_id", nullable = false, length = 36, columnDefinition = "char(36)")
	private String sessionId;

	@Column(name = "step", nullable = false)
	private int step;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	@Column(name = "metadata", columnDefinition = "json")
	private String metadata;

	@Builder
	private AnalyticsEventEntity(String featureKey, String eventName, Long userId, String sessionId,
		int step, LocalDateTime occurredAt, String metadata) {
		this.featureKey = featureKey;
		this.eventName = eventName;
		this.userId = userId;
		this.sessionId = sessionId;
		this.step = step;
		this.occurredAt = occurredAt;
		this.metadata = metadata;
	}
}
