# CI/CD パイプライン移行手順書

作成日: 2026-04-20

## 概要

### 現状

```
GitHub Actions (self-hosted runner on 192.168.10.106)
  → rsync でソースコードをサーバーに転送
  → サーバー上で docker compose build --no-cache（毎回フルビルド）
  → docker compose up（app + nginx の2コンテナ構成）
```

### 移行後

```
GitHub Actions (ubuntu-latest)
  → ./gradlew bootJar
  → docker build
  → ghcr.io/shun35960/myblog:latest へ push
       ↓ SSH経由でトリガー
サーバー (192.168.10.106)
  → podman pull ghcr.io/shun35960/myblog:latest
  → podman stop/rm → podman run（単一コンテナ）
  → Cloudflare Tunnel が localhost:8080 を直接向く（Nginx不要）
```

### 移行のメリット

| 項目 | 現状 | 移行後 |
|------|------|--------|
| ビルド場所 | サーバー上（負荷高） | GitHub Actions（無料枠） |
| デプロイ時間 | フルビルド（数分） | pull のみ（数十秒） |
| コンテナ数 | app + nginx（2台） | app のみ（1台） |
| rootless実行 | 要設定 | Podmanでデフォルト |
| ビルド再現性 | 環境依存 | CI環境で固定 |

---

## 前提条件の確認

### GitHub側

- [ ] リポジトリが `github.com/shun35960/MyBlog2` であること
- [ ] GitHub Container Registry (GHCR) はリポジトリの `packages: write` 権限で自動的に使用可能（追加費用なし）

### サーバー側（192.168.10.106）

- [ ] SSHでログインできること
- [ ] `~/.env` ファイルが存在すること（後述の形式で）
- [ ] Cloudflare Tunnel がインストール済みであること

---

## Step 1: `~/.env` ファイルの準備（サーバー作業）

サーバーにSSHログインし、以下を確認・作成する。

```bash
ssh shun@192.168.10.106
```

`~/.env` が存在しない場合は作成する：

```bash
cat > ~/.env << 'EOF'
SPRING_PROFILES_ACTIVE=prod
MONGODB_URI=mongodb+srv://<user>:<pass>@<cluster>.mongodb.net/myblog
JAVA_OPTS=-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Tokyo -XX:+UseG1GC -XX:MaxRAMPercentage=75
EOF
chmod 600 ~/.env
```

> **注意**: `MONGODB_URI` は実際の MongoDB Atlas 接続文字列に置き換えること。

---

## Step 2: Podman のインストール（サーバー作業）

```bash
# Ubuntu/Debianの場合
sudo apt-get update
sudo apt-get install -y podman

# バージョン確認
podman --version
```

rootless設定の確認：

```bash
# サブUIDとサブGIDが割り当てられていること
grep "^$(whoami)" /etc/subuid
grep "^$(whoami)" /etc/subgid

# 割り当てられていない場合
sudo usermod --add-subuids 100000-165535 --add-subgids 100000-165535 $(whoami)
```

---

## Step 3: `install.sh` の配置（サーバー作業）

初回セットアップ用スクリプトをサーバーに配置する。

```bash
cat > ~/install.sh << 'SCRIPT'
#!/bin/bash
set -e

IMAGE="ghcr.io/shun35960/myblog:latest"

echo "=== MyBlog 初回セットアップ ==="

# ~/.env の存在確認
if [ ! -f ~/.env ]; then
  echo "ERROR: ~/.env が見つかりません。先に作成してください。"
  exit 1
fi

# GHCR からイメージを pull（パブリックリポジトリの場合は認証不要）
# プライベートリポジトリの場合は以下のコメントアウトを解除してPATを設定
# echo "$GHCR_PAT" | podman login ghcr.io -u shun35960 --password-stdin

echo "イメージを pull しています..."
podman pull "$IMAGE"

# 既存コンテナがあれば停止・削除
podman stop myblog 2>/dev/null || true
podman rm   myblog 2>/dev/null || true

# コンテナを起動
podman run -d \
  --name myblog \
  --restart=always \
  -p 8080:8080 \
  --env-file ~/.env \
  -v ~/logs:/app/logs:Z \
  -v ~/uploads:/app/uploads:Z \
  "$IMAGE"

# ログディレクトリを作成（起動前に存在していなければ）
mkdir -p ~/logs ~/uploads

echo "=== セットアップ完了 ==="
echo "確認: podman ps"
podman ps
SCRIPT

chmod +x ~/install.sh
```

---

## Step 4: `update.sh` の配置（サーバー作業）

デプロイ（更新）用スクリプトをサーバーに配置する。CI/CDから呼び出されるスクリプト。

