package backend_lingua.linguas.security.principal;

import backend_lingua.linguas.member.entity.Member;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter  // Setter 추가 (Builder 대신 직접 설정)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserPrincipal implements OAuth2User, UserDetails {

    private Member member;
    private String email;
    private Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;
    private String nameAttributeKey;

    // 추가 필드 (JWT 토큰 복원 시 사용)
    private Long userId;
    private String provider;

    // 기존 생성자 메서드들 유지...
    public static UserPrincipal create(Member member) {
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(member.getRole().getKey())
        );

        UserPrincipal principal = new UserPrincipal();
        principal.member = member;
        principal.email = member.getEmail();
        principal.userId = member.getId();
        principal.provider = member.getProvider();
        principal.authorities = authorities;
        return principal;
    }

    public static UserPrincipal create(Member member, Map<String, Object> attributes, String nameAttributeKey) {
        UserPrincipal principal = create(member);
        principal.attributes = attributes;
        principal.nameAttributeKey = nameAttributeKey;
        return principal;
    }

    public static UserPrincipal createFromToken(String email, String authoritiesString) {
        Collection<GrantedAuthority> authorities = Arrays.stream(authoritiesString.split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UserPrincipal principal = new UserPrincipal();
        principal.email = email;
        principal.authorities = authorities;
        return principal;
    }

    // ===== 새로 추가되는 메서드 =====
    public static UserPrincipal createFromTokenWithId(String email, String authoritiesString,
                                                      Long userId, String provider) {
        Collection<GrantedAuthority> authorities = Arrays.stream(authoritiesString.split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UserPrincipal principal = new UserPrincipal();
        principal.email = email;
        principal.userId = userId;
        principal.provider = provider;
        principal.authorities = authorities;
        return principal;
    }

    // userId getter 추가
    public Long getUserId() {
        if (member != null) {
            return member.getId();
        }
        return userId;
    }

    // provider getter 추가
    public String getProvider() {
        if (member != null) {
            return member.getProvider();
        }
        return provider;
    }

    // 기존 메서드들 모두 유지...
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        if (member != null) {
            return member.getName();
        }
        if (nameAttributeKey != null && attributes != null) {
            return String.valueOf(attributes.get(nameAttributeKey));
        }
        return email;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return member != null ? member.getEmail() : email;
    }

}
