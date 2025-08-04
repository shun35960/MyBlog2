package com.example.MyBlog.Controller;

import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Service.MyBlogService;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * MyBlogControllerは、ブログアプリケーションのコントローラーです。
 * 記事の一覧表示、記事の編集、記事の詳細表示などの機能を提供します。
 * MyBlogServiceを使用してデータベースから記事を取得し、Flexmarkを使用してMarkdownをHTMLに変換します。
 */


@Controller
@RequiredArgsConstructor
@RequestMapping("/Hello")
public class MyBlogController {

    private final MyBlogService myBlogService;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;
    //MarkdownのパーサーとHTMLレンダラーを初期化

    private final PolicyFactory POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.STYLES)
            .and(Sanitizers.TABLES)
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.IMAGES);

    // http://localhost:8080/Hello -> Hello.htmlとモデルを呼び出し
    @GetMapping
    public String hello(Model model) {
        model.addAttribute("Hellotitle", "記事の一覧!");
        List<Article> articleList = myBlogService.findArticlePublishedTrue();
        //System.out.println("getting" + articleList.size());
        model.addAttribute("articles", articleList);
        //System.out.println("found" + articleList.size());
        return "Hello";
    }

    //~/edit -> Edit.htmlとモデルを呼び出し
    @GetMapping("/Edit")
    public String edit(Model model) {
        model.addAttribute("article", Article.newArticle());
        //model.addAttribute("Edit", "入力項目を入力してください");
        return "Edit";
    }

    @GetMapping("/Edit/{id}")
    public String editArticle(@PathVariable("id") String id, Model model) {
        Article article = myBlogService.findArticleById(id);
        model.addAttribute("article", article);
        //model.addAttribute("Edit", "入力項目を入力してください");
        return "Edit";
    }

    //EditのSubmitをしたら確認画面に遷移
    @PostMapping("/Submit")
    public String saveArticle(@PathVariable(value = "id", required = false) String id, @ModelAttribute Article article, Model model) {
        Article submitArticle = myBlogService.submitArticle(article);
        model.addAttribute("savedArticle", submitArticle);
        model.addAttribute("Submit", "登録完了");
        System.out.println("submit" + article);
        return "redirect:/Hello";
    }

    @PutMapping("/Submit/{id}")
    public String updateArticle(@PathVariable("id") String id, @ModelAttribute Article article, Model model) {
        Article updatedArticle = new Article(
                id, // IDはパスから取得
                article.title(), // タイトルはフォームから取得
                article.content(), // コンテンツはフォームから取得
                article.published(), // 公開状態はフォームから取得
                new Date() // 作成日時を現在の日時に更新
        );// 作成日時を現在の日時に更新
        Article savedArticle = myBlogService.submitArticle(updatedArticle);
        model.addAttribute("savedArticle", savedArticle);
        model.addAttribute("Submit", "更新完了");
        System.out.println("submit" + savedArticle);
        return "redirect:/Hello";
    }


    @GetMapping("/Description/{id}")
    public String showdescription(@PathVariable("id") String id, Model model) {
        //記事のIDを指定して記事を取得
        Article article = myBlogService.findArticleById(id);
        //MarkdownをHTMLに変換
        Node document = markdownParser.parse(article.content() != null ? article.content() : "");
        String renderedHtmlContent = htmlRenderer.render(document);

        String sanitizedHtmlContent = POLICY.sanitize(renderedHtmlContent);
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