```bash
cat > ~/update.sh << 'SCRIPT'
#!/bin/bash
set -e

IMAGE="ghcr.io/shun35960/myblog:latest"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] デプロイ開始"

# 新しいイメージを pull
podman pull "$IMAGE"

# 既存コンテナを停止・削除
podman stop myblog 2>/dev/null || true
podman rm   myblog 2>/dev/null || true

# 新しいコンテナを起動
podman run -d \
  --name myblog \
  --restart=always \
  -p 8080:8080 \
  --env-file ~/.env \
  -v ~/logs:/app/logs:Z \
  -v ~/uploads:/app/uploads:Z \
  "$IMAGE"

# ヘルスチェック（最大30秒待機）
echo "ヘルスチェック中..."
for i in $(seq 1 6); do
  if curl -sf --max-time 5 http://localhost:8080 > /dev/null 2>&1; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] アプリケーション起動確認"
    break
  fi
  echo "待機中... ($i/6)"
  sleep 5
done

echo "[$(date '+%Y-%m-%d %H:%M:%S')] デプロイ完了"
podman ps --filter name=myblog
SCRIPT

chmod +x ~/update.sh
```

---

## Step 5: GitHub Secrets の設定（GitHub作業）

`github.com/shun35960/MyBlog2/settings/secrets/actions` を開き、以下を追加する。

| Secret名 | 値 | 説明 |
|----------|-----|------|
| `SERVER_HOST` | `192.168.10.106` | サーバーIPアドレス |
| `SERVER_USER` | `shun` | SSHログインユーザー名 |
| `SERVER_SSH_KEY` | （秘密鍵の内容） | SSH秘密鍵（後述） |

### SSH鍵ペアの作成（ローカル作業）

専用の鍵ペアを新規作成する（既存の `~/.ssh/id_rsa` とは別にする）：

```bash
# ローカルで実行
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/github_actions_deploy -N ""

# 公開鍵の内容を表示（サーバーに登録する）
cat ~/.ssh/github_actions_deploy.pub

# 秘密鍵の内容を表示（GitHub Secretsに登録する）
cat ~/.ssh/github_actions_deploy
```

### 公開鍵をサーバーに登録（サーバー作業）

```bash
# サーバー上で実行
echo "<上記で表示された公開鍵>" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

---

## Step 6: `deploy.yml` の改修（リポジトリ作業）

`.github/workflows/deploy.yml` を以下のように変更する。

### 変更点

1. `permissions` に `packages: write` を追加
2. `deploy` ジョブ（self-hosted）を `build-and-push` ジョブ（ubuntu-latest + GHCR）と `deploy` ジョブ（SSH経由）に置き換え

### 改修後の `deploy.yml`

```yaml
name: MyBlog CI/CD Pipeline

on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]

permissions:
  contents: read
  checks: write
  pull-requests: write
  actions: read
  statuses: write
  packages: write      # GHCR push に必要

env:
  APP_NAME: myblog
  REGISTRY: ghcr.io
  IMAGE_NAME: ghcr.io/shun35960/myblog

