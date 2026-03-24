# MyBlog リプレース・改善計画

作成日: 2026-03-15

## 概要

以下の改善を段階的に実施する。

1. **テストコード拡充**（最優先・独立して進めやすい）
2. **CI/CDリプレース**（GHCRベースの透明なデプロイへ）
3. **Podman化 + Nginx廃止 + compose廃止**（rootless・シンプル構成へ）
4. **Cloudflare Access一本化**（Spring Securityフォームログイン廃止）

---

## ① テストコード拡充（最優先）

### 現状のカバレッジ

| 対象 | 状態 | 備考 |
|------|------|------|
| `MyBlogServiceTest` | ✅ 実装済み | Mockito使用 |
| `MyBlogRepositoryTest` | ✅ 実装済み | `@DataMongoTest` 使用 |
| Controller層 | ❌ 未実装 | 全Controller対象 |
| `SecurityConfig` | ❌ 未実装 | 認証・認可のテスト |
| エラーケース・バリデーション | ⚠ 不十分 | エッジケース不足 |

### 追加するテスト（優先順）

#### A. MyBlogControllerTest（優先度：最高）

`@WebMvcTest` + `@WithMockUser` を使用。MongoDBが不要なためCIで即動く。

```java
// 追加するテストケース
- GET /Hello → 認証あり→200 OK、認証なし→302リダイレクト
- GET /Hello/Edit → 認証あり→200 OK
- POST /Hello/Submit → 正常登録→/Helloへリダイレクト
- PUT /Hello/Submit/{id} → 更新→/Helloへリダイレクト
- DELETE /Hello/{id} → 削除→/Helloへリダイレクト
- GET /Hello/Description/{id} → 200 OK、Markdown変換される
- GET /Hello/Draft → 200 OK、下書き一覧が渡る
```

#### B. MyBlogServiceImplTest（優先度：高・エッジケース補完）

```java
// 現状のテストに追加
- findArticleById（存在しないid）→ IllegalArgumentException がスローされる
- deleteArticle → myBlogRepository.deleteById() が呼ばれることを検証 ← 現状ない
```

#### C. IndexControllerTest（優先度：高）

```java
// 公開エンドポイントのテスト
- GET / → 200 OK、記事一覧がモデルに渡る
- GET /ViewDescription/{id} → 200 OK、Markdown変換・サニタイズされる
- GET /ViewDescription/存在しないid → GlobalExceptionHandlerでエラー画面
```

#### D. GlobalExceptionHandlerTest（優先度：中）

```java
- IllegalArgumentException → 404ステータス、error.html が返る
- MethodArgumentNotValidException → 400ステータス、error.html が返る
```

### テストの記述スタイル

Given-When-Then パターンを統一して使用する。

```java
@Test
void 認証済みユーザーが記事一覧ページにアクセスできる() {
    // Given
    List<Article> articles = List.of(new Article("id1", "Title", "Content", true, new Date()));
    when(myBlogService.findArticlePublishedTrue()).thenReturn(articles);

    // When
    MvcResult result = mockMvc.perform(get("/Hello").with(user("admin")))

    // Then
    .andExpect(status().isOk())
    .andExpect(view().name("Hello"))
    .andExpect(model().attributeExists("articles"))
    .andReturn();
}
```

### 進め方

```bash
git checkout -b feature/add-controller-tests
# テスト追加 → PR → CI通過 → master マージ
```

---

## ② CI/CDリプレース

### 現状の問題点

```
現状フロー：
  GitHub Actions（self-hosted runner）
    → rsync でソースコードをサーバーに転送
    → サーバー上で docker compose build --no-cache

問題：
  - self-hosted runner の管理コストがかかる
  - サーバー上でJava/Gradleビルドが走るためサーバー負荷が高い
  - ビルドプロセスがブラックボックス化しやすい
  - install.sh等がないため再セットアップ手順が不明確
```

### 新しいアーキテクチャ

```
┌─ GitHub Actions（ubuntu-latest） ─────────────────┐
│  1. テスト実行                                     │
│  2. Trivyセキュリティスキャン                      │
│  3. ./gradlew bootJar                              │
│  4. docker build                                   │
│  5. ghcr.io/[username]/myblog:latest へ push       │
└───────────────────────────────────────────────────┘
                       ↓ サーバー側でpull
┌─ 自宅サーバー ─────────────────────────────────────┐
│  install.sh  … 初回セットアップ（一度だけ実行）    │
│  update.sh   … デプロイ（新バージョンに更新）      │
│    → podman pull                                  │
│    → podman stop/rm → podman run                  │
└───────────────────────────────────────────────────┘
```

### 変更ファイル一覧

| ファイル | 変更内容 |
|----------|----------|
| `.github/workflows/deploy.yml` | self-hosted runner 廃止、GHCR push 追加 |
| `compose.yml` | 廃止（podman run に移行） |
| `nginx/` | 廃止（Cloudflare Tunnelが代替） |
| `deploy.sh` | 廃止 |
| `install.sh` | 新規作成（初回セットアップスクリプト） |
| `update.sh` | 新規作成（pull & run のみ） |

### update.sh のイメージ

```bash
#!/bin/bash
set -e

IMAGE="ghcr.io/[username]/myblog:latest"

podman pull "$IMAGE"
podman stop myblog 2>/dev/null || true
podman rm   myblog 2>/dev/null || true

podman run -d \
  --name myblog \
  --restart=always \
  -p 8080:8080 \
  --env-file ~/.env \
  "$IMAGE"

echo "✅ Deployment complete"
```

### install.sh のスコープ

