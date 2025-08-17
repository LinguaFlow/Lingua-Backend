package backend_lingua.linguas.oauth.enumerated;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 소셜 로그인 제공자 타입
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum ProviderType {
    KAKAO("kakao", "1", "카카오"),
    NAVER("naver", "2", "네이버");

    private final String providerName;
    private final String providerNumber;
    private final String displayName;

    /**
     * Provider 이름으로 ProviderType 찾기
     */
    public static ProviderType byProviderName(String providerName) {
        return Arrays.stream(ProviderType.values())
                .filter(value -> value.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 제공자 번호입니다: %s"
                ));
    }

    /**
     * Provider 번호로 ProviderType 찾기
     */
    public static ProviderType byProviderNumber(String providerNumber) {
        return Arrays.stream(ProviderType.values())
                .filter(value -> value.getProviderNumber().equals(providerNumber))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("지원하지 않는 제공자 번호입니다: %s", providerNumber)
                ));
    }

}