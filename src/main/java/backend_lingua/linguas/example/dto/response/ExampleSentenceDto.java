package backend_lingua.linguas.example.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExampleSentenceDto {
    @JsonProperty("japanese_example")
    private String japaneseExample;

    @JsonProperty("korean_translation")
    private String koreanTranslation;
}
