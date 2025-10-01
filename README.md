# MyBlog

Scrapboxにインスパイアされたデザインの日本語ブログアプリケーションです。Markdown記法での記事作成、ユーザー認証、そしてモダンなUIを備えています。

## 📋 目次

- [使用技術](#使用技術)
- [主な機能](#主な機能)
- [必要条件](#必要条件)
- [セットアップ手順](#セットアップ手順)
- [開発コマンド](#開発コマンド)
- [アーキテクチャ](#アーキテクチャ)
- [デプロイメント](#デプロイメント)
- [テスト](#テスト)
- [セキュリティ](#セキュリティ)

## 🛠 使用技術

### バックエンド
- **Java 23** - 最新のJava機能を活用
- **Spring Boot 3.4.5** - エンタープライズグレードのフレームワーク
- **Spring Security** - 認証・認可機能
- **Spring Data MongoDB** - データアクセス層
- **Lombok** - ボイラープレートコード削減
- **Flexmark 0.64.8** - Markdownパーサー（GFM拡張対応）
- **OWASP HTML Sanitizer** - XSS対策

### フロントエンド
- **Thymeleaf** - サーバーサイドテンプレートエンジン
- **Bootstrap 5.3.0** - レスポンシブデザイン
- **Thymeleaf Layout Dialect** - レイアウト管理
- カスタムCSS（Scrapbox風デザイン）

### データベース
- **MongoDB** - ドキュメント指向NoSQLデータベース
- **MongoDB Atlas** - 本番環境（クラウドホスティング）

### デプロイメント & インフラ
- **Docker** - コンテナ化（マルチステージビルド）
- **Docker Compose** - オーケストレーション
- **Nginx** - リバースプロキシ
- **GitHub Actions** - CI/CD パイプライン
- **Trivy** - セキュリティスキャン

### 開発ツール
- **Gradle 8.x** - ビルドツール
- **Spring Boot DevTools** - 開発効率化
- **JUnit 5** - テストフレームワーク
- **Mockito** - モックフレームワーク

## ✨ 主な機能

- ✍️ **Markdown記法での記事作成** - Flexmarkによる高機能なMarkdown対応
- 🔐 **ユーザー認証・認可** - Spring Securityによる安全な認証
- 📝 **記事の投稿・編集・削除** - CRUD操作完備
- 🎨 **Scrapbox風UI** - 親しみやすいデザイン
- 📱 **レスポンシブデザイン** - モバイル・デスクトップ対応
- 🛡️ **XSS対策** - OWASP HTML Sanitizerによる入力サニタイゼーション
- 🌐 **日本語対応** - UTF-8エンコーディング、Asia/Tokyoタイムゾーン

## 📦 必要条件

### 開発環境
- Java 23以上
- Gradle 8.x
- MongoDB 6.0以上（ローカル開発用）
- IntelliJ IDEA（推奨）または他のJava IDE

### 本番環境
- Docker & Docker Compose
- MongoDB Atlas アカウント（本番DB）
- Nginxサーバー

## 🚀 セットアップ手順

### 1. リポジトリのクローン

```bash
git clone https://github.com/yourusername/MyBlog2.git
cd MyBlog2
```

### 2. MongoDB のセットアップ

**ローカル開発の場合:**
```bash
# MongoDBをインストール（まだの場合）
# macOS
brew install mongodb-community

# Ubuntu/Debian
sudo apt-get install mongodb

# MongoDBサービスを起動
brew services start mongodb-community  # macOS
sudo systemctl start mongodb           # Linux
```

**本番環境の場合:**
- MongoDB Atlas で無料クラスターを作成
- 接続文字列を取得し、環境変数に設定

### 3. 環境変数の設定

`.env` ファイルを作成:
```bash
MONGODB_URI=mongodb://localhost:27017/myblog
SPRING_PROFILES_ACTIVE=dev
```

本番環境用:
```bash
MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/myblog
SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS=-Xmx512m -Xms256m
```

### 4. ビルドと実行

```bash
# gradlewに実行権限を付与
chmod +x gradlew

# 依存関係のインストールとビルド
./gradlew clean build

# アプリケーションの起動
./gradlew bootRun
```

アプリケーションは `http://localhost:8081` でアクセスできます。

## 💻 開発コマンド

### ビルド関連
```bash
# クリーンビルド
./gradlew clean build

# テストをスキップしてビルド（CI用）
./gradlew build -x test --no-daemon

# アプリケーション実行
./gradlew bootRun

# 特定のテストクラスを実行
./gradlew test --tests "com.example.MyBlog.Service.MyBlogServiceTest"
```

### Docker関連
```bash
# すべてのサービスをビルド・起動
docker compose up -d

# ログを確認
docker compose logs -f app

# すべてのサービスを停止
docker compose down

# デプロイスクリプトの実行
./deploy.sh deploy
```

## 🏗 アーキテクチャ

### パッケージ構成

```
src/main/java/com/example/MyBlog/
├── Config/              # 設定クラス
│   ├── SecurityConfig.java      # Spring Security設定
│   ├── MongoConfig.java         # MongoDB設定
│   └── MarkdownConfig.java      # Markdown設定
├── Controller/          # MVCコントローラー
│   ├── IndexController.java
│   ├── MyBlogController.java
│   ├── LoginController.java
│   └── RegisterController.java
├── Entity/              # ドメインモデル
│   ├── Article.java     # 記事エンティティ
│   └── Users.java       # ユーザーエンティティ
├── Repository/          # データアクセス層
│   ├── MyBlogRepository.java
│   └── UserRepository.java
├── Service/             # ビジネスロジック層
│   ├── MyBlogService.java
│   ├── MyBlogServiceImpl.java
│   └── UserDetailsServiceimpl.java
├── Exception/           # 例外ハンドラー
│   └── GlobalExceptionHandler.java
└── MyBlogApplication.java  # メインアプリケーション
```

### アーキテクチャパターン

- **MVC アーキテクチャ**: Controller → Service → Repository の明確な層分離
- **Entity設計**: Java Recordを使用した不変エンティティ
- **依存性注入**: コンストラクタインジェクション（`@RequiredArgsConstructor`）
- **テンプレートエンジン**: Thymeleafでサーバーサイドレンダリング

### データベーススキーマ

**Articles コレクション:**
```json
{
  "id": "string",
  "title": "string",
  "content": "string (Markdown)",
  "published": "boolean",
  "createdAt": "Date"
}
```

**Users コレクション:**
```json
{
  "id": "string",
  "username": "string",
  "password": "string (BCrypt)",
  "roles": ["string"]
}
```

## 🚢 デプロイメント

### CI/CDパイプライン

GitHub Actionsによる自動デプロイメント:

1. **テストフェーズ**
   - JUnit単体テスト実行
   - テストレポート生成

2. **セキュリティスキャン**
   - TrivyによるDockerイメージの脆弱性スキャン
   - SARIF形式でレポート保存

3. **デプロイフェーズ**
   - セルフホストランナーで本番サーバーにデプロイ
   - rsyncでファイル同期
   - Docker Composeで起動

## 🧪 テスト

### テスト実行

```bash
# すべてのテスト実行（MongoDB必須）
./gradlew test

# CI環境用（MongoDB不要のテストのみ）
CI=true ./gradlew test
```

### テスト戦略

- **単体テスト**: Serviceレイヤーのロジックテスト（Mockito使用）
- **CI対応**: MongoDB依存のテストはCI環境で除外
- **テスト構造**: mainパッケージと同じ構造を`src/test/java`に配置

### カバレッジ

テストレポートは `build/reports/tests/test/index.html` で確認できます。

## 🔒 セキュリティ

### セキュリティ対策

- **パスワードハッシュ化**: BCryptによる安全な保存
- **XSS対策**: OWASP HTML Sanitizerによる入力サニタイゼーション
- **認証・認可**: Spring Securityによる保護（`/Hello/**`エンドポイント）
- **コンテナセキュリティ**: 非rootユーザー、Alpine Linuxベースイメージ
- **脆弱性スキャン**: Trivyによる定期的なスキャン

### Markdown処理フロー

```
ユーザー入力（Markdown）
    ↓
Flexmarkパーサー（GFM拡張）
    ↓
OWASP HTML Sanitizer（サニタイゼーション）
    ↓
安全なHTMLレンダリング
```

### 環境変数管理

- 機密情報は`.env`ファイルで管理（Gitで除外）
- 本番環境ではDocker ComposeとGitHub Secretsで管理
- ハードコードされた認証情報なし

## 📝 今後の改善予定

- [ ] キャッシュ機能の実装（`@Cacheable`）
- [ ] 記事検索機能
- [ ] タグ機能
- [ ] ページネーション
- [ ] コメント機能
- [ ] データベースインデックス最適化（`{published: 1, createdAt: -1}`）
- [ ] 例外ハンドリングの強化（`@ExceptionHandler`）
- [ ] APIエンドポイントの追加（REST API）

## 📄 ライセンス

このプロジェクトはMITライセンスの下で公開されています。

## 🤝 コントリビューション

プルリクエストを歓迎します。大きな変更の場合は、まずissueを開いて変更内容を議論してください。

## 📧 お問い合わせ

質問や提案がある場合は、GitHubのissueを作成してください。
