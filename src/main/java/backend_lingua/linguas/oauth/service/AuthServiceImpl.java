package backend_lingua.linguas.oauth.service;

import backend_lingua.linguas.member.entity.Member;
import backend_lingua.linguas.member.repository.MemberRepository;
import backend_lingua.linguas.security.dto.response.LoginResponse;
import backend_lingua.linguas.security.dto.response.TokenInfo;
import backend_lingua.linguas.security.filter.JwtTokenProvider;
import backend_lingua.linguas.security.principal.UserPrincipal;
import backend_lingua.linguas.security.token.dto.RefreshToken;
import backend_lingua.linguas.security.token.enumerated.TokenType;
import backend_lingua.linguas.security.token.repository.RefreshTokenRepository;
import backend_lingua.linguas.security.token.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final MemberRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * OAuth2SuccessHandler에서 호출하는 메서드
     */
    @Override
    @Transactional
    public LoginResponse createTokensForOAuth(UserPrincipal userPrincipal) {
        log.debug("OAuth 토큰 생성 시작: {}", userPrincipal.getEmail());

        // Authentication 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        );

        // 기존 리프레시 토큰 삭제
        if (userPrincipal.getUserId() != null) {
            refreshTokenRepository.deleteByMemberId(userPrincipal.getUserId());
        }

        // 새 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication);

        // TokenInfo 생성
        TokenInfo tokenInfo = TokenInfo.builder()
                .accessToken(accessToken)
                .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .refreshToken(refreshToken)
                .refreshTokenExpiresIn(jwtTokenProvider.getRefreshTokenExpiration())
                .tokenType("Bearer")
                .build();

        // LoginResponse 반환
        return LoginResponse.of(userPrincipal, tokenInfo);
    }

    /**
     * Android용 카카오 로그인 처리
     */
    @Override
    @Transactional
    public LoginResponse processKakaoLogin(String kakaoToken, String email, String name, String profileImage) {
        log.info("카카오 로그인 처리: {}", email);

        // 사용자 조회 또는 생성
        Member member = userRepository.findByEmailAndProvider(email, "kakao")
                .orElseGet(() -> {
                    Member newMember = Member.builder()
                            .email(email)
                            .name(name)
                            .picture(profileImage)
                            .provider("kakao")
                            .build();
                    return userRepository.save(newMember);
                });

        // 정보 업데이트
        if (!member.getName().equals(name) || !member.getPicture().equals(profileImage)) {
            member.updateInfo(name, profileImage);
            userRepository.save(member);
        }

        // UserPrincipal 생성 및 토큰 발급
        UserPrincipal userPrincipal = UserPrincipal.create(member);
        return createTokensForOAuth(userPrincipal);
    }

    /**
     * 토큰 재발급
     */
    @Override
    @Transactional
    public TokenInfo reissueToken(String requestRefreshToken) {
        // 리프레시 토큰 검증
        if (!jwtTokenProvider.validateRefreshToken(requestRefreshToken)) {
            throw new RuntimeException("유효하지 않은 Refresh Token입니다.");
        }

        // DB에서 토큰 조회
        RefreshToken refreshToken = refreshTokenRepository.findByTokenWithMember(requestRefreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh Token을 찾을 수 없습니다."));

        // 만료 확인
        if (tokenService.isTokenExpired(refreshToken)) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("만료된 Refresh Token입니다.");
        }

        // 새 토큰 발급
        UserPrincipal userPrincipal = UserPrincipal.create(refreshToken.getMember());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        );

        // 기존 토큰 삭제
        refreshTokenRepository.delete(refreshToken);

        // 새 토큰 생성 및 반환
        String newAccessToken = jwtTokenProvider.createAccessToken(authentication);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(authentication);

        return TokenInfo.builder()
                .accessToken(newAccessToken)
                .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .refreshToken(newRefreshToken)
                .refreshTokenExpiresIn(jwtTokenProvider.getRefreshTokenExpiration())
                .tokenType("Bearer")
                .build();
    }

    /**
     * 로그아웃
     */
    @Override
    @Transactional
    public void logout(String email) {
        refreshTokenRepository.deleteByMemberEmail(email);
        log.info("사용자 로그아웃: {}", email);
    }

    /**
     * 토큰 검증
     */
    @Override
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateAccessToken(token);
    }

    /**
     * 토큰에서 이메일 추출
     */
    @Override
    public String getEmailFromToken(String token) {
        return jwtTokenProvider.getEmailFromToken(token, TokenType.ACCESS_TOKEN);
    }

    /**
     * 토큰에서 사용자 정보 추출
     */
    @Override
    public UserPrincipal getUserFromToken(String token) {
        Authentication authentication = jwtTokenProvider.getAuthentication(token, TokenType.ACCESS_TOKEN);
        return (UserPrincipal) authentication.getPrincipal();
    }
}