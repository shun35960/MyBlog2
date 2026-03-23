package com.example.MyBlog.Controller;

import com.example.MyBlog.Config.MarkdownConfig;
import com.example.MyBlog.Config.SecurityConfig;
import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Service.MyBlogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

@WebMvcTest(MyBlogController.class)
@Import({MarkdownConfig.class, SecurityConfig.class})
class MyBlogControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MyBlogService myBlogService;

    @Test
    void hello() throws Exception {
        mockMvc.perform(get("/Hello"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(view().name("Hello"));
    }

    @Test
    void edit() throws Exception {
        mockMvc.perform(get("/Hello/Edit"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(view().name("Edit"));
    }

    @Test
    void editArticle()  throws Exception {
        Article article = new Article("none", "testtitle", "testcontent", false, new Date());
        when(myBlogService.findArticleById("none")).thenReturn(article);

        mockMvc.perform(get("/Hello/Edit/none"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(view().name("Edit"))
                .andExpect(model().attribute("article", article));
    }

    @Test
    void saveArticle() throws Exception {
//        Article article = new Article("test", "testtitle", "testcontent", false, new Date());
//        when(myBlogService.submitArticle(article)).thenReturn(article);
        mockMvc.perform(post("/Hello/Submit")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("title", "testtitle")
                        .param("content", "testcontent")
                        .param("published", "false"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
    }

    @Test
    void updateArticle() throws Exception {
        Article article = new Article("test", "testtitle", "testcontent", true, new Date());
        when(myBlogService.submitArticle(any())).thenReturn(article);
        mockMvc.perform(put("/Hello/Submit/test")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("title", "testtitle2")
                        .param("content", "testcontent2")
                        .param("published", "true"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
    }

    @Test
    void showdescription() throws Exception {
        Article article = new Article("test", "testtitle2", "testcontent2", false, new Date());
        when(myBlogService.findArticleById("test")).thenReturn(article);
        mockMvc.perform(get("/Hello/Description/test"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(view().name("Description"));
    }

    @Test
    void showDraft() throws Exception {
        mockMvc.perform(get("/Hello/Draft"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(view().name("Draft"));
    }

    @Test
    void deleteArticle() throws Exception {
        Article article = new Article("test", "testtitle", "testcontent", false, new Date());
        mockMvc.perform(delete("/Hello/test")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("id", "test"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
    }
}