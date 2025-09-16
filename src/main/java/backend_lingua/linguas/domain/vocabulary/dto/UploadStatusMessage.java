package backend_lingua.linguas.domain.vocabulary.dto;

import backend_lingua.linguas.domain.vocabulary.enumerated.TaskStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UploadStatusMessage {
    private Long taskId;
    private TaskStatus status;
    private LocalDateTime timestamp;
}
