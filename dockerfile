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
FROM eclipse-temurin:23-jre-noble
WORKDIR /app

# ベースイメージのパッケージを更新してから必要なフォントを追加
RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

# アプリケーションユーザーの作成
# ベースイメージ側で UID/GID 1000 が既に使われていても継続できるようにする
RUN existing_group="$(getent group 1000 | cut -d: -f1 || true)" \
    && if [ -n "$existing_group" ] && [ "$existing_group" != "spring" ]; then \
        groupmod -n spring "$existing_group"; \
    elif [ -z "$existing_group" ]; then \
        groupadd --gid 1000 spring; \
    fi \
    && existing_user="$(getent passwd 1000 | cut -d: -f1 || true)" \
    && if [ -n "$existing_user" ] && [ "$existing_user" != "spring" ]; then \
        usermod -l spring -d /home/spring -m -g spring -s /bin/bash "$existing_user"; \
    elif [ -z "$existing_user" ]; then \
        useradd --uid 1000 --gid spring --shell /bin/bash --create-home spring; \
    fi

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
