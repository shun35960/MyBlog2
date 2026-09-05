package com.example.MyBlog.Service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.MyBlog.Entity.Users;
import com.example.MyBlog.Repository.UserRepository;

/**
 * UserDetailsServiceimplは、ユーザー名を使用してユーザーの詳細情報をロードするサービスクラスです。
 * Spring SecurityのUserDetailsServiceインターフェースを実装しています。
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceimpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.roles())
        );

        return new org.springframework.security.core.userdetails.User(
                user.username(),
                user.password(),
                authorities
        );
    }
}
