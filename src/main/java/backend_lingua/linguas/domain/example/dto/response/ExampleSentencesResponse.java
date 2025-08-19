package backend_lingua.linguas.domain.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExampleSentencesResponse {
    private List<ExampleSentenceDto> examples;
}
