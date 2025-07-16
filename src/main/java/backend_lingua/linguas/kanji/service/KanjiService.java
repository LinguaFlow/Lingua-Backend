package backend_lingua.linguas.kanji.service;


import org.springframework.web.multipart.MultipartFile;

public interface KanjiService {

    Long createKanjiTask(MultipartFile file);

    void processKanjiData(String s3Key, Object kanjiDetails);

}
