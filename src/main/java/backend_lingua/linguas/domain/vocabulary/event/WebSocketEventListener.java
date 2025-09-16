package backend_lingua.linguas.domain.vocabulary.event;

import backend_lingua.linguas.domain.vocabulary.dto.UploadStatusMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @EventListener
    public void handleUploadStatusEvent(UploadStatusEvent event) {
        UploadStatusMessage message = UploadStatusMessage.builder()
                .taskId(event.getTaskId())
                .status(event.getStatus())
                .timestamp(event.getTimestamp())
                .build();

        String destination = "/topic/upload/" + event.getTaskId();
        messagingTemplate.convertAndSend(destination, message);

        log.info("WebSocket 알림 전송 - Task ID: {}, Status: {}, Destination: {}",
                event.getTaskId(), event.getStatus(), destination);
    }
}
