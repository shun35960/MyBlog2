package com.example.MyBlog.Entity;

import java.util.Date;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Seriesは、複数の記事をまとめる連載シリーズを表すエンティティクラスです。
 * シリーズのタイトルと説明を持ちます。
 * 記事側(Article.seriesId)が所属シリーズを参照します。
 * MongoDBのドキュメントとして保存されます。
 */

@Document(collection = "Serieses")
public record Series(
        @Id
        String id, //object _id
        @NotBlank(message = "シリーズ名は必須です")
        @Size(max = 200, message = "シリーズ名は200文字以内で入力してください")
        String title, //シリーズ名
        String description, //シリーズの説明(任意・Markdown可)
        @CreatedDate
        Date createdAt //作成日時
) {
    public static Series newSeries(String title) {
        // 新しいシリーズを作成するための静的メソッド
        return new Series(null, title, "", new Date());
    }
}
