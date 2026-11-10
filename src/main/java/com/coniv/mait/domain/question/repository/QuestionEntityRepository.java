package com.coniv.mait.domain.question.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.enums.QuestionType;

import jakarta.persistence.LockModeType;

public interface QuestionEntityRepository extends JpaRepository<QuestionEntity, Long> {
	List<QuestionEntity> findAllByQuestionSetId(final Long questionSetId);

	// MySQL의 대소문자 비구분 collation과 무관하게 키 생성과 같은 비교 기준을 사용한다.
	default List<QuestionEntity> findAllByQuestionSetIdOrderByLexoRankAsc(final Long questionSetId) {
		return findAllByQuestionSetId(questionSetId).stream()
			.sorted(DeliveryMode.MAKING.questionComparator()).toList();
	}

	// 현재 읽기로 잠금 대기 전에 만들어진 스냅샷의 순서 키를 재사용하지 않는다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT q FROM QuestionEntity q WHERE q.questionSet.id = :questionSetId ORDER BY q.id")
	List<QuestionEntity> findAllByQuestionSetIdForUpdate(@Param("questionSetId") Long questionSetId);

	long countByQuestionSetId(Long questionSetId);

	Optional<QuestionEntity> findFirstByQuestionSetAndQuestionStatusIn(
		QuestionSetEntity questionSet,
		List<QuestionStatusType> statuses
	);

	List<QuestionEntity> findAllByQuestionSetIdAndQuestionStatusIn(Long questionSetId,
		List<QuestionStatusType> statuses);

	Optional<QuestionEntity> findFirstByQuestionSetOrderByNumberAsc(QuestionSetEntity questionSet);

	@Query(value = "SELECT * FROM questions WHERE question_type = :#{#questionType.name().toLowerCase()}",
		nativeQuery = true)
	List<QuestionEntity> findAllByQuestionType(@Param("questionType") QuestionType questionType);

	List<QuestionEntity> findAllByQuestionSetIdIn(List<Long> questionSetIds);

	@Modifying(clearAutomatically = true)
	@Query("DELETE FROM QuestionEntity q WHERE q.questionSet.id = :questionSetId")
	void deleteAllByQuestionSetId(@Param("questionSetId") Long questionSetId);
}
