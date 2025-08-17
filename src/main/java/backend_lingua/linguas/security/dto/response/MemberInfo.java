package backend_lingua.linguas.security.dto.response;

import backend_lingua.linguas.member.entity.Member;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberInfo {

    @Schema(description = "사용자 PK")
    private Long id;

    @Schema(description = "사용자 이메일")
    private String email;

    @Schema(description = "사용자 이름")
    private String name;

    @Schema(description = "사용자 별명")
    private String nickname;

    @Schema(description = "사용자 프로필 이미지 URL")
    private String picture;

    @Schema(description = "인증 제공자", example = "google, kakao, naver")
    private String provider;

    @Schema(description = "사용자 권한", example = "ROLE_USER, ROLE_ADMIN")
    private String role;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "생성 일시", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "수정 일시", example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;

    public static MemberInfo from(Member member) {
        return MemberInfo.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .nickname(member.getName())  // nickname이 별도로 있다면 수정
                .picture(member.getPicture())
                .provider(member.getProvider().getProviderName())
                .role(member.getRole().name())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }
}