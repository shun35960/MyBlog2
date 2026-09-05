package com.example.MyBlog.Controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.MyBlog.Entity.Users;
import com.example.MyBlog.Repository.UserRepository;

/**
 * RegisterControllerは、ユーザー登録を処理するためのコントローラーです。
 * ユーザー登録フォームを表示し、ユーザー情報をデータベースに保存します。
 */
@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String showRegisterForm(Model model) {
        // ユーザー登録フォームを表示するメソッド
        model.addAttribute("user", Users.newUser()); // Userクラスはユーザー情報を保持するエンティティ
        return "register"; // register.htmlを返す
    }

    @PostMapping
    public String Register(@Valid @ModelAttribute Users user) {

        Users newUser = new Users(
                user.id(),
                user.username(),
                passwordEncoder.encode(user.password()), // パスワードをエンコード
                user.roles() // ユーザーの役割（ロール）をそのまま使用
        );
        userRepository.save(newUser); // ユーザーをデータベースに保存
        return "redirect:/login"; // 登録後、ログインページにリダイレクト
    }
}
