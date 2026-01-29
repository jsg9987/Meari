package com.ssafy.meari.domain.word.repository;

import com.ssafy.meari.domain.word.entity.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {

	List<Word> findAllByWordKr(String wordKr);
}
