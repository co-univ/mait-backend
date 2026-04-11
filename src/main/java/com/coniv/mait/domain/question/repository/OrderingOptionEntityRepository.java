package com.coniv.mait.domain.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.question.entity.OrderingOptionEntity;

public interface OrderingOptionEntityRepository extends JpaRepository<OrderingOptionEntity, Long> {

	List<OrderingOptionEntity> findAllByOrderingQuestionId(Long orderingQuestionId);

	@Modifying
	@Query("DELETE FROM OrderingOptionEntity o WHERE o.orderingQuestionId = :questionId")
	void deleteBulkAllByQuestionId(@Param("questionId") Long questionId);

	@Modifying
	@Query("DELETE FROM OrderingOptionEntity o WHERE o.orderingQuestionId IN :questionIds")
	void deleteAllByOrderingQuestionIdIn(@Param("questionIds") List<Long> questionIds);
}
