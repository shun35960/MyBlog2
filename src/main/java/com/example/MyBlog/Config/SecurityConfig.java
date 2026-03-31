package com.example.MyBlog.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .formLogin(login -> login
                                .loginPage("/login")
                                .loginProcessingUrl("/authenticate")
                                .defaultSuccessUrl("/Hello")
                                .failureUrl("/login?error")
                        // .permitAll() を削除
                ).logout(logout -> logout
                                .logoutUrl("/logout")
                                .invalidateHttpSession(true)
                                .deleteCookies("JSESSIONID")
                                .logoutSuccessUrl("/")
                        // .permitAll() を削除
                ).authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/authenticate", "/", "/logout").permitAll()
                        .requestMatchers("/Hello/**").authenticated()
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
