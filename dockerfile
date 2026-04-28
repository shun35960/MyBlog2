# ビルドステージ
FROM eclipse-temurin:23-jdk AS builder
WORKDIR /app

# Gradle Wrapperと設定ファイルをコピー
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# 実行権限を付与
RUN chmod +x gradlew

# 依存関係をダウンロード
RUN ./gradlew dependencies --no-daemon

# ソースコードをコピー
COPY src ./src

# アプリケーションをビルド
RUN ./gradlew clean bootJar --no-daemon

# 実行ステージ
FROM eclipse-temurin:23-jre-alpine
WORKDIR /app

# ベースイメージに含まれる Alpine パッケージも更新してから追加パッケージを入れる
RUN apk upgrade --no-cache \
    && apk add --no-cache fontconfig ttf-dejavu

# アプリケーションユーザーの作成
RUN addgroup -g 1000 spring && adduser -u 1000 -G spring -s /bin/sh -D spring

# 必要なディレクトリを作成
RUN mkdir -p /app/logs /app/uploads && chown -R spring:spring /app

# JARファイルをコピー
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

# 所有権を確認
RUN chown spring:spring app.jar

USER spring

EXPOSE 8080

ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Tokyo -XX:+UseG1GC -XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
