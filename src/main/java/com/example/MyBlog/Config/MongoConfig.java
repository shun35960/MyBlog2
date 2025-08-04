package com.example.MyBlog.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * このクラスは、MongoDBの監査機能を有効にするための設定クラスです。
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
