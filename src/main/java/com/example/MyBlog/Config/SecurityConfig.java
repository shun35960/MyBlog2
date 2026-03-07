package com.example.MyBlog.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfigは、Spring Securityの設定を行うクラスです。
 * カスタムログインページを設定します。
 * ログイン成功後のリダイレクト先や、認証が必要なURLを指定します。
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // パスワードエンコーダーを定義します。
        // ここではBCryptPasswordEncoderを使用しています。
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/images/**") // 画像API は CSRF 保護を無効化
                )
                .formLogin(AbstractHttpConfigurer::disable
                ).logout(logout -> logout
                .logoutUrl("/logout") // ログアウト処理のURL
                .invalidateHttpSession(true) // ログアウト時にセッションを無効化
                .deleteCookies("JSESSIONID") // ログアウト時にクッキー削除
                .logoutSuccessUrl("/") // ログアウト成功後のリダイレクト先
                .permitAll()
        ).authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/authenticate", "/").permitAll() // ログインページは認証不要
//                        .requestMatchers("/Hello/**").authenticated()
//                        .requestMatchers("/api/images/upload").authenticated() // 画像アップロードは認証必要
//                        .requestMatchers("/api/images/**").permitAll() // 画像取得・削除は誰でも可能
                        .anyRequest().permitAll() // その他のリクエストは認証が必要
        );
        return http.build();
    }
}
