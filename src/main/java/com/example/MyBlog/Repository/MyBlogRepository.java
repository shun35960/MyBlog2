package com.example.MyBlog.Repository;

import com.example.MyBlog.Entity.Article;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * MyBlogRepositoryは、MongoDBを使用してArticleエンティティに対するCRUD操作を提供します。
 * Spring Data MongoDBのMongoRepositoryを拡張しています。
 * カスタムクエリメソッドを追加することができます。
 */

public interface MyBlogRepository extends MongoRepository<Article, String> {
    // ここにカスタムクエリメソッドを追加できます
    // 例: List<Article> findByTitle(String title);
    List<Article> findByPublishedTrue();
    //公開状態がtrueのものを取得
    List<Article> findByPublishedFalse();
    //公開状態がfalseのものを取得
}
