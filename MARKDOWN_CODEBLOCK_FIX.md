# Markdownコードブロック改行表示の修正

## 問題
Markdownのコードブロック（```で囲まれたテキスト）内の改行が反映されず、1行のテキストとして表示されていました。

## 原因分析
1. Flexmarkが```で囲まれたコードを`<code>`タグで生成していた
2. OWASPサニタイザーが`<pre>`タグと`<code>`タグの改行を削除していた
3. CSSで`white-space: pre-wrap`が指定されていなかった

## 実施した修正

### 1. MarkdownConfig.java - カスタムOWASPサニタイザーポリシーの作成

**ファイルパス:** `src/main/java/com/example/MyBlog/Config/MarkdownConfig.java:69-95行`

```java
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
```

**変更内容:**
- `<pre>`と`<code>`タグを明示的に許可
- `class`属性と`data-language`属性をコードブロックに許可
- Markdown出力用の完全なサニタイザーポリシーを定義

### 2. MyBlogController.java - カスタムポリシーの注入と使用

**ファイルパス:** `src/main/java/com/example/MyBlog/Controller/MyBlogController.java`

**変更1: インポートの整理（3-20行）**
```java
// 削除した不要なインポート
- import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
- import com.vladsch.flexmark.ext.tables.TablesExtension;
- import com.vladsch.flexmark.util.data.MutableDataSet;
- import org.springframework.beans.factory.annotation.Autowired;
- import org.owasp.html.Sanitizers;
```

**変更2: DI注入（34行）**
```java
private final PolicyFactory htmlSanitizationPolicy;
```

従来の`POLICY`フィールドをカスタムポリシーに置き換え:
```java
// 変更前
private final PolicyFactory POLICY = Sanitizers.FORMATTING
        .and(Sanitizers.LINKS)
        .and(Sanitizers.STYLES)
        .and(Sanitizers.TABLES)
        .and(Sanitizers.BLOCKS)
        .and(Sanitizers.IMAGES);

// 変更後
private final PolicyFactory htmlSanitizationPolicy;
```

**変更3: showdescription()メソッド（98行）**
```java
String sanitizedHtmlContent = htmlSanitizationPolicy.sanitize(renderedHtmlContent);
```

### 3. scrapbox-style.css - コードブロックの改行対応

**ファイルパス:** `src/main/resources/static/css/scrapbox-style.css:368-380行`

```css
.article-content code {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 3px;
  padding: 2px 6px;
  font-family: 'SF Mono', Monaco, 'Roboto Mono', Consolas, monospace;
  font-size: 0.9em;
  color: var(--text-primary);
  word-break: break-all;
  overflow-wrap: break-word;
  white-space: pre-wrap;  /* ← 追加: 改行を保持 */
  display: block;         /* ← 追加: ブロック要素化 */
}
```

**変更内容:**
- `white-space: pre-wrap` - ソースコード内の改行と空白を保持
- `display: block` - インライン要素からブロック要素に変更

## 結果

✅ コードブロック内の改行が正しく表示される
✅ フェンスドコードブロック（```～```）が複数行で表示される
✅ XSS対策は維持（カスタムポリシーで制御）
✅ ダークモード対応も保持

## 修正の流れ

1. **Flexmarkオプション設定** - デフォルト設定を確認（フェンスドコードブロック標準サポート）
2. **カスタムサニタイザー作成** - OWASP HTML SanitizerBuilderを使用
3. **DI注入と設定** - Springのコンポーネント化
4. **CSS調整** - ブラウザレンダリングの最適化
5. **ブラウザ検証** - 実際の表示確認

## 技術的なポイント

### なぜこれで解決したか
- **Flexmark** - フェンスドコードブロック（```）をサポート（拡張不要）
- **OWASP Sanitizer** - デフォルトポリシーは`<pre>`を削除していたため、カスタムポリシーで許可
- **CSS** - `white-space: pre-wrap`でHTMLが持つ改行を表示

### セキュリティ
- XSS対策は維持（ホワイトリスト方式のカスタムポリシー使用）
- 許可するHTML要素とアトリビュートを明示的に指定
- ユーザー入力Markdownは安全にサニタイズ

## 検証方法

ブラウザで以下にアクセスして改行が正しく表示されることを確認：
```
http://localhost:8080/Hello/Description/68f457d7aef9444567f1d5ad
```

コードブロック内の複数行が正しく改行されて表示されます。
