package com.example.MyBlog.Service;

import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Repository.MyBlogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * MyBlogServiceImplは、MyBlogServiceインターフェースの実装クラスです。
 * 記事の取得、保存、削除などのビジネスロジックを提供します。
 * MyBlogRepositoryを使用してデータベースとやり取りします。
 */

@Service
@Transactional
@RequiredArgsConstructor
public class MyBlogServiceImpl implements MyBlogService {

    @Autowired
    private final MyBlogRepository myBlogRepository;

//    @Override
//    public List<Article> getAllArticle() {
//        return myBlogRepository.findAll();
//    }
//    使わないなら消す

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
    public void deleteArticle(String id) {
        myBlogRepository.deleteById(id);
    }


}


//    @Override
//    public Article editArticle(Article article) {
//        return null;
//    }
