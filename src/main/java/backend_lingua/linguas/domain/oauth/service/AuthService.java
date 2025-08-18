package backend_lingua.linguas.domain.oauth.service;

import backend_lingua.linguas.domain.oauth.dto.LoginResponse;
import backend_lingua.linguas.domain.oauth.enumerated.ProviderType;
import backend_lingua.linguas.infrastructure.security.dto.TokenInfo;

public interface AuthService {

    LoginResponse socialLogin(ProviderType provider, String accessToken);

    TokenInfo reissueToken(String requestRefreshToken);

    void logout(String refreshToken);

}