package com.example.MyBlog.Service;

import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Repository.MyBlogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MyBlogServiceTest
 * サービスクラスのテスト
 * mockitoを使用してリポジトリをモック化し、サービスメソッドのテストを行う
 * MyBlogServiceImplクラスのメソッドをテストする
 */
@ExtendWith(MockitoExtension.class)
public class MyBlogServiceTest {
    // MyBlogServiceImplクラスのテスト

    @Mock
    private MyBlogRepository myBlogRepository;

    @InjectMocks
    private MyBlogServiceImpl myBlogService;

    @Test
    void test_findArticleById() {
        // Implement your test logic here
        Article article = new Article("id1", "Title 1", "Content 1", true);
        when(myBlogRepository.findById("id1")).thenReturn(Optional.of(article));

        Article result = myBlogService.findArticleById("id1");

        assertNotNull(result);
        assertEquals("id1", result.id());
        assertEquals("Title 1", result.title());
        assertEquals("Content 1", result.content());
        verify(myBlogRepository).findById("id1");

    }

    @Test
    void test_findArticlePublishedTrue() {
        // Implement your test logic here
        Article article1 = new Article("id1", "Title 1", "Content 1", true);
        Article article2 = new Article("id2", "Title 2", "Content 2", false);
        when(myBlogRepository.findByPublishedTrue()).thenReturn(List.of(article1));

        List<Article> result = myBlogService.findArticlePublishedTrue();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("id1", result.getFirst().id());
        assertTrue(result.getFirst().published());
    }

    @Test
    void test_findArticlePublishedFalse() {
        // Implement your test logic here
        Article article1 = new Article("id1", "Title 1", "Content 1", true);
        Article article2 = new Article("id2", "Title 2", "Content 2", false);
        when(myBlogRepository.findByPublishedFalse()).thenReturn(List.of(article2));

        List<Article> result = myBlogService.findArticlePublishedFalse();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("id2", result.getFirst().id());
        assertFalse(result.getFirst().published());
    }

    @Test
    void test_submitArticle() {
        // Implement your test logic here
        Article article = new Article("id1", "Title 1", "Content 1", true);
        when(myBlogRepository.save(article)).thenReturn(article);

        Article result = myBlogService.submitArticle(article);

        assertNotNull(result);
        assertEquals("id1", result.id());
        assertEquals("Title 1", result.title());
        assertEquals("Content 1", result.content());
        verify(myBlogRepository).save(article);
    }
}
