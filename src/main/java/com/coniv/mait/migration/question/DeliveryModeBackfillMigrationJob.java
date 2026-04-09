package com.coniv.mait.migration.question;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.migration.MigrationJob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile({"dev", "local", "prod"})
@Component
@RequiredArgsConstructor
public class DeliveryModeBackfillMigrationJob implements MigrationJob {

	private final QuestionSetEntityRepository questionSetEntityRepository;

	@Transactional
	@Override
	public void migrate() {
		List<QuestionSetEntity> all = questionSetEntityRepository.findAll();

		int solveModeCount = 0;
		int reviewCount = 0;

		for (QuestionSetEntity questionSet : all) {
			DeliveryMode deliveryMode = questionSet.getDeliveryMode();
			if (deliveryMode == null) {
				continue;
			}

			if (deliveryMode == DeliveryMode.LIVE_TIME || deliveryMode == DeliveryMode.STUDY) {
				questionSet.backfillSolveMode(QuestionSetSolveMode.valueOf(deliveryMode.name()));
				solveModeCount++;
			} else if (deliveryMode == DeliveryMode.REVIEW) {
				questionSet.backfillStatus(QuestionSetStatus.REVIEW);
				reviewCount++;
			}
		}

		log.info("[{}] solve_mode 백필: {}건, status REVIEW 백필: {}건", getName(), solveModeCount, reviewCount);
	}
}
