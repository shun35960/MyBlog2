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
                .formLogin(login -> login
                        .loginPage("/login") // カスタムログインページのURL
                        .loginProcessingUrl("/authenticate") // ログイン処理のURL
                        .defaultSuccessUrl("/Hello") // ログイン成功後のリダイレクト先
                        .failureUrl("/login?error") // ログイン失敗時のリダイレクト先
                        .permitAll()
                ).logout(logout -> logout
                        .logoutSuccessUrl("/") // ログアウト成功後のリダイレクト先
                ).authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "register", "authenticate", "/").permitAll() // ログインページは認証不要
                        .requestMatchers("/Hello/**").authenticated()
                        .anyRequest().permitAll() // その他のリクエストは認証が必要
//                ).webAuthn((webAuthn) -> webAuthn
//                                .rpName("My Blog")
//                                .rpId("localhost") // RP IDは通常、ドメイン名やIPアドレスを指定します
//                                .allowedOrigins("http://localhost:8080") // クライアントのオリジンを指定します
//                        // optional properties
////                        .creationOptionsRepository(new CustomPublicKeyCredentialCreationOptionsRepository())
////                        .messageConverter(new CustomHttpMessageConverter())
                );
        return http.build();
    }
}
