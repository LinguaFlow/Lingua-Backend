package backend_lingua.linguas.domain.example.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HomonymSentencesResponse {
    private String word;
    private String level;
}
