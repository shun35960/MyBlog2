package com.example.MyBlog.Repository;

import com.example.MyBlog.Entity.Series;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * SeriesRepositoryは、MongoDBを使用してSeriesエンティティに対するCRUD操作を提供します。
 * Spring Data MongoDBのMongoRepositoryを拡張しています。
 */

public interface SeriesRepository extends MongoRepository<Series, String> {
}
