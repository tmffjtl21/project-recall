package recall.support;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * 로그인 방식별 표시 이름(= 소유자/작성자 키)을 구한다.
 * 폼 로그인: username, GitHub: login, 카카오: properties.nickname, 네이버: response.nickname.
 */
public final class LoginUsername {

    private LoginUsername() {
    }

    public static String of(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        if (authentication instanceof OAuth2AuthenticationToken token) {
            OAuth2User user = token.getPrincipal();
            String name = switch (token.getAuthorizedClientRegistrationId()) {
                case "github" -> asString(user.getAttribute("login"));
                case "kakao" -> nested(user.getAttribute("properties"), "nickname");
                case "naver" -> {
                    Object response = user.getAttribute("response");
                    String nickname = nested(response, "nickname");
                    yield nickname != null ? nickname : nested(response, "name");
                }
                default -> null;
            };
            if (name != null) {
                return name;
            }
        }
        return authentication.getName();
    }

    private static String nested(Object attribute, String key) {
        if (attribute instanceof Map<?, ?> map) {
            return asString(map.get(key));
        }
        return null;
    }

    private static String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
