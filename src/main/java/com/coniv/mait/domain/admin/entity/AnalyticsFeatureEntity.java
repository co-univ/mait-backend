package com.coniv.mait.domain.admin.entity;

import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Table(
	name = "analytics_feature",
	uniqueConstraints = @UniqueConstraint(name = "uk_analytics_feature_key", columnNames = "feature_key")
)
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalyticsFeatureEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "feature_key", nullable = false, length = 100)
	private String featureKey;

	public static AnalyticsFeatureEntity of(final String featureKey) {
		return AnalyticsFeatureEntity.builder()
			.featureKey(featureKey)
			.build();
	}
}
