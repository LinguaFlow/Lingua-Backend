package backend_lingua.linguas.domain.kanji.dto.response;


import backend_lingua.linguas.domain.kanji.enumerated.TaskStatus;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class FileUploadResponse {

    private final Long taskId;

    private final String fileName;

    private final TaskStatus status;

}
