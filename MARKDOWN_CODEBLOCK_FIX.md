# Markdownコードブロック改行表示の修正

## 問題
Markdownのコードブロック（```で囲まれたテキスト）内の改行が反映されず、1行のテキストとして表示されていました。

## 原因分析（修正版）

### 第1段階：誤った分析と修正
1. Flexmarkが```で囲まれたコードを正しく`<pre><code>`タグで生成していることを確認
2. カスタムOWASPサニタイザーポリシーを作成し、`<pre>`タグを許可
3. CSSに`white-space: pre-wrap`と`display: block`を追加

### 第2段階：インラインコードとコードブロックの区別がない問題
- すべての`<code>`タグがブロック要素になり、インラインコードまでコードブロック化
- 原因：CSSで`.article-content code`に`display: block`を指定していた

### 第3段階：最終的な根本原因の発見
- `IndexController`で古い`POLICY`を使用していた
- `Sanitizers.FORMATTING`などのポリシーが`<pre>`タグを削除していた
- 結果として`IndexController`経由でアクセスすると、HTMLに`<pre>`タグがない
- `MyBlogController`経由ではカスタムポリシーが使用されていた

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

### 3. scrapbox-style.css - インラインコードとコードブロックの分離

**ファイルパス:** `src/main/resources/static/css/scrapbox-style.css`

**修正1: `.article-content code`（368-378行）- インラインコード用**
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
  /* 削除: white-space: pre-wrap; */
  /* 削除: display: block; */
}
```

**修正2: `.article-content pre code`（403-409行）- コードブロック用**
```css
.article-content pre code {
  background: none;
  border: none;
  padding: 0;
  display: block;           /* ← 追加: ブロック要素化 */
  white-space: pre-wrap;    /* ← 追加: 改行を保持 */
}
```

**変更内容:**
- インラインコード（`` `code` ``）：`<code>`単体でインライン表示
- コードブロック（` ```code``` `）：`<pre><code>`で改行を保持してブロック表示

### 4. IndexController.java - 古いサニタイザーポリシーの置き換え（最重要）

**ファイルパス:** `src/main/java/com/example/MyBlog/Controller/IndexController.java`

**問題:**
`/ViewDescription`エンドポイントで古い`POLICY`を使用していた：
```java
// 変更前（削除）
private final PolicyFactory POLICY = Sanitizers.FORMATTING
        .and(Sanitizers.LINKS)
        .and(Sanitizers.STYLES)
        .and(Sanitizers.TABLES)
        .and(Sanitizers.BLOCKS)     // ← これが<pre>タグを削除していた！
        .and(Sanitizers.IMAGES);
```

**修正内容:**
```java
// 変更後（DI注入）
private final PolicyFactory htmlSanitizationPolicy;

// viewDescriptionメソッド内で使用
String sanitizedHtmlContent = htmlSanitizationPolicy.sanitize(renderedHtmlContent);
```

**理由:**
- `Sanitizers.BLOCKS`ポリシーが`<pre>`タグを削除していた
- カスタムポリシー（`MarkdownConfig`で定義）に統一することで、`<pre>`タグが保持されるようになった

## 結果

✅ インラインコードと**コードブロック**が正しく区別される
✅ コードブロック内の改行が正しく表示される
✅ フェンスドコードブロック（` ```code``` `）が複数行で表示される
✅ インラインコード（`` `code` ``）はインライン表示
✅ XSS対策は維持（カスタムポリシーで制御）
✅ ダークモード対応も保持
✅ **両方の記事エンドポイント** (`/Hello/Description` と `/ViewDescription`) で正しく動作

## 修正の流れ

1. **Flexmarkオプション設定** - デフォルト設定を確認（フェンスドコードブロック標準サポート）
2. **カスタムサニタイザー作成** - OWASP HTML SanitizerBuilderを使用
3. **MyBlogController** - カスタムポリシーをDI注入で使用
4. **CSS調整** - CSSセレクタの優先順位を利用してインラインコードとコードブロックを分離
5. **IndexControllerの修正** - 古いポリシーを削除し、カスタムポリシーに統一
6. **ブラウザ検証** - 両エンドポイントで動作確認

## 技術的なポイント

### なぜこれで解決したか
- **Flexmark** - フェンスドコードブロック（```）を`<pre><code>`で正しく生成
- **OWASP Sanitizer**
  - 古いデフォルトポリシー：`Sanitizers.BLOCKS`が`<pre>`を削除
  - カスタムポリシー：`<pre>`と`<code>`を明示的に許可
- **CSS セレクタの優先順位**
  - `.article-content code` - インラインコード用（インライン表示）
  - `.article-content pre code` - コードブロック用（ブロック表示 + 改行保持）

### セキュリティ
- XSS対策は維持（ホワイトリスト方式のカスタムポリシー使用）
- 許可するHTML要素とアトリビュートを明示的に指定
- ユーザー入力Markdownは安全にサニタイズ
- 両方のコントローラーで同一のカスタムポリシーを使用

## 検証方法

ブラウザで両方のエンドポイントにアクセスして表示を確認：
```
# MyBlogController経由
http://localhost:8080/Hello/Description/68f457d7aef9444567f1d5ad

# IndexController経由（メインのビューページ）
http://localhost:8080/ViewDescription/68f4dac523fd7c402ad502ba
```

両ページでコードブロック内の複数行が正しく改行されて表示される。
インラインコードは背景色付きでインライン表示される。
