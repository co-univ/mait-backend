package com.coniv.mait.domain.user.entity;

import com.coniv.mait.domain.user.enums.PolicyCategory;
import com.coniv.mait.domain.user.enums.PolicyTiming;
import com.coniv.mait.domain.user.enums.PolicyType;
import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
	name = "policies",
	indexes = {
		@Index(name = "idx_policy_version_latest", columnList = "code, version DESC")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_policy_version_unique", columnNames = {"code", "version"})
	}
)
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PolicyEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private PolicyType policyType = PolicyType.ESSENTIAL;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private PolicyCategory category;

	@Column(nullable = false)
	private Integer version;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(nullable = false)
	private String code;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private PolicyTiming timing;

	@Column(nullable = false)
	private String title;

	@Builder
	public PolicyEntity(PolicyType policyType, PolicyCategory category, PolicyTiming timing, String title) {
		this.policyType = policyType;
		this.category = category;
		this.timing = timing;
		this.title = title;
	}
}
