package com.example.MyBlog.Config;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MarkdownConfigは、Flexmarkを使用してMarkdownのパースとHTMLレンダリングを設定するためのコンフィグクラスです。
 * Flexmarkのオプションや拡張機能を設定できます。
 */
@Configuration
public class MarkdownConfig {
    // Markdownの設定をここに追加できます
    // 例: Parser、HtmlRendererの設定など
    // MutableDataSet options = new MutableDataSet();
    // Parser parser = Parser.builder(options).build();
    // HtmlRenderer renderer = HtmlRenderer.builder(options).build();
    @Bean
    public MutableDataSet flexmarkOptions() {
        MutableDataSet options = new MutableDataSet();
        // ここでflexmarkのオプションや拡張機能を設定
        // 例: テーブル拡張機能を追加
        // options.set(Parser.EXTENSIONS, Arrays.asList(
        //         TablesExtension.create(),
        //         StrikethroughExtension.create()
        // ));
        // options.set(HtmlRenderer.SOFT_BREAK, "<br />\n"); // 改行の扱いなど
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
}
