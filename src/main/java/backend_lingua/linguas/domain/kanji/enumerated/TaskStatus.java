package backend_lingua.linguas.domain.kanji.enumerated;

import backend_lingua.linguas.domain.kanji.dto.response.KanjiVocabularyListResponse;
import backend_lingua.linguas.domain.kanji.entity.File;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
public enum TaskStatus {

    PENDING("PENDING", "P001") {
        @Override
        public String getMessage(File file) {
            return "파일이 업로드되어 처리 대기 중입니다.";
        }

        @Override
        public ResponseEntity<KanjiVocabularyListResponse> buildResultResponse(File file) {
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(null); // PENDING 상태에서는 결과 데이터 없음
        }
    },

    PROCESSING("PROCESSING", "P002") {
        @Override
        public String getMessage(File file) {
            return "파일을 처리 중입니다.";
        }

        @Override
        public ResponseEntity<KanjiVocabularyListResponse> buildResultResponse(File file) {
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(null);
        }
    },

    DONE("DONE", "D001") {
        @Override
        public String getMessage(File file) {
            return "처리가 완료되었습니다. /result 엔드포인트에서 결과를 확인할 수 있습니다.";
        }

        @Override
        public ResponseEntity<KanjiVocabularyListResponse> buildResultResponse(File file) {
            return ResponseEntity
                    .ok()
                    .body(KanjiVocabularyListResponse.from(file));
        }
    },

    FAILED("FAILED", "F001") {
        @Override
        public String getMessage(File file) {
            return "처리 중 오류가 발생했습니다: " + file.getErrorMessage();
        }

        @Override
        public ResponseEntity<KanjiVocabularyListResponse> buildResultResponse(File file) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    };

    private final String status;
    private final String code;

    TaskStatus(String status, String code) {
        this.status = status;
        this.code = code;
    }

    public abstract String getMessage(File file);
    public abstract ResponseEntity<KanjiVocabularyListResponse> buildResultResponse(File file);

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