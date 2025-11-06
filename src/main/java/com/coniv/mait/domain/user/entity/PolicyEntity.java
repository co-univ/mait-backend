package com.coniv.mait.domain.user.entity;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PolicyEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	@ColumnDefault(value = "'ESSENTIAL'")
	private PolicyType policyType;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private PolicyCategory category;

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
