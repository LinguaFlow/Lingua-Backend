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
public class MemberInfo {
    private Long id;
    private String email;
    private String name;
    private String nickname;
    private String picture;
    private String provider;
    private String role;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // User 엔티티로부터 생성
    public static MemberInfo from(Member member) {
        return MemberInfo.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .nickname(member.getName())  // nickname이 별도로 있다면 수정
                .picture(member.getPicture())
                .provider(member.getProvider())
                .role(member.getRole().name())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }

    // UserPrincipal로부터 생성
    public static MemberInfo from(UserPrincipal principal) {
        if (principal.getMember() != null) {
            return from(principal.getMember());
        }

        // JWT에서 복원된 경우 (User 엔티티 없음)
        return MemberInfo.builder()
                .email(principal.getEmail())
                .name(principal.getName())
                .role(principal.getAuthorities().iterator().next().getAuthority())
                .build();
    }
}