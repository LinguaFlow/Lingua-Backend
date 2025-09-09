package backend_lingua.linguas.domain.vocabulary.enumerated;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskStatus {

    PENDING("PENDING", "P001" ,true) ,

    PROCESSING("PROCESSING", "P002", true),

    DONE("DONE", "D001" , false),

    FAILED("FAILED", "F001" , false);

    private final String status;

    private final String code;

    private final boolean cancellable;

    public boolean canBeProcessed() {
        return this == PENDING || this == PROCESSING;
    }

    public static TaskStatus fromStatus(String status) {
        for (TaskStatus taskStatus : values()) {
            if (taskStatus.status.equalsIgnoreCase(status)) {
                return taskStatus;
            }
        }
        throw new IllegalArgumentException("존재하지 않는 상태입니다: " + status);
    }
}