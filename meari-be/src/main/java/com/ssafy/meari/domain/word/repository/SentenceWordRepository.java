package com.ssafy.meari.domain.word.repository;

import com.ssafy.meari.domain.word.entity.SentenceWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SentenceWordRepository extends JpaRepository<SentenceWord, Long> {

	List<SentenceWord> findBySentenceSentenceIdOrderBySequence(Long sentenceId);

	void deleteBySentenceSentenceId(Long sentenceId);
}
