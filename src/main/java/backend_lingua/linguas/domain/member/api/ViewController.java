package backend_lingua.linguas.domain.member.api;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @Value("${kakao.js-key}")
    private String kakaoJsKey;

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("kakaoJsKey", kakaoJsKey);
        return "login";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile"; // templates/profile.html
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }
}