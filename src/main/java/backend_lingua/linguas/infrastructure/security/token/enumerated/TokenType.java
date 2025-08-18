package backend_lingua.linguas.infrastructure.security.token.enumerated;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenType {
    ACCESS_TOKEN("access-token"),

    REFRESH_TOKEN("refresh-token");

    private final String value;
}
