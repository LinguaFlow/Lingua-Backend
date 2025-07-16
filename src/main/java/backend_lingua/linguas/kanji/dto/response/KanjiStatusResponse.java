package backend_lingua.linguas.kanji.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KanjiStatusResponse {
    private final String status;
    private final String code;
    private final String message;

    public static KanjiStatusResponse of(String status, String code, String message) {
        return KanjiStatusResponse.builder()
                .status(status)
                .code(code)
                .message(message)
                .build();
    }
}