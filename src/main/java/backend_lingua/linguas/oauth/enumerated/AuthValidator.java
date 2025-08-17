package backend_lingua.linguas.oauth.enumerated;


import backend_lingua.linguas.util.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.Optional;


public class AuthValidator {

    public static void validRequestToken(String token) {
        Optional.ofNullable(token)
                .filter(t -> !t.trim().isEmpty())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST ,"유효한 토큰이 필요합니다."));
    }

    public static void validateJwtFormat(String token) {
        Optional.ofNullable(token)
                .filter(t -> t.matches("^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$"))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST , "유효한 JWT 토큰 형식이 아닙니다."));
    }

    public static void validateProviderToken(String token, String providerName) {
        Optional.ofNullable(token)
                .filter(t -> t.length() >= 20)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "유효하지 않은 %s 액세스 토큰입니다."));
    }

}