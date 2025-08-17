package backend_lingua.linguas.oauth.service;

import backend_lingua.linguas.oauth.dto.LoginResponse;
import backend_lingua.linguas.oauth.enumerated.ProviderType;
import backend_lingua.linguas.security.dto.response.TokenInfo;

public interface AuthService {

    LoginResponse socialLogin(ProviderType provider, String accessToken);

    TokenInfo reissueToken(String requestRefreshToken);

    void logout(String refreshToken);

}