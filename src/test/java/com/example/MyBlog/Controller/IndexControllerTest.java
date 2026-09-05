package com.example.MyBlog.Controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.MyBlog.Config.MarkdownConfig;
import com.example.MyBlog.Config.SecurityConfig;
import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Entity.Series;
import com.example.MyBlog.Service.MyBlogService;
import com.example.MyBlog.Service.SeriesService;

@WebMvcTest(IndexController.class)
@Import({MarkdownConfig.class, SecurityConfig.class})
@TestPropertySource(properties = "myblog.about.article-id=aboutArticleId")
class IndexControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MyBlogService myBlogService;

    @MockitoBean
    SeriesService seriesService;

    // --- index ---

    @Test
    void index_公開記事一覧が表示される() throws Exception {
        // Arrange
        List<Article> articles = List.of(
                new Article("id1", "タイトル1", "内容1", true, null, new Date()),
                new Article("id2", "タイトル2", "内容2", true, null, new Date())
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
        Article article = new Article("aboutArticleId", "About", "## 筆者紹介", true, null, new Date());
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
        Article article = new Article("aboutArticleId", "About", null, true, null, new Date());
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
        Article article = new Article("id1", "タイトル1", "## 見出し\n本文", true, null, new Date());
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
        Article article = new Article("id1", "タイトル1", null, true, null, new Date());
        when(myBlogService.findArticleById("id1")).thenReturn(article);

        // Act & Assert
        mockMvc.perform(get("/ViewDescription/id1"))
                .andExpect(status().isOk())
                .andExpect(view().name("ViewDescription"))
                .andExpect(model().attributeExists("renderedHtmlContent"));
    }

    // --- viewSeriesList / viewSeriesDetail ---

    @Test
    void viewSeriesList_公開シリーズ一覧が表示される() throws Exception {
        // Arrange
        Series series = new Series("s1", "シリーズ1", "説明", new Date());
        when(seriesService.findPublishedSeriesSummaries())
                .thenReturn(List.of(new SeriesService.SeriesSummary(series, 2L)));

        // Act & Assert
        mockMvc.perform(get("/Series"))
                .andExpect(status().isOk())
                .andExpect(view().name("ViewSeriesList"))
                .andExpect(model().attribute("seriesSummaries", hasSize(1)));

        verify(seriesService).findPublishedSeriesSummaries();
    }

    @Test
    void viewSeriesDetail_公開記事が連結表示される() throws Exception {
        // Arrange
        Series series = new Series("s1", "シリーズ1", "## 説明", new Date());
        Article article1 = new Article("a1", "第1回", "# 本文1", true, "s1", new Date());
        Article article2 = new Article("a2", "第2回", "# 本文2", true, "s1", new Date());
        when(seriesService.findSeriesById("s1")).thenReturn(series);
        when(myBlogService.findPublishedArticlesBySeriesId("s1")).thenReturn(List.of(article1, article2));

        // Act & Assert
        mockMvc.perform(get("/Series/s1"))
                .andExpect(status().isOk())
                .andExpect(view().name("ViewSeriesDetail"))
                .andExpect(model().attribute("series", series))
                .andExpect(model().attribute("articleViews", hasSize(2)))
                .andExpect(model().attributeExists("renderedDescription"));
    }
}
