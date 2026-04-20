---
name: gen-test
description: 指定されたJavaクラス（Service、Controller、Repository）のJUnit5テストコードを生成する。対象クラスのパスまたはクラス名を引数として受け取る。
tools: Read, Glob, Grep, Write, Bash
---

# テストコード生成スキル

このスキルは、MyBlog2プロジェクトのJavaクラスに対してJUnit5テストコードを自動生成します。

## 入力

ユーザーが指定するもの:
- 対象クラスのパス（例: `src/main/java/com/example/MyBlog/Service/MyBlogServiceImpl.java`）
- またはクラス名（例: `MyBlogServiceImpl`）
- またはレイヤー指定（例: `Service`, `Controller`, `Repository`）

## ワークフロー

### Step 1: 対象クラスの特定

```bash
# クラス名からファイルを検索
find src/main -name "*.java" | xargs grep -l "<クラス名>"
```

対象ファイルを Read で読み込み、以下を把握する:
- クラスの種別（Service実装、Controller、Repository）
- 依存関係（@Mock対象）
- 公開メソッド一覧とシグネチャ
- 戻り値の型と例外

### Step 2: 既存テストの確認

```bash
# 対応するテストファイルが存在するか確認
find src/test -name "*<クラス名>*Test.java"
```

存在する場合は Read で読み込み、不足しているテストケースを特定する。

### Step 3: レイヤー別テンプレートを適用

#### Serviceテスト（Mockitoを使用）

```java
package com.example.MyBlog.Service;

import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Repository.MyBlogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class <ClassName>Test {

    @Mock
    private <DependencyRepository> repository;

    @InjectMocks
    private <ServiceImpl> service;

    @Test
    void <methodName>_正常系_期待値が返される() {
        // Arrange
        // モックの設定

        // Act
        var result = service.<method>();

        // Assert
        assertNotNull(result);
        verify(repository).<repositoryMethod>();
    }

    @Test
    void <methodName>_異常系_例外がスローされる() {
        // Arrange
        when(repository.<method>(any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(<ExceptionClass>.class,
                () -> service.<method>(<args>));
    }
}
```

#### Controllerテスト（MockMvcを使用）

```java
package com.example.MyBlog.Controller;

import com.example.MyBlog.Config.MarkdownConfig;
import com.example.MyBlog.Config.SecurityConfig;
import com.example.MyBlog.Entity.Article;
import com.example.MyBlog.Service.MyBlogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Date;

@WebMvcTest(<ControllerClass>.class)
@Import({MarkdownConfig.class, SecurityConfig.class})
class <ControllerClass>Test {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MyBlogService myBlogService;

    @Test
    void <endpoint>_GETリクエスト_200が返される() throws Exception {
        mockMvc.perform(get("<path>"))
                .andExpect(status().isOk())
                .andExpect(view().name("<viewName>"));
    }

    @Test
    void <endpoint>_認証済みPOST_リダイレクトされる() throws Exception {
        mockMvc.perform(post("<path>")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("<paramName>", "<paramValue>"))
                .andExpect(status().is3xxRedirection());
    }
}
```

### Step 4: テストケースの網羅

各メソッドに対して以下のケースを生成する:

| ケース種別 | 内容 |
|----------|------|
| 正常系 | 正しい入力で期待値が返る |
| 空リスト | 結果が0件のとき |
| 存在しないID | Optional.empty()が返るとき |
| 例外スロー | IllegalArgumentExceptionなど |
| モック検証 | `verify()`でリポジトリ呼び出しを確認 |

### Step 5: テストファイルの出力

テストファイルのパスを決定する:
- `src/main/java/com/example/MyBlog/Service/XxxServiceImpl.java`
  → `src/test/java/com/example/MyBlog/Service/XxxServiceImplTest.java`
- `src/main/java/com/example/MyBlog/Controller/XxxController.java`
  → `src/test/java/com/example/MyBlog/Controller/XxxControllerTest.java`

生成したコードを Write ツールでファイルに書き込む。

### Step 6: テスト実行確認

```bash
./gradlew test --tests "com.example.MyBlog.<Layer>.<ClassName>Test"
```

テストが通過するか確認し、失敗した場合は修正する。

## 品質チェックリスト

- [ ] `@ExtendWith(MockitoExtension.class)` または `@WebMvcTest` が付いている
- [ ] 依存クラスはすべて `@Mock` / `@MockitoBean` でモック化されている
- [ ] テストメソッド名が日本語で意図を表している（例: `findById_存在しないID_例外がスローされる`）
- [ ] Arrange/Act/Assert のコメントで構造が明確
- [ ] `verify()` でモックの呼び出しを検証している
- [ ] 正常系・異常系・境界値がカバーされている

## 注意事項

- CI環境ではMongoDBが不要なテストのみ実行される（`build.gradle`のCI条件を確認）
- Securityのテストには `spring-security-test` の `csrf()` と `user()` を使用
- Entityは `record` 型なので `new Article(id, title, content, published, date)` で生成
