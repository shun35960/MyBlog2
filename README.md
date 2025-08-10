# MyBlog

ブログの投稿と管理ができるWebアプリケーションです。

## 使用技術

### フロントエンド (Frontend)
- Thymeleaf (テンプレートエンジン)
- Bootstrap 5.3.0
- HTML, CSS, JavaScript
- Thymeleaf Layout Dialect

### バックエンド
- Java 23
- Spring Boot 3.4.5
- Spring Security
- Spring Data MongoDB
- Lombok
- Flexmark (Markdownパーサー)

### データベース
- MongoDB

### 開発ツール
- Gradle
- Spring Boot DevTools
- JUnit (テストフレームワーク)

## 必要条件

- Java 23以上
- Gradle 8.x
- MongoDB
- IntelliJ IDEA (推奨) または他のJava IDE

## セットアップ手順

1. リポジトリをクローンします。
   ```bash
   git clone https://github.com/yourusername/MyBlog.git
   cd MyBlog
   ```

2. MongoDBをインストールして起動します。
   - MongoDBがインストールされていない場合は、[公式サイト](https://www.mongodb.com/try/download/community)からダウンロードしてインストールしてください。
   - MongoDBサービスが起動していることを確認してください。

3. Gradleを使用して依存関係をインストールし、ビルドします。
   ```bash
   ./gradlew clean build
   ```

4. アプリケーションを起動します。
   ```bash
   ./gradlew bootRun
   ```

## 主な機能

- ブログ記事の投稿、編集、削除
- Markdownによる記事作成
- ユーザー認証・認可
- レスポンシブデザイン

## テスト

プロジェクトには単体テストとセキュリティテストが含まれています。以下のコマンドでテストを実行できます：

```bash
./gradlew test
```


## 
## CI/DCパイプライン