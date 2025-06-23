package backend_lingua.linguas.kanji.entity;

import lombok.Getter;

@Getter
public enum TaskStatus {
    /**
     * 대기 상태 - 작업이 등록되었지만 아직 처리 시작 전
     */
    PENDING("PENDING", "1"),

    /**
     * 처리 중 - 작업이 현재 진행 중
     */
    PROCESSING("PROCESSING", "2"),

    /**
     * 완료 - 작업이 성공적으로 처리됨
     */
    DONE("DONE", "3"),

    /**
     * 실패 - 작업 처리 중 오류 발생
     */
    FAILED("FAILED", "4");

    private final String status;
    private final String code;

    TaskStatus(String status, String code) {
        this.status = status;
        this.code = code;
    }
}
