package com.example.MyBlog.Entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Articleは、ブログ記事を表すエンティティクラスです。
 * 記事のタイトル、内容、公開状態を持ちます。
 * MongoDBのドキュメントとして保存されます。
 */


@Document(collection = "Articles")
public record Article(
        @Id
        String id, //object _id
        String title, //記事のタイトル
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
