package backend_lingua.linguas.infrastructure.security.filter;

import backend_lingua.linguas.infrastructure.security.token.dto.TokenInfo;
import backend_lingua.linguas.infrastructure.security.principal.UserPrincipal;
import backend_lingua.linguas.infrastructure.security.token.enumerated.TokenType;
import backend_lingua.linguas.infrastructure.security.token.service.TokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

import backend_lingua.linguas.infrastructure.security.principal.UserDetailsServiceImpl;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.access-token-secret}")
    private String accessTokenSecret;

    @Value("${jwt.refresh-token-secret}")
    private String refreshTokenSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final UserDetailsServiceImpl userDetailsService;

    private final TokenService tokenService;

    /**
     * 액세스 토큰 & 리프레시 토큰 생성
     */
    public TokenInfo generateToken(Authentication authentication) {
        String accessToken = createAccessToken(authentication);
        String refreshToken = createRefreshToken(authentication);
        Date accessTokenExpiryDate = createExpiryDate(accessTokenExpiration);

        return TokenInfo.from(accessToken, refreshToken, accessTokenExpiryDate.getTime(), refreshTokenExpiration);
    }

    /**
     * 액세스 토큰 생성
     */
    public String createAccessToken(Authentication authentication) {
        return createToken(authentication, accessTokenExpiration, accessTokenSecret);
    }

    /**
     * 리프레시 토큰 생성 및 저장
     */
    public String createRefreshToken(Authentication authentication) {
        String token = createToken(authentication, refreshTokenExpiration, refreshTokenSecret);
        Date expiryDate = createExpiryDate(refreshTokenExpiration);

        // DB에 리프레시 토큰 저장
        tokenService.createRefreshToken(token, expiryDate, authentication);

        return token;
    }

    /**
     * JWT 토큰 생성 (공통 메서드)
     */
    private String createToken(Authentication authentication, long expirationTime, String secretKey) {
        String authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(authentication.getName())  // email
                .claim("auth", authorities)
                .setIssuedAt(new Date())
                .setExpiration(createExpiryDate(expirationTime))
                .signWith(createKey(secretKey))
                .compact();
    }

    /**
     * 토큰에서 사용자 이메일 추출
     */
    public String getUsernameFromToken(String token, TokenType tokenType) {
        String secretKey = (tokenType == TokenType.ACCESS_TOKEN) ? accessTokenSecret : refreshTokenSecret;

        return Jwts.parserBuilder()
                .setSigningKey(createKey(secretKey))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * 토큰에서 Authentication 객체 생성
     */
    public Authentication getAuthentication(String token, TokenType tokenType) {
        String username = getUsernameFromToken(token, tokenType);
        UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername(username);

        return new UsernamePasswordAuthenticationToken(
                userPrincipal,
                token,
                userPrincipal.getAuthorities()
        );
    }

    /**
     * 액세스 토큰 검증
     */
    public boolean validateAccessToken(String token) {
        return validateToken(token, accessTokenSecret);
    }

    /**
     * 리프레시 토큰 검증
     */
    public boolean validateRefreshToken(String token) {
        return validateToken(token, refreshTokenSecret);
    }

    /**
     * 토큰 검증 (공통 메서드)
     */
    private boolean validateToken(String token, String secretKey) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(createKey(secretKey))
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    private Date createExpiryDate(long expirationTime) {
        return new Date(System.currentTimeMillis() + expirationTime);
    }

    private SecretKey createKey(String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}