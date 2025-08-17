package backend_lingua.linguas.oauth.dto;

import backend_lingua.linguas.security.dto.response.MemberInfo;
import backend_lingua.linguas.security.dto.response.TokenInfo;
import backend_lingua.linguas.security.principal.UserPrincipal;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;


import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private TokenInfo tokenInfo;
    private MemberInfo userInfo;
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // User 엔티티와 토큰 정보로 생성
    public static LoginResponse of(MemberInfo memberInfo, TokenInfo tokenInfo) {
        return LoginResponse.builder()
                .tokenInfo(tokenInfo)
                .userInfo(memberInfo)
                .message("로그인 성공")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
