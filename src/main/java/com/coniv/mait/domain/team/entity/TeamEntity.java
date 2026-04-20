package com.coniv.mait.domain.team.entity;

import java.time.LocalDateTime;

import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "teams")
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private Long creatorId;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	private TeamEntity(String name, Long creatorId) {
		this.name = name;
		this.creatorId = creatorId;
	}

	public static TeamEntity of(String name, Long creatorId) {
		return new TeamEntity(name, creatorId);
	}

	public void updateDeletedAt(final LocalDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}

	public boolean deleted() {
		return deletedAt != null;
	}
}
