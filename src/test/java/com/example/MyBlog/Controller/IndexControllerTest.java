package com.example.MyBlog.Controller;

import com.example.MyBlog.Config.MarkdownConfig;
import com.example.MyBlog.Config.SecurityConfig;
import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Service.MyBlogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IndexController.class)
@Import({MarkdownConfig.class, SecurityConfig.class})
@TestPropertySource(properties = "myblog.about.article-id=aboutArticleId")
class IndexControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MyBlogService myBlogService;

    // --- index ---

    @Test
    void index_公開記事一覧が表示される() throws Exception {
        // Arrange
        List<Article> articles = List.of(
                new Article("id1", "タイトル1", "内容1", true, new Date()),
                new Article("id2", "タイトル2", "内容2", true, new Date())
        );
        when(myBlogService.findArticlePublishedTrue()).thenReturn(articles);

        // Act & Assert
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("Indextitle", "ようこそ"))
                .andExpect(model().attribute("articles", hasSize(2)));

        verify(myBlogService).findArticlePublishedTrue();
    }

    @Test
    void index_公開記事がない場合_空リストでindexが表示される() throws Exception {
        // Arrange
        when(myBlogService.findArticlePublishedTrue()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("articles", empty()));
    }

    // --- about ---

    @Test
    void about_記事が取得されてViewDescriptionが表示される() throws Exception {
        // Arrange
        Article article = new Article("aboutArticleId", "About", "## 筆者紹介", true, new Date());
        when(myBlogService.findArticleById("aboutArticleId")).thenReturn(article);

        // Act & Assert
        mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(view().name("ViewDescription"))
                .andExpect(model().attributeExists("article"))
                .andExpect(model().attributeExists("renderedHtmlContent"));

        verify(myBlogService).findArticleById("aboutArticleId");
    }

    @Test
    void about_記事のcontentがnull_空文字でレンダリングされる() throws Exception {
        // Arrange
        Article article = new Article("aboutArticleId", "About", null, true, new Date());
        when(myBlogService.findArticleById("aboutArticleId")).thenReturn(article);

        // Act & Assert
        mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(view().name("ViewDescription"))
                .andExpect(model().attributeExists("renderedHtmlContent"));
    }

    // --- viewDescription ---

    @Test
    void viewDescription_指定IDの記事が表示される() throws Exception {
        // Arrange
        Article article = new Article("id1", "タイトル1", "## 見出し\n本文", true, new Date());
        when(myBlogService.findArticleById("id1")).thenReturn(article);

        // Act & Assert
        mockMvc.perform(get("/ViewDescription/id1"))
                .andExpect(status().isOk())
                .andExpect(view().name("ViewDescription"))
                .andExpect(model().attribute("article", article))
                .andExpect(model().attributeExists("renderedHtmlContent"));

        verify(myBlogService).findArticleById("id1");
    }

    @Test
    void viewDescription_記事のcontentがnull_空文字でレンダリングされる() throws Exception {
        // Arrange
        Article article = new Article("id1", "タイトル1", null, true, new Date());
        when(myBlogService.findArticleById("id1")).thenReturn(article);

        // Act & Assert
        mockMvc.perform(get("/ViewDescription/id1"))
                .andExpect(status().isOk())
                .andExpect(view().name("ViewDescription"))
                .andExpect(model().attributeExists("renderedHtmlContent"));
    }
}