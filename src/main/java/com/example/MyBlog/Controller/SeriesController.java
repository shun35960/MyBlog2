package com.example.MyBlog.Controller;

import java.util.List;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Entity.Series;
import com.example.MyBlog.Service.MyBlogService;
import com.example.MyBlog.Service.SeriesService;

/**
 * SeriesControllerは、シリーズ(連載)機能のコントローラーです。
 * シリーズの一覧表示、シリーズ内の公開記事の連結表示、シリーズの更新・削除を提供します。
 * MarkdownのレンダリングはMyBlogControllerと同じフロー(Flexmark → OWASPサニタイズ)を使用します。
 */

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/Hello/Series")
public class SeriesController {

    private final SeriesService seriesService;
    private final MyBlogService myBlogService;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;
    private final PolicyFactory htmlSanitizationPolicy;

    /**
     * シリーズ詳細ページ表示用(記事+レンダリング済みHTML)
     */
    public record ArticleView(Article article, String html) {
    }

    // シリーズ一覧(公開記事が1件以上あるシリーズのみ)
    @GetMapping
    public String seriesList(Model model) {
        List<SeriesService.SeriesSummary> summaries = seriesService.findPublishedSeriesSummaries();
        model.addAttribute("seriesSummaries", summaries);
        model.addAttribute("SeriesListTitle", "シリーズ一覧");
        log.debug("Series with published articles found: {}", summaries.size());
        return "SeriesList";
    }

    // シリーズ詳細:所属する公開記事を作成日時順に全文連結表示
    @GetMapping("/{id}")
    public String seriesDetail(@PathVariable("id") String id, Model model) {
        Series series = seriesService.findSeriesById(id);
        List<ArticleView> articleViews = myBlogService.findPublishedArticlesBySeriesId(id).stream()
                .map(article -> new ArticleView(article, renderMarkdown(article.content())))
                .toList();

        model.addAttribute("series", series);
        model.addAttribute("renderedDescription", renderMarkdown(series.description()));
        model.addAttribute("articleViews", articleViews);
        return "SeriesDetail";
    }

    // シリーズ名・説明の更新
    @PostMapping("/{id}")
    public String updateSeries(@PathVariable("id") String id,
                               @RequestParam("title") String title,
                               @RequestParam(value = "description", required = false) String description) {
        Series updatedSeries = seriesService.updateSeries(id, title, description != null ? description : "");
        log.info("Series updated: id={}, title={}", id, updatedSeries.title());
        return "redirect:/Hello/Series/" + id;
    }

    // シリーズの削除(所属記事は残り、紐付けのみ解除される)
    @DeleteMapping("/{id}")
    public String deleteSeries(@PathVariable("id") String id) {
        seriesService.deleteSeries(id);
        log.info("Series deleted: id={}", id);
        return "redirect:/Hello/Series";
    }

    private String renderMarkdown(String content) {
        Node document = markdownParser.parse(content != null ? content : "");
        String renderedHtmlContent = htmlRenderer.render(document);
        return htmlSanitizationPolicy.sanitize(renderedHtmlContent);
    }
}
