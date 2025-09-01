package backend_lingua.linguas.domain.vocabulary.entity;

import backend_lingua.linguas.domain.vocabulary.enumerated.TaskStatus;
import backend_lingua.linguas.domain.member.entity.Member;
import backend_lingua.linguas.global.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "vocabulary_word")
@Getter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class VocabularyWord extends BaseEntity {

    private String bookName;

    private String s3Key; // S3 키 저장용 필드

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String,Object> book = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public void completeProcessing(Map<String, Object> bookData) {
        this.book = bookData;
        this.status = TaskStatus.DONE;
    }
}