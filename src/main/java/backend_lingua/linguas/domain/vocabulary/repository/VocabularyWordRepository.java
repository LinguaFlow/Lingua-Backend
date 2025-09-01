package backend_lingua.linguas.domain.vocabulary.repository;

import backend_lingua.linguas.domain.vocabulary.entity.VocabularyWord;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface VocabularyWordRepository extends JpaRepository<VocabularyWord, Long> {

    Optional<VocabularyWord> findByS3Key(String s3Key);

}
