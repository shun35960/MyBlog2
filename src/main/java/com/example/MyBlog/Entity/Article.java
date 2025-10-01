package com.example.MyBlog.Entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Articleは、ブログ記事を表すエンティティクラスです。
 * 記事のタイトル、内容、公開状態を持ちます。
 * MongoDBのドキュメントとして保存されます。
 */


@Document(collection = "Articles")
@CompoundIndex(name = "published_createdAt_idx", def = "{'published': 1, 'createdAt': -1}")
public record Article(
        @Id
        String id, //object _id
        @NotBlank(message = "タイトルは必須です")
        @Size(max = 200, message = "タイトルは200文字以内で入力してください")
        String title, //記事のタイトル
        @NotBlank(message = "記事内容は必須です")
        String content, //記事の内容
        boolean published, //公開状態
        @CreatedDate
        Date createdAt//作成日時
        ) {
    public static Article newArticle() {
        // 新しい記事を作成するための静的メソッド
        return new Article(null, "", "", false, new Date());
        //新規作成時は下書き(published=false)で作成
    }
}
