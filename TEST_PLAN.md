# テストクラス概要

## 構成一覧

| クラス | レイヤー | テスト種別 | テスト数 |
|---|---|---|---|
| `IndexControllerTest` | Controller | `@WebMvcTest` | 6 |
| `MyBlogControllerTest` | Controller | `@WebMvcTest` | 8 |
| `alterMyBlogControllerTest` | Controller | `@WebMvcTest` | 7 |
| `ImageControllerTest` | Controller | `@WebMvcTest` | 9 |
| `MyBlogServiceTest` | Service | `@ExtendWith(MockitoExtension)` | 8 |
| `ImageServiceImplTest` | Service | スケルトン（未実装） | 3 |
| `MyBlogRepositoryTest` | Repository | `@DataMongoTest` | 3 |

---

## Controller層

### IndexControllerTest
**対象**: `IndexController` (`GET /`, `/about`, `/ViewDescription/{id}`)
**設定**: `@WebMvcTest` + `@Import({MarkdownConfig, SecurityConfig})` + `@TestPropertySource`

| テストメソッド | エンドポイント | 検証内容 |
|---|---|---|
| `index_公開記事一覧が表示される` | `GET /` | 200、view=index、記事2件がモデルに渡る |
| `index_公開記事がない場合_空リストでindexが表示される` | `GET /` | 200、articles=空リスト |
| `about_記事が取得されてViewDescriptionが表示される` | `GET /about` | 200、view=ViewDescription、article・renderedHtmlContent がモデルに渡る |
| `about_記事のcontentがnull_空文字でレンダリングされる` | `GET /about` | 200、null content でもエラーにならない |
| `viewDescription_指定IDの記事が表示される` | `GET /ViewDescription/{id}` | 200、view=ViewDescription、article がモデルに渡る |
| `viewDescription_記事のcontentがnull_空文字でレンダリングされる` | `GET /ViewDescription/{id}` | 200、null content でもエラーにならない |

---

### MyBlogControllerTest
**対象**: `MyBlogController` (`/Hello/**`)
**設定**: `@WebMvcTest` + `@Import({MarkdownConfig, SecurityConfig})`

| テストメソッド | エンドポイント | 検証内容 |
|---|---|---|
| `hello` | `GET /Hello` | 200、view=Hello |
| `edit` | `GET /Hello/Edit` | 200、view=Edit |
| `editArticle` | `GET /Hello/Edit/{id}` | 200、view=Edit、article がモデルに渡る |
| `saveArticle` | `POST /Hello/Submit` | 3xxリダイレクト |
| `updateArticle` | `PUT /Hello/Submit/{id}` | 3xxリダイレクト |
| `showdescription` | `GET /Hello/Description/{id}` | 200、view=Description |
| `showDraft` | `GET /Hello/Draft` | 200、view=Draft |
| `deleteArticle` | `DELETE /Hello/{id}` | 3xxリダイレクト |

---

### alterMyBlogControllerTest
**対象**: `MyBlogController` (`/Hello/**`) — 認証・未認証シナリオ中心
**設定**: `@WebMvcTest` + `@Import({SecurityAutoConfiguration, MarkdownConfig})`

| テストメソッド | エンドポイント | 検証内容 |
|---|---|---|
| `認証済みユーザーが記事一覧ページにアクセスできる` | `GET /Hello` | 200、model=articles・Hellotitle |
| `未認証ユーザーがアクセスするとリダイレクトされる` | `GET /Hello` | 3xxリダイレクト、serviceが呼ばれない |
| `記事が0件のとき空リストがモデルに渡る` | `GET /Hello` | 200、articles=空リスト |
| `認証済みユーザーが新規作成ページにアクセスできる` | `GET /Hello/Edit` | 200、view=Edit、article が存在 |
| `認証済みユーザーが記事編集ページにアクセスできる` | `GET /Hello/Edit/{id}` | 200、view=Edit、article が渡る |
| `存在しないIDで編集ページにアクセスするとエラー画面が返る` | `GET /Hello/Edit/{id}` | 404、view=error |
| `認証済みユーザーが記事を登録するとトップにリダイレクトされる` | `POST /Hello/Submit` | 3xxリダイレクト、Location=/Hello |

