package com.example.MyBlog.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.PlaceholderResolutionException;

import java.util.Arrays;

/**
 * MongoUriCheckConfigは、MongoDBの接続先が正しく渡されているかを
 * 起動時に検証・記録する設定クラスです。
 *
 * 接続先のプロパティはSpring Boot 4.0.0で spring.data.mongodb.uri から
 * spring.mongodb.uri へ改名された。旧名は deprecation level=error のため
 * 指定しても無視され、既定値のlocalhostにフォールバックする。
 *
 * Spring Bootは接続先が未設定の場合、既定値のlocalhost:27017に
 * 黙ってフォールバックして正常起動してしまいます。
 * その結果コンテナはヘルシーと判定され、DBを参照する全ページが
 * 実行時に500を返す状態で公開されてしまいます。
 *
 * 診断ログは全プロファイルで出力し、起動の中断はprodプロファイルの場合のみ行います。
 * prod限定にすると、SPRING_PROFILES_ACTIVEの渡し忘れ自体を検知できないためです。
 */
@Slf4j
@Configuration
public class MongoUriCheckConfig {

    private static final String PROD_PROFILE = "prod";

    public MongoUriCheckConfig(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        String uri = resolveUri(environment);

        // 接続先が届いていない場合の切り分け用。値は出さず、環境変数の有無だけを記録する
        log.info("MongoDB接続設定: activeProfiles={}, uri={}, env[SPRING_MONGODB_URI]={}, env[SPRING_DATA_MONGODB_URI]={}, env[MONGODB_URI]={}",
                Arrays.toString(activeProfiles),
                uri == null ? "未設定(既定のlocalhost:27017が使われます)" : maskUri(uri),
                System.getenv("SPRING_MONGODB_URI") != null ? "あり" : "なし",
                System.getenv("SPRING_DATA_MONGODB_URI") != null ? "あり" : "なし",
                System.getenv("MONGODB_URI") != null ? "あり" : "なし");

        if (!Arrays.asList(activeProfiles).contains(PROD_PROFILE)) {
            // 開発・テストではlocalhostが正しいので検証しない
            return;
        }

        if (uri == null || uri.isBlank()) {
            throw new IllegalStateException("""
                    MongoDBの接続先(spring.mongodb.uri)が設定されていません。\
                    環境変数 SPRING_MONGODB_URI を設定して下さい。""");
        }

        if (uri.contains("localhost") || uri.contains("127.0.0.1")) {
            throw new IllegalStateException("""
                    本番プロファイルでMongoDBの接続先がlocalhostになっています。\
                    環境変数 SPRING_MONGODB_URI が渡っていない可能性があります。\
                    (Spring Bootは未設定時にlocalhost:27017へフォールバックします)""");
        }
    }

    /**
     * 接続先URIを解決します。プレースホルダを解決できない場合は未設定として扱います。
     *
     * @param environment 環境設定
     * @return 接続先URI。未設定の場合はnull
     */
    private String resolveUri(Environment environment) {
        try {
            return environment.getProperty("spring.mongodb.uri");
        } catch (PlaceholderResolutionException e) {
            // プレースホルダを解決できない = 接続先の環境変数が渡っていない
            return null;
        }
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
