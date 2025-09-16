package backend_lingua.linguas.domain.vocabulary.repository;

import backend_lingua.linguas.domain.vocabulary.dto.UploadTaskStatusResponse;
import backend_lingua.linguas.domain.vocabulary.enumerated.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;


import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VocabularyWordJdbcRepository {

    private final JdbcClient jdbcClient;

    private RowMapper<UploadTaskStatusResponse> rowMapper = (rs, rowNum) ->
            UploadTaskStatusResponse.builder()
            .taskId(rs.getLong("id"))
            .fileName(rs.getString("book_name"))
            .status(TaskStatus.fromStatus(rs.getString("status")))
            .build();

    public Optional<UploadTaskStatusResponse> findById(Long id) {
        return jdbcClient.sql("SELECT voca.id, voca.book_name, voca.status FROM vocabulary_word voca WHERE id = ?")
                .params(id)
                .query(rowMapper)
                .optional();
    }
}
