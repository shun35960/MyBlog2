package com.example.MyBlog.Controller;

import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Service.MyBlogService;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import lombok.RequiredArgsConstructor;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.vladsch.flexmark.util.ast.Node;

import java.util.List;

/**
 * IndexControllerは、アプリケーションのインデックスページを表示するためのコントローラーです。
 * MyBlogServiceを使用して記事を取得し、index.htmlテンプレートに渡します。
 * 未ログイン者のためにビューを渡す
 */
@Controller
@RequiredArgsConstructor
public class IndexController {

    private final MyBlogService myBlogService;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;
    private final PolicyFactory htmlSanitizationPolicy;

    // index.htmlを返すためのメソッド
    @GetMapping("/")
    public String index(Model model) {
        List<Article> articleList = myBlogService.findArticlePublishedTrue();
        model.addAttribute("articles", articleList);
        model.addAttribute("Indextitle", "ようこそ");
        return "index"; // index.htmlを返す
    }

    //~/About-> About.html
    @GetMapping("/about")
    public String About() {
        return "About";
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
