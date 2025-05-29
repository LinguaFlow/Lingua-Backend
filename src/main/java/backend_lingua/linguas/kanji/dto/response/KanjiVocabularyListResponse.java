package backend_lingua.linguas.kanji.dto.response;

import backend_lingua.linguas.kanji.entity.Kanji;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanjiVocabularyListResponse {

    @JsonProperty("book_name")
    private String bookName;

    @JsonProperty("file_name")
    private List<KanjiVocabularyListDto> kanjiDetails;

    public static KanjiVocabularyListResponse from(Kanji kanji) {
        String bookName = kanji.getBookName();
        List<KanjiVocabularyListDto> detailResponses = new ArrayList<>();
        Map<String, Object> bookData = kanji.getBook();
        if (bookData != null && !bookData.isEmpty()) {
            String fileNameKey = bookData.keySet().iterator().next();
            Object fileData = bookData.get(fileNameKey);
            if (fileData instanceof List<?>) {
                List<?> entries = (List<?>) fileData;
                for (Object entry : entries) {
                    if (!(entry instanceof Map)) continue;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> detailMap = (Map<String, Object>) entry;

                    int worldOrder = ((Number) detailMap.getOrDefault("world_order", 0)).intValue();
                    String kanjis = Objects.toString(detailMap.get("kanji"), "");
                    String furigana = Objects.toString(detailMap.get("furigana"), "");
                    String means = Objects.toString(detailMap.get("means"), ""); // 일부 항목은 값이 누락됨
                    String level = Objects.toString(detailMap.get("level"), "");
                    int page = ((Number) detailMap.getOrDefault("page", 0)).intValue();

                    detailResponses.add(
                            KanjiVocabularyListDto.builder()
                                    .vocabularyBookOrder(worldOrder)
                                    .kanji(kanjis)
                                    .furigana(furigana)
                                    .means(means)
                                    .level(level)
                                    .page(page)
                                    .build()
                    );
                }
            }
        }

        return KanjiVocabularyListResponse.builder()
                .bookName(bookName)
                .kanjiDetails(detailResponses)
                .build();
    }
}