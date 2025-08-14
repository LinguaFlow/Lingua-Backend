package backend_lingua.linguas.security.filter;

import backend_lingua.linguas.security.principal.UserPrincipal;
import backend_lingua.linguas.security.token.enumerated.TokenType;
import backend_lingua.linguas.security.token.service.TokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.access-token-secret}")
    private String accessTokenSecret;

    @Value("${jwt.refresh-token-secret}")
    private String refreshTokenSecret;

    @Getter
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Getter
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final TokenService tokenService;

    /**
     * Access Token 생성
     */
    public String createAccessToken(Authentication authentication) {
        return createToken(authentication, accessTokenExpiration, accessTokenSecret, TokenType.ACCESS_TOKEN);
    }

    /**
     * Refresh Token 생성 및 저장
     */
    public String createRefreshToken(Authentication authentication) {
        String token = createToken(authentication, refreshTokenExpiration, refreshTokenSecret, TokenType.REFRESH_TOKEN);
        Date expiryDate = createExpiryDate(refreshTokenExpiration);

        // DB에 Refresh Token 저장
        tokenService.createRefreshToken(token, expiryDate, authentication);

        return token;
    }

    /**
     * 토큰 생성 공통 메서드
     */
    private String createToken(Authentication authentication, long expirationTime, String secret, TokenType tokenType) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        String email = authentication.getName();

        // Claims 빌더 생성
        JwtBuilder builder = Jwts.builder()
                .setSubject(email)
                .claim("auth", authorities)
                .claim("type", tokenType.getValue());

        // UserPrincipal인 경우 추가 정보 포함
        if (authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            // getUserId() 메서드 사용
            if (principal.getUserId() != null) {
                builder.claim("userId", principal.getUserId());
            }

            // getProvider() 메서드 사용
            if (principal.getProvider() != null) {
                builder.claim("provider", principal.getProvider());
            }
        }

        return builder
                .setIssuedAt(new Date())
                .setExpiration(createExpiryDate(expirationTime))
                .signWith(createKey(secret))
                .compact();
    }

    /**
     * 토큰에서 인증 정보 추출
     */
    public Authentication getAuthentication(String token, TokenType tokenType) {
        Claims claims = parseClaims(token, tokenType);

        String email = claims.getSubject();
        String auth = claims.get("auth", String.class);
        Long userId = claims.get("userId", Long.class);
        String provider = claims.get("provider", String.class);

        // JWT 토큰에서 UserPrincipal 생성
        UserPrincipal principal;
        if (userId != null) {
            // User ID가 있으면 간단한 User 객체 생성 (DB 조회 없이)
            principal = UserPrincipal.createFromTokenWithId(email, auth, userId, provider);
        } else {
            // User ID가 없으면 기본 생성
            principal = UserPrincipal.createFromToken(email, auth);
        }

        return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
    }

    /**
     * Access Token 검증
     */
    public boolean validateAccessToken(String token) {
        return validateToken(token, accessTokenSecret);
    }

    /**
     * Refresh Token 검증
     */
    public boolean validateRefreshToken(String token) {
        return validateToken(token, refreshTokenSecret);
    }

    /**
     * 토큰 검증 공통 메서드
     */
    private boolean validateToken(String token, String secret) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(createKey(secret))
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

    /**
     * 토큰에서 클레임 추출
     */
    private Claims parseClaims(String token, TokenType tokenType) {
        String secret = tokenType == TokenType.ACCESS_TOKEN ? accessTokenSecret : refreshTokenSecret;

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(createKey(secret))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    /**
     * 토큰에서 이메일 추출
     */
    public String getEmailFromToken(String token, TokenType tokenType) {
        return parseClaims(token, tokenType).getSubject();
    }

    /**
     * 토큰에서 User ID 추출
     */
    public Long getUserIdFromToken(String token, TokenType tokenType) {
        return parseClaims(token, tokenType).get("userId", Long.class);
    }

    /**
     * 토큰 만료 시간 추출
     */
    public Date getExpirationFromToken(String token, TokenType tokenType) {
        return parseClaims(token, tokenType).getExpiration();
    }

    /**
     * 만료 날짜 생성
     */
    private Date createExpiryDate(long expirationTime) {
        return new Date(System.currentTimeMillis() + expirationTime);
    }

    /**
     * 서명 키 생성
     */
    private SecretKey createKey(String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}