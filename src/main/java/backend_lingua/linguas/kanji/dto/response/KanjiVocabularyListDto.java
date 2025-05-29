package backend_lingua.linguas.kanji.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanjiVocabularyListDto {

    @JsonProperty("vocabulary_book_order")
    private int vocabularyBookOrder;

    @JsonProperty("kanji")
    private String kanji;

    @JsonProperty("furigana")
    private String furigana;

    @JsonProperty("means")
    private String means;

    @JsonProperty("level")
    private String level;

    @JsonProperty("page")
    private int page;
}
