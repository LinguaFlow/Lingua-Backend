package backend_lingua.linguas.domain.kanji.service;

import backend_lingua.linguas.domain.member.entity.Member;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    Long createKanjiTask(Member member, MultipartFile file);

    void processKanjiData(String s3Key, Object kanjiDetails);

}
