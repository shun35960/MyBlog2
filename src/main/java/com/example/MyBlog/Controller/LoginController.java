package com.example.MyBlog.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * LoginControllerは、ログインページを表示するためのコントローラーです。
 * ログインエラーが発生した場合、エラーメッセージを表示します。
 */
@Controller
@RequiredArgsConstructor
public class LoginController {
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping(value = "/login", params = "error")
    public String loginError(Model model) {
        model.addAttribute("errormessage", "Invalid username or password.");
        return "login";
    }
}
