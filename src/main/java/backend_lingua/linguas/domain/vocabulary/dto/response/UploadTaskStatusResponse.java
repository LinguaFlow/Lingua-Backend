package backend_lingua.linguas.domain.vocabulary.dto.response;


import backend_lingua.linguas.domain.vocabulary.enumerated.TaskStatus;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class UploadTaskStatusResponse {

    private final Long taskId;

    private final String fileName;

    private final TaskStatus status;

}
