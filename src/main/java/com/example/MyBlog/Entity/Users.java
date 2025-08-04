package com.example.MyBlog.Entity;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Usersは、ブログのユーザーを表すエンティティクラスです。
 * ユーザー名とパスワードを持ち、MongoDBのドキュメントとして保存されます。
 */

@Document(collection = "Users")
public record Users(
        String id, // object _id
        String username, // ユーザー名
        String password, // パスワード
        String roles // ユーザーの役割（ロール）
) {
    public static Users newUser() {
        // 新しいユーザーを作成するための静的メソッド
        return new Users(null, "", "", "");
        // 新規作成時は空のユーザー名とパスワードで作成
    }
}
