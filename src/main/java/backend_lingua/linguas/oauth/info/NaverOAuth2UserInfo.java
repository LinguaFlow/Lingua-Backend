package backend_lingua.linguas.oauth.info;

import backend_lingua.linguas.oauth.enumerated.ProviderType;

import java.util.Map;

public class NaverOAuth2UserInfo extends OAuth2UserInfo {

    private Map<String, Object> response;

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
        this.response = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getId() {

        return (String) response.get("id");
    }

    @Override
    public String getName() {
        if (response != null) {
            String name = (String) response.get("name");
            if (name != null) return name;

            // name이 없으면 nickname 사용
            String nickname = (String) response.get("nickname");
            if (nickname != null) return nickname;
        }
        return "네이버사용자";
    }

    @Override
    public String getEmail() {
        return (String) response.get("email");
    }

    @Override
    public String getImageUrl() {
        return (String) response.get("profile_image");
    }

    @Override
    public Map<String, Object> getAttributes() {
        return super.getAttributes();
    }

}