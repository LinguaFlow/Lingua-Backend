package backend_lingua.linguas.domain.vocabulary.event;

import backend_lingua.linguas.domain.vocabulary.enumerated.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class UploadStatusEvent {
    private Long taskId;
    private TaskStatus status;
    private LocalDateTime timestamp;

}
