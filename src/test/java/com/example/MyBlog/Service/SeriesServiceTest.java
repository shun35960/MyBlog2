package com.example.MyBlog.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Entity.Series;
import com.example.MyBlog.Repository.MyBlogRepository;
import com.example.MyBlog.Repository.SeriesRepository;

/**
 * SeriesServiceTest
 * シリーズサービスクラスのテスト
 * mockitoを使用してリポジトリをモック化し、サービスメソッドのテストを行う
 * SeriesServiceImplクラスのメソッドをテストする
 */
@ExtendWith(MockitoExtension.class)
public class SeriesServiceTest {

    @Mock
    private SeriesRepository seriesRepository;

    @Mock
    private MyBlogRepository myBlogRepository;

    @InjectMocks
    private SeriesServiceImpl seriesService;

    @Test
    void test_findAllSeries() {
        Series series1 = new Series("s1", "シリーズ1", "説明1", new Date());
        Series series2 = new Series("s2", "シリーズ2", "説明2", new Date());
        when(seriesRepository.findAll()).thenReturn(List.of(series1, series2));

        List<Series> result = seriesService.findAllSeries();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(seriesRepository).findAll();
    }

    @Test
    void test_findSeriesById() {
        Series series = new Series("s1", "シリーズ1", "説明1", new Date());
        when(seriesRepository.findById("s1")).thenReturn(Optional.of(series));

        Series result = seriesService.findSeriesById("s1");

        assertNotNull(result);
        assertEquals("s1", result.id());
        assertEquals("シリーズ1", result.title());
        verify(seriesRepository).findById("s1");
    }

    @Test
    void findSeriesById_存在しないID_例外がスローされる() {
        // Arrange
        when(seriesRepository.findById("notExist")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> seriesService.findSeriesById("notExist"));
        verify(seriesRepository).findById("notExist");
    }

    @Test
    void findPublishedSeriesSummaries_公開記事のないシリーズは除外される() {
        // Arrange
        Series series1 = new Series("s1", "シリーズ1", "説明1", new Date());
        Series series2 = new Series("s2", "シリーズ2", "説明2", new Date());
        when(seriesRepository.findAll()).thenReturn(List.of(series1, series2));
        when(myBlogRepository.countBySeriesIdAndPublishedTrue("s1")).thenReturn(3L);
        when(myBlogRepository.countBySeriesIdAndPublishedTrue("s2")).thenReturn(0L);

        // Act
        List<SeriesService.SeriesSummary> result = seriesService.findPublishedSeriesSummaries();

        // Assert
        assertEquals(1, result.size());
        assertEquals("s1", result.getFirst().series().id());
        assertEquals(3L, result.getFirst().publishedCount());
    }

    @Test
    void test_createSeries() {
        Series saved = new Series("s1", "新シリーズ", "", new Date());
        when(seriesRepository.save(any(Series.class))).thenReturn(saved);

        Series result = seriesService.createSeries("新シリーズ");

        assertNotNull(result);
        assertEquals("s1", result.id());
        ArgumentCaptor<Series> captor = ArgumentCaptor.forClass(Series.class);
        verify(seriesRepository).save(captor.capture());
        assertNull(captor.getValue().id());
        assertEquals("新シリーズ", captor.getValue().title());
    }

    @Test
    void updateSeries_タイトルと説明が更新されIDと作成日時は維持される() {
        // Arrange
        Date createdAt = new Date();
        Series existing = new Series("s1", "旧タイトル", "旧説明", createdAt);
        when(seriesRepository.findById("s1")).thenReturn(Optional.of(existing));
        when(seriesRepository.save(any(Series.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Series result = seriesService.updateSeries("s1", "新タイトル", "新説明");

        // Assert
        assertEquals("s1", result.id());
        assertEquals("新タイトル", result.title());
        assertEquals("新説明", result.description());
        assertEquals(createdAt, result.createdAt());
    }

    @Test
    void deleteSeries_所属記事の紐付けが解除されてからシリーズが削除される() {
        // Arrange
        Article article = new Article("a1", "Title 1", "Content 1", true, "s1", new Date());
        when(myBlogRepository.findBySeriesId("s1")).thenReturn(List.of(article));

        // Act
        seriesService.deleteSeries("s1");

        // Assert
        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(myBlogRepository).save(captor.capture());
        assertNull(captor.getValue().seriesId());
        assertEquals("a1", captor.getValue().id());
        verify(seriesRepository).deleteById("s1");
    }

    @Test
    void deleteSeries_所属記事がない場合もシリーズは削除される() {
        // Arrange
        when(myBlogRepository.findBySeriesId("s1")).thenReturn(List.of());

        // Act
        seriesService.deleteSeries("s1");

        // Assert
        verify(myBlogRepository, never()).save(any());
        verify(seriesRepository).deleteById("s1");
    }
}
