package backend_lingua.linguas.oauth.service;

import backend_lingua.linguas.member.entity.Member;
import backend_lingua.linguas.member.enumerated.MemberRole;
import backend_lingua.linguas.member.repository.MemberRepository;
import backend_lingua.linguas.oauth.dto.LoginResponse;
import backend_lingua.linguas.oauth.enumerated.ProviderType;
import backend_lingua.linguas.oauth.info.KakaoOAuth2UserInfo;
import backend_lingua.linguas.oauth.info.NaverOAuth2UserInfo;
import backend_lingua.linguas.oauth.info.OAuth2UserInfo;
import backend_lingua.linguas.security.dto.response.MemberInfo;
import backend_lingua.linguas.security.dto.response.TokenInfo;
import backend_lingua.linguas.security.filter.JwtTokenProvider;
import backend_lingua.linguas.security.principal.UserPrincipal;
import backend_lingua.linguas.security.token.enumerated.TokenType;
import backend_lingua.linguas.security.token.service.TokenService;
import backend_lingua.linguas.util.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    @Value("${oauth2.kakao.user-info-uri:https://kapi.kakao.com/v2/user/me}")
    private String kakaoUserInfoUri;

    @Value("${oauth2.naver.user-info-uri:https://openapi.naver.com/v1/nid/me}")
    private String naverUserInfoUri;

    private final RestTemplate restTemplate;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

    /**
     * 소셜 로그인
     */
    @Override
    @Transactional
    public LoginResponse socialLogin(ProviderType provider, String accessToken) {
        log.info("[소셜 로그인] provider: {}", provider.getProviderName());

        // 1. Provider API 호출하여 사용자 정보 가져오기
        Map<String, Object> attributes = fetchUserAttributes(provider, accessToken);

        // 2. OAuth2UserInfo 객체 생성
        OAuth2UserInfo userInfo = createOAuth2UserInfo(provider, attributes);

        // 3. 이메일 검증
        validateEmail(userInfo.getEmail(), provider);

        // 4. 회원 조회 또는 생성
        Member member = findOrCreateMember(userInfo, provider);

        // 5. Authentication 객체 생성
        Authentication authentication = createAuthentication(member, attributes);

        // 6. 기존 리프레시 토큰 삭제
        tokenService.deleteRefreshToken(member.getEmail());

        // 7. 새 토큰 발급
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        // 8. 응답 생성
        MemberInfo memberInfo = MemberInfo.from(member);
        return LoginResponse.of(memberInfo, tokenInfo);
    }

    /**
     * 토큰 재발급
     */
    @Override
    @Transactional
    public TokenInfo reissueToken(String refreshToken) {
        log.debug("[토큰 재발급 요청]");

        // 1. 리프레시 토큰 검증
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "유효하지 않은 리프레시 토큰입니다."
            );
        }

        // 2. 리프레시 토큰으로 인증 정보 추출
        Authentication authentication = jwtTokenProvider.getAuthentication(
                refreshToken,
                TokenType.REFRESH_TOKEN
        );

        // 3. 기존 리프레시 토큰 삭제
        tokenService.deleteRefreshToken(authentication.getName());

        // 4. 새 토큰 발급
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        log.info("[토큰 재발급 성공] email: {}", authentication.getName());
        return tokenInfo;
    }

    /**
     * Provider API 호출하여 사용자 정보 가져오기
     */
    private Map<String, Object> fetchUserAttributes(ProviderType provider, String accessToken) {
        String endPoint = getProviderEndpoint(provider);
        HttpMethod method = (provider == ProviderType.KAKAO) ? HttpMethod.POST : HttpMethod.GET;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    endPoint,
                    method,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        String.format("%s 사용자 정보 조회 실패", provider.getDisplayName())
                );
            }

            return response.getBody();

        } catch (Exception e) {
            log.error("[{}] 사용자 정보 조회 실패: {}", provider.getProviderName(), e.getMessage());
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    String.format("%s 사용자 정보 조회 실패", provider.getDisplayName())
            );
        }
    }

    /**
     * OAuth2UserInfo 팩토리
     */
    private OAuth2UserInfo createOAuth2UserInfo(ProviderType provider, Map<String, Object> attributes) {
        return switch (provider) {
            case KAKAO -> new KakaoOAuth2UserInfo(attributes);
            case NAVER -> new NaverOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException(
                    "지원하지 않는 소셜 로그인입니다: " + provider.getProviderName()
            );
        };
    }

    /**
     * 이메일 검증
     */
    private void validateEmail(String email, ProviderType provider) {
        if (!StringUtils.hasText(email)) {
            throw new OAuth2AuthenticationException(
                    String.format("%s 이메일을 찾을 수 없습니다.", provider.getDisplayName())
            );
        }
    }

    /**
     * 회원 조회 또는 생성
     */
    private Member findOrCreateMember(OAuth2UserInfo userInfo, ProviderType provider) {
        return memberRepository.findByEmailAndProvider(userInfo.getEmail(), provider)
                .orElseGet(() -> {
                    Member newMember = Member.builder()
                            .email(userInfo.getEmail())
                            .name(userInfo.getName())
                            .picture(userInfo.getImageUrl())
                            .provider(provider)
                            .providerId(userInfo.getId())
                            .role(MemberRole.MEMBER)
                            .build();

                    log.info("[신규 회원 가입] email: {}, provider: {}",
                            userInfo.getEmail(), provider.getProviderName());

                    return memberRepository.save(newMember);
                });
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {

        // 2. 토큰에서 사용자 정보 추출
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken, TokenType.REFRESH_TOKEN);

        log.info("토큰에서 사용자 정보 추출 = {}", username);
        // 3. DB에서 리프레시 토큰 삭제
        tokenService.deleteRefreshToken(username);

    }

    /**
     * Authentication 객체 생성
     */
    private Authentication createAuthentication(Member member, Map<String, Object> attributes) {
        UserPrincipal userPrincipal = UserPrincipal.create(member, attributes);
        return new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        );
    }

    /**
     * Provider별 API 엔드포인트 반환
     */
    private String getProviderEndpoint(ProviderType provider) {
        return switch (provider) {
            case KAKAO -> kakaoUserInfoUri;
            case NAVER -> naverUserInfoUri;
            default -> throw new OAuth2AuthenticationException(
                    "지원하지 않는 서비스입니다: " + provider.getProviderName()
            );
        };
    }
}