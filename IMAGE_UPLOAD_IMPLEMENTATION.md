# MyBlog2 画像投稿機能の実装と修正

## 概要

MyBlog2プロジェクトに画像投稿機能を実装しました。ユーザーが記事作成時に画像をアップロードでき、Markdown形式で自動的に記事に埋め込まれる機能です。

## 実装内容

### 1. バックエンド実装

#### ImageService インターフェース (`src/main/java/com/example/MyBlog/Service/ImageService.java`)
- `storeImage(MultipartFile file, String uploadedBy)` - 画像の保存
- `getImage(String fileId)` - 画像の取得
- `deleteImage(String fileId)` - 画像の削除

#### ImageServiceImpl 実装クラス (`src/main/java/com/example/MyBlog/Service/ImageServiceImpl.java`)
- GridFSを使用したMongoDB内での画像保存
- ファイルバリデーション（形式、サイズチェック）
- ファイル名のサニタイゼーション（セキュリティ対策）

#### ImageController (`src/main/java/com/example/MyBlog/Controller/ImageController.java`)
- `POST /api/images/upload` - ファイルアップロードエンドポイント
- `GET /api/images/{fileId}` - 画像取得エンドポイント
- `DELETE /api/images/{fileId}` - 画像削除エンドポイント

#### ImageConfig (`src/main/java/com/example/MyBlog/Config/ImageConfig.java`)
- GridFSBucketの設定

### 2. フロントエンド実装

#### Edit.html テンプレート (`src/main/resources/templates/Edit.html`)
- ドラッグ&ドロップ対応のファイルアップロードUI
- ファイル選択ボタン
- アップロード進捗バー表示
- アップロード済み画像リスト表示

**主な機能：**
- ファイル形式検証（JPEG, PNG, GIF, WebP）
- ファイルサイズ検証（最大100MB）
- XMLHttpRequestを使用したアップロード
- アップロード完了後、自動的にMarkdown形式で記事内容に挿入
- 削除機能

### 3. セキュリティ設定

#### SecurityConfig (`src/main/java/com/example/MyBlog/Config/SecurityConfig.java`)
```java
.csrf(csrf -> csrf
    .ignoringRequestMatchers("/api/images/**")
)
```
- 画像APIのCSRF保護を除外

#### layout.html (`src/main/resources/templates/layout/layout.html`)
```html
<meta name="_csrf" th:content="${_csrf.token}" />
<meta name="_csrf_header" th:content="${_csrf.headerName}" />
```
- CSRFトークンメタタグの追加

### 4. 設定ファイル

#### application.properties
```properties
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
```
- マルチパートファイルのサイズ制限設定

## 発生した問題と解決策

### 問題1: CSRF 403 エラー

**症状：**
画像アップロード時に403（Forbidden）エラーが発生していた。

**原因：**
XMLHttpRequestのヘッダーにCSRFトークンが含まれていなかった。

**解決策：**
Edit.html のJavaScriptで、リクエスト前にCSRFトークンを取得してヘッダーに追加：
```javascript
const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
if (csrfToken && csrfHeader) {
    xhr.setRequestHeader(csrfHeader, csrfToken);
}
```

### 問題2: イベントリスナーが機能していない

**症状：**
ファイル選択ボタンをクリックしても反応がなかった。

**原因：**
スクリプトがDOM要素の読み込み前に実行されていた（DOMContentLoadedイベント外）。

**解決策：**
スクリプト全体を`DOMContentLoaded`イベント内にラップ：
```javascript
document.addEventListener('DOMContentLoaded', function() {
    // すべてのイベント設定をここに移動
});
```

### 問題3: 画像が表示されない（重大な問題）

**症状：**
- アップロード後、記事表示ページで画像が表示されない
- 画像URLに直接アクセスすると「null null」エラーが出ていた

**原因：**
`ImageServiceImpl.java`で、アップロード時に`GridFSBucket.uploadFromStream()`を使用し、取得時に`GridFsTemplate.findOne()`を使用していた。これらが異なるデータストアを参照していた。

```java
// アップロード時
ObjectId fileId = gridFSBucket.uploadFromStream(...);

// 取得時
GridFSFile gridFSFile = gridFsTemplate.findOne(query);
```

**解決策：**
`GridFsTemplate`に統一：
```java
// アップロード時
ObjectId fileId = gridFsTemplate.store(
    inputStream,
    sanitizedFilename,
    file.getContentType(),
    metadata
);

// 削除時
Query query = new Query(Criteria.where("_id").is(objectId));
gridFsTemplate.delete(query);
```

## 修正後の動作確認

### テスト手順
1. 新規記事作成ページ（`/Hello/Edit`）へアクセス
2. タイトルと内容を入力
3. ファイル選択ボタンから画像をアップロード
4. 自動的にMarkdown形式が記事内容に挿入される
5. 「公開」を選択して記事を登録
6. 記事詳細ページで画像が正しく表示される

### 結果
✅ 画像が正しくGridFSに保存される
✅ 記事表示ページで画像が正しく表示される
✅ 画像のブルーカラーが正しく表示される

## 技術スタック

- **バックエンド:** Java 25, Spring Boot 4.1.1, Spring Data MongoDB
- **データベース:** MongoDB (GridFS)
- **フロントエンド:** Thymeleaf, Bootstrap 5.3.0, Vanilla JavaScript
- **画像処理:** GridFS (MongoDB の分散ファイルストレージ)

## セキュリティ機能

- ファイル形式チェック（JPEG, PNG, GIF, WebP のみ許可）
- ファイルサイズチェック（最大100MB）
- ファイル名のサニタイゼーション（パストラバーサル攻撃防止）
- CSRF保護
- 認証ユーザーのみアップロード可能

## 修正されたファイル一覧

1. `src/main/java/com/example/MyBlog/Service/ImageServiceImpl.java`
   - GridFsTemplateの統一
   - アップロード・削除メソッドの修正

2. `src/main/resources/templates/Edit.html`
   - DOMContentLoadedイベント内でのスクリプト実行
   - CSRFトークンの取得と送信

3. `src/main/resources/templates/layout/layout.html`
   - CSRFトークンメタタグの追加

4. `src/main/java/com/example/MyBlog/Config/SecurityConfig.java`
   - `/api/images/**` エンドポイントのCSRF保護除外

5. `src/main/resources/application.properties`
   - マルチパートファイルサイズ設定

## 今後の改善案

- 画像の圧縮機能
- サムネイル生成
- キャッシング機能
- アップロード履歴管理
- 複数画像の一括アップロード対応
- 画像の編集機能（クロップ、リサイズなど）

---

**実装完了日:** 2026-01-05
