package backend_lingua.linguas.security.dto.response;

import backend_lingua.linguas.member.entity.Member;
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
    public static LoginResponse of(Member member, TokenInfo tokenInfo) {
        return LoginResponse.builder()
                .tokenInfo(tokenInfo)
                .userInfo(MemberInfo.from(member))
                .message("로그인 성공")
                .timestamp(LocalDateTime.now())
                .build();
    }

    // UserPrincipal과 토큰 정보로 생성 (DB 조회 없이)
    public static LoginResponse of(UserPrincipal principal, TokenInfo tokenInfo) {
        return LoginResponse.builder()
                .tokenInfo(tokenInfo)
                .userInfo(MemberInfo.from(principal))
                .message("로그인 성공")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
