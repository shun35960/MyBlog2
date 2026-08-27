package com.example.MyBlog.Controller;

import com.example.MyBlog.Config.MarkdownConfig;
import com.example.MyBlog.Config.SecurityConfig;
import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Entity.Series;
import com.example.MyBlog.Service.MyBlogService;
import com.example.MyBlog.Service.SeriesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(SeriesController.class)
@Import({MarkdownConfig.class, SecurityConfig.class})
class SeriesControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SeriesService seriesService;

    @MockitoBean
    MyBlogService myBlogService;

    @Test
    void seriesList() throws Exception {
        Series series = new Series("s1", "シリーズ1", "説明", new Date());
        when(seriesService.findPublishedSeriesSummaries())
                .thenReturn(List.of(new SeriesService.SeriesSummary(series, 2L)));

        mockMvc.perform(get("/Hello/Series"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(view().name("SeriesList"))
                .andExpect(model().attribute("seriesSummaries", hasSize(1)));
    }

    @Test
    void seriesDetail() throws Exception {
        Series series = new Series("s1", "シリーズ1", "## 説明", new Date());
        Article article1 = new Article("a1", "第1回", "# 本文1", true, "s1", new Date());
        Article article2 = new Article("a2", "第2回", "# 本文2", true, "s1", new Date());
        when(seriesService.findSeriesById("s1")).thenReturn(series);
        when(myBlogService.findPublishedArticlesBySeriesId("s1")).thenReturn(List.of(article1, article2));

        mockMvc.perform(get("/Hello/Series/s1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(view().name("SeriesDetail"))
                .andExpect(model().attribute("series", series))
                .andExpect(model().attribute("articleViews", hasSize(2)));
    }

    @Test
    void updateSeries() throws Exception {
        Series updated = new Series("s1", "新タイトル", "新説明", new Date());
        when(seriesService.updateSeries("s1", "新タイトル", "新説明")).thenReturn(updated);

        mockMvc.perform(post("/Hello/Series/s1")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("title", "新タイトル")
                        .param("description", "新説明"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
        verify(seriesService).updateSeries("s1", "新タイトル", "新説明");
    }

    @Test
    void deleteSeries() throws Exception {
        mockMvc.perform(delete("/Hello/Series/s1")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
        verify(seriesService).deleteSeries("s1");
    }
}