---

### ImageControllerTest
**対象**: `ImageController` (`/api/images/**`)
**設定**: `@WebMvcTest` + `@Import({SecurityConfig})` ※CSRF無効のため `.with(csrf())` 不要

| テストメソッド | エンドポイント | 検証内容 |
|---|---|---|
| `uploadImage_成功_201とfileIdが返される` | `POST /api/images/upload` | 201、fileId・url・message を返す |
| `uploadImage_バリデーションエラー_400とエラーメッセージが返される` | `POST /api/images/upload` | 400、IllegalArgumentException |
| `uploadImage_サービス例外_500とuploadFailedが返される` | `POST /api/images/upload` | 500、error=uploadFailed |
| `getImage_成功_200と画像データが返される` | `GET /api/images/{fileId}` | 200、Content-Type=image/jpeg |
| `getImage_contentTypeがnull_octetStreamで返される` | `GET /api/images/{fileId}` | 200、Content-Type=application/octet-stream |
| `getImage_存在しないfileId_404が返される` | `GET /api/images/{fileId}` | 404、IllegalArgumentException |
| `getImage_IOエラー_500が返される` | `GET /api/images/{fileId}` | 500、IOException |
| `deleteImage_成功_200とdeleteSuccessが返される` | `DELETE /api/images/{fileId}` | 200、message=deleteSuccess |
| `deleteImage_存在しないfileId_404が返される` | `DELETE /api/images/{fileId}` | 404 |
| `deleteImage_サービス例外_500とdeleteFailedが返される` | `DELETE /api/images/{fileId}` | 500、error=deleteFailed |

---

## Service層

### MyBlogServiceTest
**対象**: `MyBlogServiceImpl`
**設定**: `@ExtendWith(MockitoExtension)` + `@Mock MyBlogRepository` + `@InjectMocks MyBlogServiceImpl`

| テストメソッド | 検証内容 |
|---|---|
| `test_findArticleById` | 存在するIDで記事が返る |
| `findArticleById_存在しないID_例外がスローされる` | `IllegalArgumentException` がスローされる |
| `test_findArticlePublishedTrue` | 公開記事のみ返る |
| `findArticlePublishedTrue_記事が存在しない_空リストが返される` | 空リストが返る |
| `test_findArticlePublishedFalse` | 非公開記事のみ返る |
| `findArticlePublishedFalse_記事が存在しない_空リストが返される` | 空リストが返る |
| `test_submitArticle` | save() が呼ばれ記事が返る |
| `deleteArticle_指定IDの記事が削除される` | `deleteById()` が呼ばれる |

---

### ImageServiceImplTest
**対象**: `ImageServiceImpl`
**状態**: スケルトン（テスト未実装）

---

## Repository層

### MyBlogRepositoryTest
**対象**: `MyBlogRepository`
**設定**: `@DataMongoTest` + `@ActiveProfiles("test")` ※MongoDB 必須のため CI 環境では除外

| テストメソッド | 検証内容 |
|---|---|
| `getAllBlogs` | 全件取得（2件） |
| `findByPublishedTrue` | published=true の記事のみ取得 |
| `findByPublishedFalse` | published=false の記事のみ取得 |

---

## CI での除外設定

`@DataMongoTest` を使用する `MyBlogRepositoryTest` は MongoDB が必要なため、CI 環境 (`CI=true`) では除外される。
その他のテストは MockMvc + Mockito で完結するため CI でも実行可能。

```bash
# 全テスト実行（MongoDB必要）
./gradlew test

# CI環境（MongoDBなし）
CI=true ./gradlew test
```