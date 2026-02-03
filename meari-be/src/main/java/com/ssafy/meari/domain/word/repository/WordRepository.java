package com.ssafy.meari.domain.word.repository;

import com.ssafy.meari.domain.word.entity.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {

    @Query(value = "SELECT * FROM word ORDER BY RANDOM() LIMIT 10", nativeQuery = true)
    List<Word> findRandomWordsLimit10();

    List<Word> findAllByWordKr(String wordKr);
}
