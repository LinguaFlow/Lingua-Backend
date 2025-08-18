package backend_lingua.linguas.infrastructure.security.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;


@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenInfo {
    private String accessToken;
    private Long accessTokenExpiresIn;
    private String refreshToken;
    private Long refreshTokenExpiresIn;

    @Builder.Default
    private String tokenType = "Bearer";

    // JwtResponse를 TokenInfo로 변환
    public static TokenInfo from(String accessToken, String refreshToken, Long accessTokenExp, Long refreshTokenExp) {
        return TokenInfo.builder()
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExp)
                .refreshToken(refreshToken)
                .refreshTokenExpiresIn(refreshTokenExp)
                .tokenType("Bearer")
                .build();
    }
}
