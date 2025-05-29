package backend_lingua.linguas.kanji.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.HashMap;
import java.util.Map;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Kanji {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private String bookName;

    private String s3Key; // S3 키 저장용 필드

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String,Object> book = new HashMap<>();

    public void completeProcessing(Map<String, Object> bookData) {
        this.book = bookData;
        this.status = TaskStatus.DONE;
    }
}