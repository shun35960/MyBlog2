package com.example.MyBlog.Service;

import com.example.MyBlog.Entity.Article;

import java.util.List;

public interface MyBlogService {
//    List<Article> getAllArticle();
//    記事を全件表示

    Article findArticleById(String id);
    //idを指定して記事を表示

    List<Article> findArticlePublishedTrue();
    //公開状態がtrueのものを取得

    List<Article> findArticlePublishedFalse();
    //公開状態がfalseのものを取得

    //Article editArticle(Article article);
    //記事の編集

    Article submitArticle(Article article);
    //記事の登録

    Article updateArticle(String id, Article article);

    void deleteArticle(String id);
    //記事の削除
}
