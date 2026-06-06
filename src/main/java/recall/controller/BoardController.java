package recall.controller;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import recall.domain.Memo;
import recall.repository.MemoRepository;

@Controller
public class BoardController {

    private final MemoRepository memoRepository;

    public BoardController(MemoRepository memoRepository) {
        this.memoRepository = memoRepository;
    }

    @GetMapping("/")
    public String board(Model model, Authentication authentication) {
        model.addAttribute("memos", memoRepository.findAllByOrderByIdDesc());
        model.addAttribute("username", displayName(authentication));
        return "board";
    }

    @PostMapping("/memos")
    public String addMemo(@RequestParam String title,
                          @RequestParam String content,
                          Authentication authentication) {
        memoRepository.save(Memo.builder()
                .title(title)
                .content(content)
                .writer(displayName(authentication))
                .build());
        return "redirect:/";
    }

    /**
     * 로그인 방식별로 표시 이름을 결정한다.
     * - 폼 로그인: username(test)
     * - GitHub: login 속성
     * - 카카오: properties.nickname
     * - 네이버: response.nickname (없으면 response.name)
     */
    private String displayName(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            OAuth2User user = token.getPrincipal();
            String provider = token.getAuthorizedClientRegistrationId();
            String name = switch (provider) {
                case "github" -> asString(user.getAttribute("login"));
                case "kakao" -> nested(user.getAttribute("properties"), "nickname");
                case "naver" -> {
                    Object response = user.getAttribute("response");
                    String nn = nested(response, "nickname");
                    yield nn != null ? nn : nested(response, "name");
                }
                default -> null;
            };
            if (name != null) {
                return name;
            }
        }
        return authentication.getName();
    }

    private String nested(Object attribute, String key) {
        if (attribute instanceof Map<?, ?> map) {
            return asString(map.get(key));
        }
        return null;
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
