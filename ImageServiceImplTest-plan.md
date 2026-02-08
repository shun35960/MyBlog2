# ImageServiceImplTest テストケース実装計画

## Context

`ImageServiceImpl`のテストファイル（`ImageServiceImplTest.java`）は現在スケルトン状態で、3つの空テストメソッドのみ存在する。GridFSを使用した画像管理サービスの品質を保証するため、包括的なユニットテストを実装する。

## 変更対象ファイル

1. **`src/test/java/com/example/MyBlog/Service/ImageServiceImplTest.java`** — テスト実装（メイン）
2. **`src/main/java/com/example/MyBlog/Service/ImageServiceImpl.java`** — `getImage`の例外ハンドリングバグ修正
3. **`build.gradle`** — CI環境のテスト対象に`ImageServiceImplTest`を追加（67行目）

## テスト構造

既存の`MyBlogServiceTest`のパターンに準拠:
- `@ExtendWith(MockitoExtension.class)`
- `@Mock`: `GridFSBucket`, `GridFsTemplate`
- `@InjectMocks`: `ImageServiceImpl`
- AAA（Arrange-Act-Assert）パターン

## テストケース一覧（全27ケース）

### storeImage — 正常系（4ケース）

| テストケース | 検証内容 |
|---|---|
| 有効なJPEG画像を保存できる | `gridFsTemplate.store()`が呼ばれ、ObjectIdの16進文字列が返る |
| 全対応形式(PNG/GIF/WebP)で保存できる | 各Content-Typeでバリデーション通過を確認 |
| メタデータが正しく設定される | `ArgumentCaptor`でuploadedBy, originalFilename, contentType, fileSize, uploadDateを検証 |
| 最大サイズ(100MB)ちょうどで保存できる | 境界値テスト（100*1024*1024バイト） |

### storeImage — バリデーションエラー（9ケース）

| テストケース | 入力 | 期待される例外メッセージ |
|---|---|---|
| nullファイル | `null` | `"ファイルが選択されていません"` |
| 空ファイル | `isEmpty()=true` | `"ファイルが選択されていません"` |
| サイズ超過 | 100MB+1バイト | `"ファイルサイズが大きすぎます"` |
| null ContentType | `getContentType()=null` | `"サポートされていないファイル形式です"` |
| 非対応ContentType (image/bmp) | `"image/bmp"` | 同上 |
| 非画像ContentType (text/plain) | `"text/plain"` | 同上 |
| nullファイル名 | `getOriginalFilename()=null` | `"ファイル名が無効です"` |
| 空文字ファイル名 | `""` | 同上 |
| 空白のみファイル名 | `"   "` | 同上 |

全ケースで `verifyNoInteractions(gridFsTemplate)` を検証。

### storeImage — ファイル名サニタイズ（4ケース）

`ArgumentCaptor`で`store()`に渡されるファイル名を検証:

| テストケース | 入力ファイル名 | 期待されるサニタイズ結果 |
|---|---|---|
| パス区切り文字の除去 | `"../../etc/passwd.jpg"` | `"....etcpasswd.jpg"` |
| バックスラッシュの除去 | `"folder\\image.jpg"` | `"folderimage.jpg"` |
| 制御文字の除去 | `"test\u0000image.jpg"` | `"testimage.jpg"` |
| 日本語ファイル名の保持 | `"画像テスト.png"` | `"画像テスト.png"` |

### storeImage — 例外ハンドリング（1ケース）

| テストケース | 検証内容 |
|---|---|
| IOException → RuntimeExceptionにラップ | `getInputStream()`がIOExceptionをスロー → `RuntimeException("Failed to store image")`、causeがIOException |

### getImage — 正常系（1ケース）

| テストケース | 検証内容 |
|---|---|
| 有効なIDでリソースを取得 | `findOne()` → `getResource()` の順に呼ばれ、`GridFsResource`が返る |

### getImage — エラー系（4ケース）

| テストケース | 入力 | 期待される動作 |
|---|---|---|
| 不正なfileId形式 | `"invalid-id"` | `IllegalArgumentException("Invalid file ID format")` |
| null fileId | `null` | `IllegalArgumentException` |
| 空文字fileId | `""` | `IllegalArgumentException` |
| 存在しないID | 有効なObjectId形式、`findOne()`がnull返却 | `IllegalArgumentException("Image not found with id: ...")` ※バグ修正後の正しい動作 |

### deleteImage — 正常系（1ケース）

| テストケース | 検証内容 |
|---|---|
| 有効なIDで削除 | `gridFsTemplate.delete(query)`が呼ばれる |

### deleteImage — エラー系（3ケース）

| テストケース | 入力 | 期待される動作 |
|---|---|---|
| 不正なfileId形式 | `"invalid-id"` | `IllegalArgumentException("Invalid file ID format")` |
| null fileId | `null` | `IllegalArgumentException` |
| 空文字fileId | `""` | `IllegalArgumentException` |

## getImageのバグ修正

**問題（100-124行目）:**
`gridFSFile == null`の場合に111行目で`IllegalArgumentException("Image not found with id: ...")`をスローするが、120行目の`catch (IllegalArgumentException e)`がこの例外もキャッチし、`"Invalid file ID format"`に変換してしまう。

**修正方針:** `new ObjectId(fileId)`だけを`try-catch`で囲み、「画像未検出」の例外がキャッチされないようにする。

```java
@Override
public GridFsResource getImage(String fileId) {
    ObjectId objectId;
    try {
        objectId = new ObjectId(fileId);
    } catch (IllegalArgumentException e) {
        log.error("Invalid fileId format: {}", fileId);
        throw new IllegalArgumentException("Invalid file ID format", e);
    }

    Query query = new Query(Criteria.where("_id").is(objectId));
    GridFSFile gridFSFile = gridFsTemplate.findOne(query);

    if (gridFSFile == null) {
        log.warn("Image not found: fileId={}", fileId);
        throw new IllegalArgumentException("Image not found with id: " + fileId);
    }

    GridFsResource resource = gridFsTemplate.getResource(gridFSFile);
    log.debug("Image retrieved successfully: fileId={}", fileId);
    return resource;
}
```

## build.gradle の変更

67行目の`include`に`ImageServiceImplTest`を追加:

```groovy
include '**/MyBlogServiceTest.class'
include '**/ImageServiceImplTest.class'
```

## 検証手順

```bash
# テスト実行
./gradlew test --tests "com.example.MyBlog.Service.ImageServiceImplTest"

# CI環境を模擬して実行
CI=true ./gradlew test

# 全テスト実行（MongoDB接続が必要）
./gradlew test
```
