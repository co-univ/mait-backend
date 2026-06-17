package com.coniv.mait.domain.solve.service.component;

public interface ScorerProcessor {

	Long getScorer(final Long questionId, final Long userId, final Long submitOrder);

	void setScorer(final Long questionId, final Long userId, final Long submitOrder);

	void clearScorer(final Long questionId);
}
