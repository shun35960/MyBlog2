package com.example.MyBlog.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.util.PlaceholderResolutionException;

/**
 * MongoUriCheckConfigは、本番プロファイルでMongoDBの接続先が
 * 正しく渡されているかを起動時に検証する設定クラスです。
 *
 * Spring Bootは接続先が未設定の場合、既定値のlocalhost:27017に
 * 黙ってフォールバックして正常起動してしまいます。
 * その結果コンテナはヘルシーと判定され、DBを参照する全ページが
 * 実行時に500を返す状態で公開されてしまうため、起動時に落とします。
 */
@Slf4j
@Configuration
@Profile("prod")
public class MongoUriCheckConfig {

    public MongoUriCheckConfig(Environment environment) {
        String uri;
        try {
            uri = environment.getProperty("spring.data.mongodb.uri");
        } catch (PlaceholderResolutionException e) {
            // ${SPRING_DATA_MONGODB_URI} を解決できない = 環境変数が渡っていない
            uri = null;
        }

        if (uri == null || uri.isBlank()) {
            throw new IllegalStateException("""
                    MongoDBの接続先(spring.data.mongodb.uri)が設定されていません。\
                    環境変数 SPRING_DATA_MONGODB_URI を設定して下さい。""");
        }

        if (uri.contains("localhost") || uri.contains("127.0.0.1")) {
            throw new IllegalStateException("""
                    本番プロファイルでMongoDBの接続先がlocalhostになっています。\
                    環境変数 SPRING_DATA_MONGODB_URI が渡っていない可能性があります。\
                    (Spring Bootは未設定時にlocalhost:27017へフォールバックします)""");
        }

        log.info("MongoDB接続先の設定を確認しました: host={}", maskUri(uri));
    }

    /**
     * 接続先URIから認証情報を除いたホスト部分のみを返します（ログ出力用）。
     *
     * @param uri MongoDBの接続先URI
     * @return 認証情報を除いた文字列
     */
    private String maskUri(String uri) {
        int at = uri.lastIndexOf('@');
        return at < 0 ? uri : "***@" + uri.substring(at + 1);
    }
}
