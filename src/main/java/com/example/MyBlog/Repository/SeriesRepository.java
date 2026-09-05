package com.example.MyBlog.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.MyBlog.Entity.Series;

/**
 * SeriesRepositoryは、MongoDBを使用してSeriesエンティティに対するCRUD操作を提供します。
 * Spring Data MongoDBのMongoRepositoryを拡張しています。
 */

public interface SeriesRepository extends MongoRepository<Series, String> {
}
