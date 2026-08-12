package com.example.MyBlog.Service;

import com.example.MyBlog.Entity.Series;

import java.util.List;

public interface SeriesService {

    /**
     * シリーズ一覧ページ表示用のサマリ(シリーズ+公開記事数)
     */
    record SeriesSummary(Series series, long publishedCount) {
    }

    List<Series> findAllSeries();
    //全シリーズを取得(Edit画面のセレクトボックス用)

    Series findSeriesById(String id);
    //idを指定してシリーズを取得

    List<SeriesSummary> findPublishedSeriesSummaries();
    //公開記事が1件以上あるシリーズのサマリを取得(一覧ページ用)

    Series createSeries(String title);
    //シリーズの新規作成

    Series updateSeries(String id, String title, String description);
    //シリーズ名・説明の更新

    void deleteSeries(String id);
    //シリーズの削除(所属記事のseriesIdはnullに戻す)
}