jobs:
  # ======== テスト・品質チェック ========
  test:
    name: Run Tests & Quality Checks
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '23'
          distribution: 'temurin'
      - uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: ${{ runner.os }}-gradle-
      - run: chmod +x gradlew
      - run: ./gradlew test --no-daemon --info
      - name: Generate test report
        uses: dorny/test-reporter@v1
        if: success() || failure()
        with:
          name: Gradle Tests
          path: build/test-results/test/*.xml
          reporter: java-junit
          token: ${{ secrets.GITHUB_TOKEN }}
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: |
            build/reports/tests/
            build/test-results/

  # ======== Dockerイメージビルド & GHCR push ========
  build-and-push:
    name: Build & Push to GHCR
    runs-on: ubuntu-latest
    needs: test
    if: github.ref == 'refs/heads/master'
    outputs:
      image-digest: ${{ steps.push.outputs.digest }}
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/metadata-action@v5
        id: meta
        with:
          images: ${{ env.IMAGE_NAME }}
          tags: |
            type=raw,value=latest
            type=sha,prefix=sha-
      - uses: docker/build-push-action@v5
        id: push
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}

  # ======== セキュリティスキャン ========
  security:
    name: Security Scan
    runs-on: ubuntu-latest
    needs: build-and-push
    if: github.ref == 'refs/heads/master'
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: '${{ env.IMAGE_NAME }}:latest'
          format: 'table'
      - name: Run Trivy (SARIF output)
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: '${{ env.IMAGE_NAME }}:latest'
          format: 'sarif'
          output: 'trivy-results.sarif'
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: trivy-security-scan-results
          path: trivy-results.sarif
          retention-days: 30

  # ======== 本番環境デプロイ ========
  deploy:
    name: Deploy to Production
    runs-on: ubuntu-latest
    needs: [ build-and-push, security ]
    if: github.ref == 'refs/heads/master'
    steps:
      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SERVER_SSH_KEY }}
          script: ~/update.sh

  # ======== 通知 ========
  notify:
    name: Notify Results
    runs-on: ubuntu-latest
    needs: [ deploy ]
    if: always()
    steps:
      - name: Create deployment summary
        run: |
          echo "## Deployment Summary" >> $GITHUB_STEP_SUMMARY
          echo "- **Repository**: ${{ github.repository }}" >> $GITHUB_STEP_SUMMARY
          echo "- **Commit**: ${{ github.sha }}" >> $GITHUB_STEP_SUMMARY
          echo "- **Image**: ghcr.io/shun35960/myblog:latest" >> $GITHUB_STEP_SUMMARY
          echo "- **Status**: ${{ needs.deploy.result }}" >> $GITHUB_STEP_SUMMARY
```

---

## Step 7: Cloudflare Tunnel の向き先変更（サーバー作業）

Nginx経由（ポート80）から直接アプリ（ポート8080）へ変更する。

Cloudflare Tunnel の設定ファイルを確認：

```bash
# 設定ファイルの場所を確認
cat ~/.cloudflared/config.yml
# または
cat /etc/cloudflared/config.yml
```

`ingress` セクションの `service` を変更する：

```yaml
# 変更前
ingress:
  - hostname: p49e.com
    service: http://localhost:80

# 変更後
ingress:
  - hostname: p49e.com
    service: http://localhost:8080
```

Tunnel を再起動して反映：

```bash
sudo systemctl restart cloudflared
# または
cloudflared tunnel run
```

---

## Step 8: 動作確認と旧構成の停止（サーバー作業）

### 旧 Docker 構成を停止

```bash
cd ~/IdeaProjects   # 現在の本番ディレクトリ
sudo docker compose down
```

### 新構成で初回起動

```bash
~/install.sh
```

### 動作確認

```bash
# コンテナが起動していること
podman ps

# アプリが応答すること
curl -I http://localhost:8080

# ログを確認
podman logs myblog --tail=50
```

### Cloudflare 経由でアクセス確認

ブラウザで `https://p49e.com` にアクセスし、ブログが表示されることを確認する。

---

## Step 9: 旧構成ファイルの削除（リポジトリ作業）

動作確認が完了したら、不要ファイルを削除してPRを作成する。

```bash
git checkout -b cleanup/remove-old-deploy

# 削除対象
git rm deploy.sh
git rm compose.yml.bak
git rm -r nginx/

git commit -m "Remove old Docker Compose and Nginx deployment files"
git push origin cleanup/remove-old-deploy
```

> **注意**: self-hosted runner はGitHubの設定画面から削除する。
> `Settings → Actions → Runners` でランナーを選択して Remove。

---

## ロールバック手順

新構成でトラブルが発生した場合：

### Podman コンテナを旧バージョンに戻す

```bash
# SHAタグ付きのイメージ一覧を確認
podman images ghcr.io/shun35960/myblog

# 旧バージョンのSHAを指定して起動
podman stop myblog && podman rm myblog
podman run -d \
  --name myblog \
  --restart=always \
  -p 8080:8080 \
  --env-file ~/.env \
  -v ~/logs:/app/logs:Z \
  ghcr.io/shun35960/myblog:sha-<旧コミットSHA>
```

### 旧 Docker Compose 構成に戻す（緊急時）

```bash
cd ~/IdeaProjects
sudo docker compose up -d
# Cloudflare Tunnel を localhost:80 に戻す
```

---

## チェックリスト（実施時に使用）

### サーバー準備

- [ ] SSH で `192.168.10.106` にログインできる
- [ ] `~/.env` を作成・`chmod 600` 済み
- [ ] `podman --version` で Podman が動作確認できる
- [ ] `/etc/subuid`, `/etc/subgid` に `shun` のエントリがある
- [ ] `~/install.sh`, `~/update.sh` を配置・`chmod +x` 済み
- [ ] GitHub Actions 用の SSH 公開鍵を `~/.ssh/authorized_keys` に追加済み

### GitHub 準備

- [ ] `SERVER_HOST` Secret を設定済み
- [ ] `SERVER_USER` Secret を設定済み
- [ ] `SERVER_SSH_KEY` Secret を設定済み（秘密鍵の内容）
- [ ] `deploy.yml` を改修済み

### デプロイ確認

- [ ] GitHub Actions の `build-and-push` ジョブが成功している
- [ ] `ghcr.io/shun35960/myblog:latest` が GHCR に存在する
- [ ] サーバーで `podman ps` に `myblog` コンテナが表示される
- [ ] `curl http://localhost:8080` が応答する
- [ ] `https://p49e.com` でブログが表示される

### クリーンアップ

- [ ] 旧 Docker Compose を `docker compose down` で停止
- [ ] `deploy.sh`, `compose.yml`, `nginx/` を削除するPRをマージ
- [ ] GitHub の self-hosted runner を削除