```bash
#!/bin/bash
# 初回セットアップ。以後は update.sh を使う。

# 1. podman のインストール
# 2. ~/.env の配置確認（手動で用意）
# 3. ghcr.io からイメージをpull
# 4. podman run で初回起動
# 5. Cloudflare Tunnel の設定確認（localhost:8080 向き）
```

### 進め方

```bash
git checkout -b feature/ghcr-deploy
# deploy.yml修正（GHCR push） → install.sh/update.sh作成 → PR
```

---

## ③ Podman化 + Nginx廃止 + compose廃止

### アーキテクチャの変化

```
変更前：
  Cloudflare Tunnel → Nginx(80) → App(8080)
  管理: docker compose（app + nginx の2コンテナ）

変更後：
  Cloudflare Tunnel → App(8080)
  管理: podman run 一本（単一コンテナ）
```

### Nginx廃止が可能な理由

| Nginxが担っていた機能 | 代替 |
|--------------------|------|
| リバースプロキシ（80→8080） | Cloudflare Tunnelが直接8080に向く |
| セキュリティヘッダー | Cloudflare WAF または Spring Securityに追加 |
| 静的ファイルキャッシュ | Cloudflare CDN |

### Podmanの特徴

| 項目 | Docker | Podman |
|------|--------|--------|
| rootless実行 | △（要設定） | ✅ デフォルト |
| デーモン | 必要（dockerd） | 不要（デーモンレス） |
| Docker CLI互換 | — | ✅ ほぼ互換 |
| systemd連携 | 設定が必要 | ✅ ネイティブ対応 |
| sudo不要 | ❌ | ✅ |

### 注意点

- rootlessでも `8080` 番ポートは問題なくバインド可能（1024以上のため）
- `podman generate systemd` でsystemdサービス化すると自動起動も管理できる

### 進め方

```bash
git checkout -b feature/podman-migration
# 1. サーバーに podman インストール
# 2. podman run で動作確認
# 3. systemdサービス化（自動起動設定）
# 4. Cloudflare Tunnel を localhost:8080 に向け直す
# 5. Nginx停止・削除
# 6. compose.yml / nginx/ を削除するPR
```

---

## ④ Cloudflare Access一本化（Spring Securityフォームログイン廃止）

### 現状

```
Spring Security（フォームログイン）
  → /Hello/** を保護
  → ユーザー/パスワードをMongoDBで管理
```

### 変更後

```
Cloudflare Access（Google認証）
  → アプリへのアクセス自体をCloudflare側で遮断
  → Spring Securityはパブリックエンドポイントの制御のみ残す
  → ユーザー管理不要（MongoDBのUsersコレクションも不要になる）
```

### 削除対象ファイル

| ファイル | 理由 |
|----------|------|
| `LoginController.java` | フォームログイン不要 |
| `login.html` | フォームログイン不要 |
| `RegisterController.java` | ユーザー登録不要 |
| `register.html` | ユーザー登録不要 |
| `UserDetailsServiceimpl.java` | DB認証不要 |
| `UserRepository.java` | Usersコレクション不要 |
| `Users.java` | エンティティ不要 |

### SecurityConfig の変更イメージ

```java
// 変更後：フォームログイン廃止、全リクエストを許可
// （アクセス制御はCloudflare Access側で完結）
http
    .authorizeHttpRequests(auth -> auth
        .anyRequest().permitAll()
    )
    .csrf(AbstractHttpConfigurer::disable); // Cloudflare Tunnel経由のためCSRF不要
```

### Cloudflare Access側の設定

```
- Application: p49e.com/Hello/*  → Google認証必須
- Application: p49e.com/register → Google認証必須（管理者のみ）
- それ以外（/ , /ViewDescription/**）→ 認証不要（公開）
```

### メリット

- パスワード管理・ユーザー管理が不要になる
- MongoDBのUsersコレクションが不要になり構成がシンプルに
- ログインUIを自前で持たなくてよい
- Spring Security依存を大幅に削減できる

### 注意点

- Cloudflare Tunnelが落ちている間は管理画面にアクセス不可
  → ただし既にTunnel依存のため新たなリスクは増えない
- `Hello.html` のログアウトボタン（`/logout`）は削除が必要

### 進め方

```bash
git checkout -b feature/cloudflare-access-auth
# 1. SecurityConfig を簡素化（formLogin廃止）
# 2. 削除対象ファイルを順次削除
# 3. Hello.html のログアウトボタン削除
# 4. Cloudflare Access でアプリケーションを設定
# 5. 動作確認 → PR
```

---

## 実施スケジュール

```
Week 1: テストコード拡充
  ブランチ: feature/add-controller-tests
  優先: MyBlogControllerTest → ServiceImplテスト補完 → IndexControllerTest

Week 2: CI/CDリプレース
  ブランチ: feature/ghcr-deploy
  作業: GHCR push設定 → install.sh / update.sh 作成（podman run ベース）

Week 3: Podman化 + Nginx廃止 + Cloudflare Access一本化
  ブランチ: feature/podman-migration, feature/cloudflare-access-auth
  作業: Podmanインストール → 動作確認 → systemd化 → Nginx/compose廃止
        → SecurityConfig簡素化 → 不要ファイル削除
```

---

## 関連リンク

- [GitHub Container Registry ドキュメント](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [Podman公式ドキュメント](https://podman.io/docs)
- [podman generate systemd](https://docs.podman.io/en/latest/markdown/podman-generate-systemd.1.html)
- [Cloudflare Access ドキュメント](https://developers.cloudflare.com/cloudflare-one/applications/)
