package com.example.MyBlog.Service;

import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Repository.MyBlogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MyBlogServiceImplは、MyBlogServiceインターフェースの実装クラスです。
 * 記事の取得、保存、削除などのビジネスロジックを提供します。
 * MyBlogRepositoryを使用してデータベースとやり取りします。
 */

@Service
@RequiredArgsConstructor
public class MyBlogServiceImpl implements MyBlogService {

    private final MyBlogRepository myBlogRepository;

    @Override
    public Article findArticleById(String id) {
        return myBlogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found id =" + id));
    }

    @Override
    public List<Article> findArticlePublishedTrue() {
        return myBlogRepository.findByPublishedTrue();
    }

    @Override
    public List<Article> findArticlePublishedFalse() {
        return myBlogRepository.findByPublishedFalse();
    }

    @Override
    public Article submitArticle(Article article) {
        return myBlogRepository.save(article);
    }

    @Override
    public Article updateArticle(String id, Article article){
        Article existingArticle = findArticleById(id);
        Article updateArticle = new Article(
                existingArticle.id(),
                article.title(),
                article.content(),
                article.published(),
                existingArticle.createdAt()

        );
        return myBlogRepository.save(updateArticle);
    }

    @Override
    public void deleteArticle(String id) {
        myBlogRepository.deleteById(id);
    }
}
