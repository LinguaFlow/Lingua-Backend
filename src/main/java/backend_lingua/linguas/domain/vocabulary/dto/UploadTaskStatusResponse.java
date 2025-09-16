package backend_lingua.linguas.domain.vocabulary.dto;


import backend_lingua.linguas.domain.vocabulary.enumerated.TaskStatus;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UploadTaskStatusResponse {

    private Long taskId;

    private String fileName;

    private TaskStatus status;

}
