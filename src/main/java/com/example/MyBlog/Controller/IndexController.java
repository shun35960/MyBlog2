package com.example.MyBlog.Controller;

import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Entity.Series;
import com.example.MyBlog.Service.MyBlogService;
import com.example.MyBlog.Service.SeriesService;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import lombok.RequiredArgsConstructor;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.vladsch.flexmark.util.ast.Node;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * IndexControllerは、アプリケーションのインデックスページを表示するためのコントローラーです。
 * MyBlogServiceを使用して記事を取得し、index.htmlテンプレートに渡します。
 * 未ログイン者のためにビューを渡す
 */
@Controller
@RequiredArgsConstructor
public class IndexController {

    private final MyBlogService myBlogService;
    private final SeriesService seriesService;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;
    private final PolicyFactory htmlSanitizationPolicy;

    @Value("${myblog.about.article-id}")
    private String aboutArticleId;

    // index.htmlを返すためのメソッド
    @GetMapping("/")
    public String index(Model model) {
        List<Article> articleList = myBlogService.findArticlePublishedTrue();
        model.addAttribute("articles", articleList);
        //シリーズバッジ表示用にシリーズID→シリーズ名のマップを渡す
        //Collectors.toMap は key/value が null だと NPE を投げるため、事前に除外する
        Map<String, String> seriesTitles = seriesService.findAllSeries().stream()
                .filter(series -> series.id() != null && series.title() != null)
                .collect(Collectors.toMap(Series::id, Series::title));
        model.addAttribute("seriesTitles", seriesTitles);
        model.addAttribute("Indextitle", "ようこそ");
        return "index"; // index.htmlを返す
    }

    //~/Series -> 未ログイン者向けのシリーズ一覧(管理機能なし)
    @GetMapping("/Series")
    public String viewSeriesList(Model model) {
        model.addAttribute("seriesSummaries", seriesService.findPublishedSeriesSummaries());
        model.addAttribute("SeriesListTitle", "シリーズ一覧");
        return "ViewSeriesList"; // ViewSeriesList.htmlを返す
    }

    //~/Series/{id} -> 未ログイン者向けのシリーズ詳細(公開記事を作成日時順に連結表示・管理機能なし)
    @GetMapping("/Series/{id}")
    public String viewSeriesDetail(Model model, @PathVariable String id) {
        Series series = seriesService.findSeriesById(id);
        List<SeriesController.ArticleView> articleViews = myBlogService.findPublishedArticlesBySeriesId(id).stream()
                .map(article -> new SeriesController.ArticleView(article, renderMarkdown(article.content())))
                .toList();
        model.addAttribute("series", series);
        model.addAttribute("renderedDescription", renderMarkdown(series.description()));
        model.addAttribute("articleViews", articleViews);
        return "ViewSeriesDetail"; // ViewSeriesDetail.htmlを返す
    }

    private String renderMarkdown(String content) {
        Node document = markdownParser.parse(content != null ? content : "");
        return htmlSanitizationPolicy.sanitize(htmlRenderer.render(document));
    }

    //~/About-> 筆者紹介の記事を返す
    @GetMapping("/about")
    public String About(Model model) {
        Article article = myBlogService.findArticleById(aboutArticleId);
        Node document = markdownParser.parse(article.content() != null ? article.content() : "");
        String renderedHtmlContent = htmlRenderer.render(document);
        String sanitizedHtmlContent = htmlSanitizationPolicy.sanitize(renderedHtmlContent);
        model.addAttribute("article", article);
        model.addAttribute("renderedHtmlContent", sanitizedHtmlContent);
        return "ViewDescription"; // ViewDescription.htmlを返す
    }

    //~/Privacy-> プライバシーポリシーのページを返す
    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("Indextitle", "プライバシーポリシー");
        return "Privacy"; // Privacy.htmlを返す
    }

    @GetMapping("/ViewDescription/{id}")
    public String viewDescription(Model model, @PathVariable String id) {
        Article article = myBlogService.findArticleById(id);
        Node document = markdownParser.parse(article.content() != null ? article.content() : "");
        String renderedHtmlContent = htmlRenderer.render(document);
        String sanitizedHtmlContent = htmlSanitizationPolicy.sanitize(renderedHtmlContent);
        model.addAttribute("article", article);
        model.addAttribute("renderedHtmlContent", sanitizedHtmlContent);
        return "ViewDescription"; // ViewDescription.htmlを返す
    }

}
