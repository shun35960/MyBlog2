package com.example.MyBlog.Controller;

import com.example.MyBlog.Config.MarkdownConfig;
import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Service.ImageServiceImpl;
import com.example.MyBlog.Service.MyBlogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(MyBlogController.class)
@Import({SecurityAutoConfiguration.class, MarkdownConfig.class})
class MyBlogControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MyBlogService myBlogService;

    @Test
    void 認証済みユーザーが記事一覧ページにアクセスできる() throws Exception {
        List<Article> articles = List.of(
                new Article("id1", "title1", "content1", true, new Date())
        );
        when(myBlogService.findArticlePublishedTrue()).thenReturn(articles);

        mockMvc.perform(get("/Hello").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(view().name("Hello"))
                .andExpect(model().attributeExists("articles"))
                .andExpect(model().attribute("Hellotitle", "記事の一覧!"));

    }

    @Test
    void 未認証ユーザーがアクセスするとリダイレクトされる() throws Exception {
        mockMvc.perform(get("/Hello"))
                .andExpect(status().is3xxRedirection());
        //未認証のためサービスは呼ばれない
        verify(myBlogService, never()).findArticlePublishedTrue();
    }

    @Test
    void 記事が0件のとき空リストがあモデルに渡る() throws Exception {
        when(myBlogService.findArticlePublishedTrue()).thenReturn(List.of());

        mockMvc.perform(get("/Hello").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("articles", List.of()));
    }

}