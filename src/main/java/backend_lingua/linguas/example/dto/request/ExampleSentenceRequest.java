package backend_lingua.linguas.example.dto.request;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor  // Jackson 역직렬화용 기본 생성자
@AllArgsConstructor  // 모든 필드를 인자로 받는 생성자
public class ExampleSentenceRequest {
    private String word;
    private String level;
}
