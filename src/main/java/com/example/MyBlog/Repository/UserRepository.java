package com.example.MyBlog.Repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.MyBlog.Entity.Users;

/**
 * UserRepositoryは、MongoDBを使用してユーザー情報を管理するリポジトリインターフェースです。
 * ユーザー名でユーザーを検索するメソッドを提供します。
 */

public interface UserRepository extends MongoRepository<Users, String> {
    // ユーザー名でユーザーを検索するメソッド
    Optional<Users> findByUsername(String username);
}
