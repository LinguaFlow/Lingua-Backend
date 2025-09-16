package backend_lingua.linguas.domain.vocabulary.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
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