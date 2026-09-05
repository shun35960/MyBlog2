package com.example.MyBlog.Entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Usersは、ブログのユーザーを表すエンティティクラスです。
 * ユーザー名とパスワードを持ち、MongoDBのドキュメントとして保存されます。
 */

@Document(collection = "Users")
public record Users(
        @Id
        String id, // object _id
        @NotBlank(message = "ユーザー名は必須です")
        @Size(min = 3, max = 50, message = "ユーザー名は3文字以上50文字以内で入力してください")
        @Indexed(unique = true)
        String username, // ユーザー名
        @NotBlank(message = "パスワードは必須です")
        @Size(min = 8, message = "パスワードは8文字以上で入力してください")
        String password, // パスワード
        @NotBlank(message = "ロールは必須です")
        String roles // ユーザーの役割（ロール）
) {
    public static Users newUser() {
        // 新しいユーザーを作成するための静的メソッド
        return new Users(null, "", "", "");
        // 新規作成時は空のユーザー名とパスワードで作成
    }
}
