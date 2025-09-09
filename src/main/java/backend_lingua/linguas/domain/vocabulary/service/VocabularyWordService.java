package backend_lingua.linguas.domain.vocabulary.service;

import backend_lingua.linguas.domain.member.entity.Member;
import backend_lingua.linguas.domain.vocabulary.dto.response.KanjiVocabularyListResponse;
import backend_lingua.linguas.domain.vocabulary.dto.response.UploadTaskStatusResponse;
import org.springframework.web.multipart.MultipartFile;

public interface VocabularyWordService {

    UploadTaskStatusResponse uploadFile(Member member, MultipartFile file);

    UploadTaskStatusResponse uploadTaskStatus(Long fileId);

    KanjiVocabularyListResponse getCompletedTask(Long fileId);

    void cancelUpload(Member member, Long fileId);
}
