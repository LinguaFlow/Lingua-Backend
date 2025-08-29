package backend_lingua.linguas.domain.kanji.repository;

import backend_lingua.linguas.domain.kanji.entity.File;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface FileRepository extends JpaRepository<File, Long> {

    Optional<File> findByS3Key(String s3Key);

}
