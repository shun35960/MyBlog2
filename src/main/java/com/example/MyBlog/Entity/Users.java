package com.example.MyBlog.Entity;

import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Usersは、ブログのユーザーを表すエンティティクラスです。
 * ユーザー名とパスワードを持ち、MongoDBのドキュメントとして保存されます。
 */

@Document(collection = "Users")
public record Users(
        @Id
        String id, // object _id
        @Indexed(unique = true)
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
