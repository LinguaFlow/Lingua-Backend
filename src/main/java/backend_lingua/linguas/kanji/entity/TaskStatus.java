package backend_lingua.linguas.kanji.entity;

import backend_lingua.linguas.kanji.dto.response.KanjiVocabularyListResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


@Getter
public enum TaskStatus {

    PENDING("PENDING", "P001") {
        @Override
        public String getMessage(Kanji kanji) {
            return "파일이 업로드되어 처리 대기 중입니다.";
        }

        @Override
        public ResponseEntity<KanjiVocabularyListResponse> buildResultResponse(Kanji kanji) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(null); // PENDING 상태에서는 결과 데이터 없음
        }
    },

    PROCESSING("PROCESSING", "P002") {
        @Override
        public String getMessage(Kanji kanji) {
            return "파일을 처리 중입니다.";
        }

        @Override
        public ResponseEntity<KanjiVocabularyListResponse> buildResultResponse(Kanji kanji) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(null); // PROCESSING 상태에서는 결과 데이터 없음
        }
    },

    DONE("DONE", "D001") {
        @Override
        public String getMessage(Kanji kanji) {
            return "처리가 완료되었습니다. /result 엔드포인트에서 결과를 확인할 수 있습니다.";
        }

        @Override
        public ResponseEntity<KanjiVocabularyListResponse> buildResultResponse(Kanji kanji) {
            return ResponseEntity.ok()
                    .body(KanjiVocabularyListResponse.from(kanji));
        }
    },

    FAILED("FAILED", "F001") {
        @Override
        public String getMessage(Kanji kanji) {
            return "처리 중 오류가 발생했습니다: " + kanji.getErrorMessage();
        }

        @Override
        public ResponseEntity<KanjiVocabularyListResponse> buildResultResponse(Kanji kanji) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null); // FAILED 상태에서는 결과 데이터 없음
        }
    };

    // 수동으로 getter 메서드 작성 (Lombok @Getter 제거)
    private final String status;
    private final String code;

    TaskStatus(String status, String code) {
        this.status = status;
        this.code = code;
    }

    // Abstract 메서드들 - 각 상태별로 구현 필요
    public abstract String getMessage(Kanji kanji);
    public abstract ResponseEntity<KanjiVocabularyListResponse> buildResultResponse(Kanji kanji);

    // 상태 그룹 관리 메서드들 (우아한형제들 3번 사례 적용)
    public boolean isProcessing() {
        return this == PENDING || this == PROCESSING;
    }

    public boolean isCompleted() {
        return this == DONE || this == FAILED;
    }

    public boolean isSuccessful() {
        return this == DONE;
    }

    public boolean isFailed() {
        return this == FAILED;
    }

    // 상태 전환 검증 메서드
    public boolean canTransitionTo(TaskStatus nextStatus) {
        return switch (this) {
            case PENDING -> nextStatus == PROCESSING || nextStatus == FAILED;
            case PROCESSING -> nextStatus == DONE || nextStatus == FAILED;
            case DONE, FAILED -> false; // 완료/실패 상태에서는 전환 불가
        };
    }

    public void validateTransition(TaskStatus nextStatus) {
        if (!canTransitionTo(nextStatus)) {
            throw new IllegalStateException(
                    String.format("상태 전환이 불가능합니다. [%s] -> [%s]",
                            this.status, nextStatus.status)
            );
        }
    }

    // 정적 팩토리 메서드들
    public static TaskStatus fromCode(String code) {
        for (TaskStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("존재하지 않는 코드입니다: " + code);
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