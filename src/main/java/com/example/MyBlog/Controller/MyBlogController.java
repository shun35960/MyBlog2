package com.example.MyBlog.Controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

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
 * MyBlogControllerは、ブログアプリケーションのコントローラーです。
 * 記事の一覧表示、記事の編集、記事の詳細表示などの機能を提供します。
 * MyBlogServiceを使用してデータベースから記事を取得し、Flexmarkを使用してMarkdownをHTMLに変換します。
 */


@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/Hello")
public class MyBlogController {

    private final MyBlogService myBlogService;
    private final SeriesService seriesService;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;
    private final PolicyFactory htmlSanitizationPolicy;
    //MarkdownのパーサーとHTMLレンダラーを初期化

    // http://localhost:8080/Hello -> Hello.htmlとモデルを呼び出し
    @GetMapping
    public String hello(Model model) {
        model.addAttribute("Hellotitle", "記事の一覧!");
        List<Article> articleList = myBlogService.findArticlePublishedTrue();
        model.addAttribute("articles", articleList);
        //シリーズバッジ表示用にシリーズID→シリーズ名のマップを渡す
        Map<String, String> seriesTitles = seriesService.findAllSeries().stream()
                .collect(Collectors.toMap(Series::id, Series::title));
        model.addAttribute("seriesTitles", seriesTitles);
        log.debug("Published articles found: {}", articleList.size());
        return "Hello";
    }

    //~/edit -> Edit.htmlとモデルを呼び出し
    @GetMapping("/Edit")
    public String edit(Model model) {
        model.addAttribute("article", Article.newArticle());
        model.addAttribute("seriesList", seriesService.findAllSeries());
        //model.addAttribute("Edit", "入力項目を入力してください");
        return "Edit";
    }

    @GetMapping("/Edit/{id}")
    public String editArticle(@PathVariable("id") String id, Model model) {
        Article article = myBlogService.findArticleById(id);
        model.addAttribute("article", article);
        model.addAttribute("seriesList", seriesService.findAllSeries());
        //model.addAttribute("Edit", "入力項目を入力してください");
        return "Edit";
    }

    //EditのSubmitをしたら確認画面に遷移
    @PostMapping("/Submit")
    public String saveArticle(@PathVariable(value = "id", required = false) String id, @Valid @ModelAttribute Article article,
                              @RequestParam(value = "newSeriesTitle", required = false) String newSeriesTitle, Model model) {
        Article submitArticle = myBlogService.submitArticle(withResolvedSeries(article, newSeriesTitle));
        model.addAttribute("savedArticle", submitArticle);
        model.addAttribute("Submit", "登録完了");
        log.info("Article submitted: title={}, published={}", article.title(), article.published());
        return "redirect:/Hello";
    }

    @PutMapping("/Submit/{id}")
    public String updateArticle(@PathVariable("id") String id, @Valid @ModelAttribute Article article,
                                @RequestParam(value = "newSeriesTitle", required = false) String newSeriesTitle, Model model) {
        Article savedArticle = myBlogService.updateArticle(id, withResolvedSeries(article, newSeriesTitle));
        model.addAttribute("savedArticle", savedArticle);
        model.addAttribute("Submit", "更新完了");
        log.info("Article updated: id={}, title={}, published={}", id, savedArticle.title(), savedArticle.published());
        return "redirect:/Hello";
    }

    /**
     * フォーム入力からseriesIdを確定する。
     * 新規シリーズ名が入力されていればシリーズを作成してそのIDを使い、
     * セレクトボックス未選択(空文字)はnull(未所属)に正規化する。
     */
    private Article withResolvedSeries(Article article, String newSeriesTitle) {
        String seriesId = article.seriesId();
        if (newSeriesTitle != null && !newSeriesTitle.isBlank()) {
            seriesId = seriesService.createSeries(newSeriesTitle.trim()).id();
        } else if (seriesId != null && seriesId.isBlank()) {
            seriesId = null;
        }
        return new Article(article.id(), article.title(), article.content(), article.published(), seriesId, article.createdAt());
    }


    @GetMapping("/Description/{id}")
    public String showdescription(@PathVariable("id") String id, Model model) {
        //記事のIDを指定して記事を取得
        Article article = myBlogService.findArticleById(id);
        //MarkdownをHTMLに変換
        Node document = markdownParser.parse(article.content() != null ? article.content() : "");
        String renderedHtmlContent = htmlRenderer.render(document);

        log.debug("=== Markdown Rendering Debug ===");
        log.debug("Flexmark output (first 500 chars): {}", renderedHtmlContent.substring(0, Math.min(500, renderedHtmlContent.length())));
        log.debug("Contains <pre>: {}", renderedHtmlContent.contains("<pre"));
        log.debug("Contains <code>: {}", renderedHtmlContent.contains("<code"));

        String sanitizedHtmlContent = htmlSanitizationPolicy.sanitize(renderedHtmlContent);

        log.debug("After sanitization (first 500 chars): {}", sanitizedHtmlContent.substring(0, Math.min(500, sanitizedHtmlContent.length())));
        log.debug("Sanitized contains <pre>: {}", sanitizedHtmlContent.contains("<pre"));
        log.debug("=== End Debug ===");

        model.addAttribute("description", article);
        model.addAttribute("renderedHtmlContent", sanitizedHtmlContent);
        return "Description";
    }

    @GetMapping("/Draft")
    public String showDraft(Model model) {
        List<Article> articleList = myBlogService.findArticlePublishedFalse();
        model.addAttribute("drafts", articleList);
        model.addAttribute("Drafttitle", "下書き一覧");
        return "Draft";
    }

    @DeleteMapping("{id}")
    public String deleteArticle(@PathVariable("id") String id) {
        myBlogService.deleteArticle(id);
        return "redirect:/Hello";
    }

}
