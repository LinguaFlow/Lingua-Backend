package backend_lingua.linguas.domain.vocabulary.event;

import backend_lingua.linguas.domain.vocabulary.enumerated.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadStatusEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishStatusUpdate(Long taskId, TaskStatus status) {

        UploadStatusEvent event = UploadStatusEvent.builder().taskId(taskId).status(status).build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published status update event - Task ID: {}, Status: {}", taskId, status);
    }
}
