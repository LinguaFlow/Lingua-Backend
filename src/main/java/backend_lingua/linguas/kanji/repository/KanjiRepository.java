package backend_lingua.linguas.kanji.repository;

import backend_lingua.linguas.kanji.entity.Kanji;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface KanjiRepository extends JpaRepository<Kanji, Long> {
    Optional<Kanji> findByS3Key(String s3Key);
}
