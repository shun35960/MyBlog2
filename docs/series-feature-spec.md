# シリーズ機能 仕様書

複数の記事をまとめて表示できる「シリーズ(連載)」機能の仕様。

## コンセプト

連載シリーズ型。記事は最大1つのシリーズに所属でき、シリーズページでは所属記事の全文を作成日時順に連結して1ページで読み通せる。

## データモデル

### 新規: Series エンティティ(`Serieses` コレクション)

```java
@Document(collection = "Serieses")
public record Series(
        @Id String id,
        @NotBlank @Size(max = 200) String title,  // シリーズ名
        String description,                        // 説明(任意・Markdown可)
        @CreatedDate Date createdAt
) { }
```

### 変更: Article に seriesId を追加

```java
public record Article(
        @Id String id,
        String title,
        String content,
        boolean published,
        String seriesId,   // 所属シリーズID(null = 未所属)
        @CreatedDate Date createdAt
) { }
```

- 記事側が親を知る設計。Series 側に記事リストを持たせないため、整合性の管理が楽
- 既存記事は `seriesId` フィールドが存在しないだけ(null として読まれる)なので **MongoDB のマイグレーションは不要**

## リポジトリ

- **`MyBlogRepository`(既存に追加)**:
  `List<Article> findBySeriesIdAndPublishedTrueOrderByCreatedAtAsc(String seriesId);`
- **`SeriesRepository`(新規・宣言のみ)**:
  `interface SeriesRepository extends MongoRepository<Series, String> { }`
  ※ Series ドキュメント自体の CRUD 用。Spring Data は 1 エンティティ = 1 リポジトリのため必要

## 画面とルーティング

| URL | 内容 |
|---|---|
| `GET /Hello/Series` | シリーズ一覧(タイトル・記事数・最終更新) |
| `GET /Hello/Series/{id}` | シリーズ詳細:公開記事を createdAt 昇順で全文連結表示 |
| 既存 `Edit` 画面 | 「所属シリーズ」セレクトボックス+新規シリーズ名の入力欄を追加 |

### シリーズ詳細ページ

- 各記事の間に区切り(記事タイトル+日付の見出し)を入れる
- 各記事の個別ページ(`/Hello/Description/{id}`)へのリンクを置く
- Markdown レンダリングは既存フロー(Flexmark → OWASP サニタイズ)を各記事に適用

### 記事一覧(Hello.html)

- シリーズ所属記事にはシリーズ名のバッジを表示し、シリーズページへリンク

## 公開制御

- シリーズ自体に published は持たせない
- シリーズ詳細に表示するのは `published=true` の記事のみ
- 公開記事が 1 件もないシリーズは、シリーズ一覧に表示しない

## エッジケース

| ケース | 挙動 |
|---|---|
| シリーズ削除時 | 所属記事の `seriesId` を null にする(記事は消さない) |
| 記事削除時 | 特別な処理は不要(Series 側が記事リストを持たないため) |
| 付け替え/解除 | Edit 画面のセレクトボックスで「なし」を選べば解除 |

## 実装範囲

### 新規ファイル

- `Entity/Series.java`
- `Repository/SeriesRepository.java`
- `Service/SeriesService.java` / `Service/SeriesServiceImpl.java`
- `templates/SeriesList.html`(一覧)
- `templates/SeriesDetail.html`(詳細)

### 変更ファイル

- `Entity/Article.java`(seriesId 追加)
- `Repository/MyBlogRepository.java`(seriesId 検索メソッド追加)
- `Controller/MyBlogController.java`(または新規 `SeriesController`)
- `templates/Edit.html`(シリーズ選択 UI)
- `templates/Hello.html`(シリーズバッジ)

### テスト

- SeriesService のユニットテスト(既存の Mockito パターンに合わせる)