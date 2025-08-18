package backend_lingua.linguas.domain.kanji.dto.response;


import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@JsonPropertyOrder({"vocabulary_book_order", "kanji", "furigana", "means" , "level" , "page"})
public class KanjiVocabularyList {

    private int vocabularyBookOrder;

    private String kanji;

    private String furigana;

    private String means;

    private String level;

    private int page;
}
