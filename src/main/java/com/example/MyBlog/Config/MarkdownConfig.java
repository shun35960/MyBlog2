package com.example.MyBlog.Config;

import java.util.Arrays;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MarkdownConfigは、Flexmarkを使用してMarkdownのパースとHTMLレンダリングを設定するためのコンフィグクラスです。
 * GFM（GitHub Flavored Markdown）の拡張機能を有効化し、以下の機能をサポートします：
 * - コードブロック（フェンスド・コードブロック）
 * - テーブル
 * - 打ち消し線
 * - タスクリスト（チェックボックス）
 * - 自動リンク
 * - 順序付き/順序なしリスト
 */
@Configuration
public class MarkdownConfig {
    @Bean
    public MutableDataSet flexmarkOptions() {
        MutableDataSet options = new MutableDataSet();

        // GFM拡張機能を有効化
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),           // テーブルサポート
                StrikethroughExtension.create(),    // 打ち消し線サポート（~~text~~）
                TaskListExtension.create(),         // タスクリスト（- [ ] / - [x]）
                AutolinkExtension.create()          // 自動リンク変換
        ));

        // 改行の扱いを設定
        options.set(HtmlRenderer.SOFT_BREAK, " \n");
        options.set(HtmlRenderer.HARD_BREAK, "\n");

        // コードブロックの言語指定クラスプレフィックスを有効化
        options.set(HtmlRenderer.FENCED_CODE_LANGUAGE_CLASS_PREFIX, "language-");

        // コードブロック内の改行を保持
        options.set(HtmlRenderer.ESCAPE_HTML, false);

        return options;
    }

    @Bean
    public Parser flexmarkParser(MutableDataSet options) {
        return Parser.builder(options).build();
    }

    @Bean
    public HtmlRenderer flexmarkHtmlRenderer(MutableDataSet options) {
        return HtmlRenderer.builder(options).build();
    }

    /**
     * Markdownレンダリング結果用のカスタムサニタイザーポリシー
     * preタグとcodeタグ内の改行と空白を保持します
     */
    @Bean
    public PolicyFactory htmlSanitizationPolicy() {
        return new HtmlPolicyBuilder()
                // 基本的なテキストフォーマット
                .allowElements("p", "br", "strong", "b", "em", "i", "u", "s", "del", "ins", "mark", "small", "sub", "sup")
                // リスト
                .allowElements("ul", "ol", "li")
                // テーブル
                .allowElements("table", "thead", "tbody", "tfoot", "tr", "th", "td")
                // 引用・コード（重要）
                .allowElements("blockquote", "pre", "code")
                // ヘッダー
                .allowElements("h1", "h2", "h3", "h4", "h5", "h6")
                // リンク
                .allowElements("a")
                .allowAttributes("href").onElements("a")
                .allowStandardUrlProtocols()
                // 画像
                .allowElements("img")
                .allowAttributes("src", "alt", "title", "width", "height").onElements("img")
                .allowStandardUrlProtocols()
                // preタグ内のクラス属性（言語指定用）
                .allowAttributes("class").onElements("pre", "code")
                .allowAttributes("data-language").onElements("pre", "code")
                // hr - 水平線
                .allowElements("hr")
                .toFactory();
    }
}
