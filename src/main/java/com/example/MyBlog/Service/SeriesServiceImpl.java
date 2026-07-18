package com.example.MyBlog.Service;

import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Entity.Series;
import com.example.MyBlog.Repository.MyBlogRepository;
import com.example.MyBlog.Repository.SeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SeriesServiceImplは、SeriesServiceインターフェースの実装クラスです。
 * シリーズの取得、作成、更新、削除のビジネスロジックを提供します。
 * シリーズ削除時は所属記事のseriesIdをnullに戻し、記事自体は削除しません。
 */

@Service
@RequiredArgsConstructor
public class SeriesServiceImpl implements SeriesService {

    private final SeriesRepository seriesRepository;
    private final MyBlogRepository myBlogRepository;

    @Override
    public List<Series> findAllSeries() {
        return seriesRepository.findAll();
    }

    @Override
    public Series findSeriesById(String id) {
        return seriesRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Series not found id =" + id));
    }

    @Override
    public List<SeriesSummary> findPublishedSeriesSummaries() {
        return seriesRepository.findAll().stream()
                .map(series -> new SeriesSummary(series, myBlogRepository.countBySeriesIdAndPublishedTrue(series.id())))
                .filter(summary -> summary.publishedCount() > 0)
                .toList();
    }

    @Override
    public Series createSeries(String title) {
        return seriesRepository.save(Series.newSeries(title));
    }

    @Override
    public Series updateSeries(String id, String title, String description) {
        Series existingSeries = findSeriesById(id);
        Series updateSeries = new Series(
                existingSeries.id(),
                title,
                description,
                existingSeries.createdAt()
        );
        return seriesRepository.save(updateSeries);
    }

    @Override
    public void deleteSeries(String id) {
        // 所属記事の紐付けを解除してからシリーズを削除する(記事は残す)
        List<Article> articles = myBlogRepository.findBySeriesId(id);
        for (Article article : articles) {
            myBlogRepository.save(new Article(
                    article.id(),
                    article.title(),
                    article.content(),
                    article.published(),
                    null,
                    article.createdAt()
            ));
        }
        seriesRepository.deleteById(id);
    }
}
