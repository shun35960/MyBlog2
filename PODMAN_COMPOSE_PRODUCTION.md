# Rootless Podman Production Runbook

この手順は、本番サーバー上で `rootless Podman + systemd --user + Quadlet` で運用するための最小手順です。

## 前提

- Linux サーバー上に `podman` が入っていること
- `systemd --user` が使えること
- MongoDB 接続文字列を環境変数で渡せること

## 1. アプリを配置

```bash
git clone <repo-url> myblog2
cd myblog2
```

## 2. Podman と rootless 前提条件を確認

```bash
podman --version
systemctl --user --version
grep "^$(whoami)" /etc/subuid
grep "^$(whoami)" /etc/subgid
```

`Linger` を有効にして、ログアウト後も user service が残るようにします。

```bash
sudo loginctl enable-linger "$(whoami)"
loginctl show-user "$(whoami)" --property=Linger
```

## 3. 本番用の環境変数を設定

最低限 `MONGODB_URI` は必須です。

```bash
export MONGODB_URI='mongodb+srv://...'
export SPRING_PROFILES_ACTIVE='prod'
export APP_PORT='8080'
export JAVA_OPTS='-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Tokyo -XX:+UseG1GC -XX:MaxRAMPercentage=75'
```

## 4. 永続化用ディレクトリを作成

```bash
mkdir -p logs uploads
```

## 5. rootless user service をインストール

```bash
./install.sh
```

`install.sh` は以下を行います。

- `ghcr.io/shun35960/myblog2:latest` を `podman pull`
- `~/.config/myblog/myblog-app.env` を生成
- `~/.config/containers/systemd/myblog-app.container` を生成
- `systemctl --user start myblog-app.service`

`[Install]` セクションは Quadlet generator が処理するため、`myblog-app.service` への `systemctl enable` は不要です。

## 6. 状態確認

```bash
systemctl --user status myblog-app.service
podman ps --filter name=myblog-app
podman logs myblog-app
```

## 7. 停止・再起動

```bash
systemctl --user stop myblog-app.service
systemctl --user start myblog-app.service
systemctl --user restart myblog-app.service
```

## 8. 更新デプロイ

```bash
git pull
./update.sh
```

## 注意点

- `MONGODB_URI` を渡さないとアプリは MongoDB 接続に失敗します。
- `logs` と `uploads` は bind mount されます。
- SELinux 環境を考慮して volume に `:Z,U` を付けています。
- `Linger=no` のままだと、ユーザーセッション再起動時にコンテナが止まることがあります。
