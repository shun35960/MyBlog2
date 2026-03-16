package com.example.MyBlog.Controller;

import com.example.MyBlog.Config.MarkdownConfig;
import com.example.MyBlog.Service.ImageServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest(MyBlogController.class)
@Import({SecurityAutoConfiguration.class, MarkdownConfig.class})
class MyBlogControllerTest {

    @Test
    void hello() {
    }

    @Test
    void edit() {
    }

    @Test
    void editArticle() {
    }

    @Test
    void saveArticle() {
    }

    @Test
    void updateArticle() {
    }

    @Test
    void showdescription() {
    }

    @Test
    void showDraft() {
    }

    @Test
    void deleteArticle() {
    }
}