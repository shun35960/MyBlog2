package com.example.MyBlog.Repository;

import com.example.MyBlog.Entity.Article;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MyBlogRepositoryTest
 * リポジトリクラスのテスト
 * MongoDBを使用しているため、DataMongoTestを使用してリポジトリのテストを行う
 * MyBlogRepositoryクラスのメソッドをテストする
 */
@DataMongoTest
@AutoConfigureDataMongo
@ActiveProfiles("test")
@Testcontainers
public class MyBlogRepositoryTest {
    private static final String TEST_DATABASE_NAME = "myblog_test";

    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void configureMongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongoDBContainer.getReplicaSetUrl(TEST_DATABASE_NAME));
    }

    @Autowired
    private MyBlogRepository myBlogRepository;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        String databaseName = mongoTemplate.getDb().getName();
        assertTrue(
                databaseName.toLowerCase(Locale.ROOT).contains("test"),
                "Unsafe MongoDB database for tests: " + databaseName
        );
        // Clean up the repository before each test
        myBlogRepository.deleteAll();
    }

    // Add your test methods here
    @Test
    void getAllBlogs() {
        // Add test data
        Article article1 = new Article("id1", "Title 1", "Content 1", true, new Date());
        Article article2 = new Article("id2", "Title 2", "Content 2", false, new Date());

        myBlogRepository.save(article1);
        myBlogRepository.save(article2);

        // Call the method to be tested
        List<Article> articles = myBlogRepository.findAll();

        // Assertions
        assertEquals(2, articles.size());
        assertEquals("Title 1", articles.get(0).title());
        assertEquals("Title 2", articles.get(1).title());

    }

    @Test
    void findByPublishedTrue() {
        // Add test data
        Article article1 = new Article("id1", "Title 1", "Content 1", true, new Date());
        Article article2 = new Article("id2", "Title 2", "Content 2", false, new Date());

        myBlogRepository.save(article1);
        myBlogRepository.save(article2);

        // Call the method to be tested
        List<Article> articles = myBlogRepository.findByPublishedTrue();

        // Assertions
        assertEquals(1, articles.size());
        //article1が公開状態trueのため、1件取得される
        assertTrue(articles.getFirst().published());
        //公開状態がtrueのものを取得するメソッドをテスト
        assertEquals("id1", articles.getFirst().id());


    }

    @Test
    void findByPublishedFalse() {
        // Add test data
        Article article1 = new Article("id1", "Title 1", "Content 1", true, new Date());
        Article article2 = new Article("id2", "Title 2", "Content 2", false, new Date());

        myBlogRepository.save(article1);
        myBlogRepository.save(article2);

        // Call the method to be tested
        List<Article> articles = myBlogRepository.findByPublishedFalse();

        // Assertions
        assertEquals(1, articles.size());
        //article2が公開状態falseのため、1件取得される
        assertFalse(articles.getFirst().published());
        //公開状態がfalseのものを取得するメソッドをテスト
        assertEquals("id2", articles.getFirst().id());

    }

}
