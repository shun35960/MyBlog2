package com.example.MyBlog.Repository;

import com.example.MyBlog.Entity.Users;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.security.core.userdetails.User;

import java.util.Optional;

/**
 * UserRepositoryは、MongoDBを使用してユーザー情報を管理するリポジトリインターフェースです。
 * ユーザー名でユーザーを検索するメソッドを提供します。
 */

public interface UserRepository extends MongoRepository<Users, String> {
    // ユーザー名でユーザーを検索するメソッド
    Optional<Users> findByUsername(String username);
}
