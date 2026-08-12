# GitHub Actions → Gitea Actions 移行記録

作業日: 2026-07-02

## 概要

CI/CD を GitHub Actions から自宅サーバーの Gitea Actions へ移行した。
コンテナレジストリは引き続き GHCR（ghcr.io）を使用し、デプロイはサーバー側の `update.sh`（podman pull → 再起動）で行う構成は変更なし。

```
Gitea (http://100.82.74.31:3000, v1.26.4)
  → Gitea Actions (act_runner, 同一ホストの Docker 上)
  → build & test → Trivy スキャン → ghcr.io/shun35960/myblog2 へ push
       ↓
サーバー側 update.sh が podman pull して再起動（ワークフロー外）
```

## リポジトリ側の変更

### `.gitea/workflows/deploy.yml`（新規・GitHub 版から移植）

GitHub 版 `.github/workflows/deploy.yml` からの主な変更点:

| 項目 | GitHub 版 | Gitea 版 |
|------|-----------|----------|
| GHCR 認証 | `secrets.GITHUB_TOKEN` | `secrets.GHCR_TOKEN`（GitHub PAT, `write:packages`） |
| コンテキスト | `github.sha` など | `gitea.sha` など（`github.*` も併用可） |
| ビルドキャッシュ | `type=gha` | 削除（GHA キャッシュは Gitea 非対応） |
| アクション参照 | タグ | コミット SHA 固定 |
| MongoDB service | `services: mongodb` + `TEST_MONGODB_URI` | 削除（下記参照） |
| アーティファクト | `actions/upload-artifact@v4` | `actions/upload-artifact@v3`（下記参照） |
| Trivy | `aquasecurity/trivy-action` | CLI直接インストール＋`trivy image`（下記参照） |

MongoDB service コンテナと `TEST_MONGODB_URI` は削除した。テストは flapdoodle の組み込み
MongoDB（`de.flapdoodle.embed.mongo`）を使用しており、`TEST_MONGODB_URI` はコードのどこからも
参照されていなかった。また act_runner ではジョブ自体がコンテナ内で動くため、service の
`localhost:27017` ポートマッピングはそもそもジョブコンテナに届かない。

### `.github/workflows/` の削除

- `claude.yml`: anthropics/claude-code-action は GitHub API・OIDC 前提のため Gitea では動作不可。削除。
- `deploy.yml.bak`: 旧 GitHub 版のバックアップ（git 管理外）。削除。

## サーバー側の構成（shun-sv, `~/gitea/`）

### compose.yml（最終形の要点)

```yaml
services:
  gitea:
    image: docker.gitea.com/gitea:latest
    container_name: gitea
    environment:
      - GITEA__server__ROOT_URL=http://100.82.74.31:3000/   # ブラウザ用（Tailscale IP）
      # ...
    ports:
      - "3000:3000"
      - "2222:22"
  runner:
    image: docker.gitea.com/act_runner:latest
    container_name: gitea-runner
    environment:
      - GITEA_INSTANCE_URL=http://gitea:3000    # コンテナ内部名でOK（下記の仕組み参照）
      - GITEA_RUNNER_REGISTRATION_TOKEN=<token>
      - CONFIG_FILE=/config.yaml
    volumes:
      - ./runner-data:/data
      - ./runner-config.yaml:/config.yaml
      - /var/run/docker.sock:/var/run/docker.sock
```

### runner-config.yaml

```yaml
container:
  network: gitea_default
```

## トラブルシューティング記録

### 症状: checkout が `Could not resolve host: gitea` で失敗

初回実行時、`actions/checkout` が `http://gitea:3000/shun/MyBlog2` を clone しようとして失敗した。

**原因**: act_runner は登録時の `GITEA_INSTANCE_URL` をジョブの `github.server_url` として渡す。
runner コンテナ自身は compose ネットワーク内にいるので `gitea` を解決できるが、
**act_runner はジョブごとに独立した Docker ネットワークを新規作成してジョブコンテナを起動する**ため、
ジョブコンテナからはホスト名 `gitea` を解決できない。

**試行1（失敗)**: `GITEA_INSTANCE_URL` を Tailscale IP（`http://100.82.74.31:3000`）に変更
→ コンテナからホストIPへの通信がファイアウォールでブロックされ、登録自体がタイムアウト。

**試行2（失敗)**: LAN IP（`http://192.168.1.249:3000`）に変更
→ 同じくタイムアウト。このホストのファイアウォールは **Tailscale 経由のみ許可**しており、
Docker ブリッジからホストIPへの通信は LAN IP 宛でもブロックされる。

**解決策**: `GITEA_INSTANCE_URL=http://gitea:3000` に戻し、runner の設定ファイルで
`container.network: gitea_default` を指定。ジョブコンテナも Gitea と同じ Docker ネットワークに
参加させることで、`gitea` というホスト名がジョブコンテナからも解決できるようになった。
ファイアウォールの変更は不要。

### 症状: upload-artifact が `GHESNotSupportedError` で失敗

`actions/upload-artifact@v4` は実装内で `GITHUB_SERVER_URL` のホスト名をチェックし、
github.com（および *.ghe.com）以外では「GHES では非対応」エラーを出して失敗する。
Gitea では `GITHUB_SERVER_URL` が Gitea インスタンスの URL になるため必ずこのエラーになる。

**解決策**: `actions/upload-artifact@v3` に戻す。Gitea は v3 のアーティファクト API に対応している。
v4 系を使いたい場合は Gitea 対応フォークの `christopherhx/gitea-upload-artifact@v4` という選択肢もある。

### 症状: trivy-action が `could not read Username for 'https://github.com'` で失敗

`aquasecurity/trivy-action` は内部の `setup-trivy` がインストールスクリプトを
github.com（aquasecurity/trivy リポジトリ）から `actions/checkout` で取得する。その際
`github.token`（Gitea Actions では Gitea のトークン）を github.com への認証ヘッダーに使うため、
github.com に 401 で拒否されて失敗する。

**解決策**: action をやめて Trivy CLI を直接インストールし、`trivy image` を実行する。

```yaml
- name: Install Trivy
  run: |
    curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin v0.70.0
- name: Run Trivy vulnerability scanner
  run: |
    trivy image --format table --exit-code 1 --ignore-unfixed \
      --pkg-types os,library --severity CRITICAL,HIGH <イメージ名>
```

### 補足

- `ROOT_URL`（ブラウザ用の表示 URL）と `GITEA_INSTANCE_URL`（runner・ジョブが使う URL）は別物。
  混在してよい。
- `GITEA_INSTANCE_URL` を変更した場合は `runner-data/.runner` を削除して再登録が必要
  （環境変数の変更だけでは反映されない）。
- compose.yml の環境変数変更は `docker compose up -d runner` でコンテナが再作成されて反映される
  （`stop` → `up` だけでは反映されないことがある）。

## 必要な Secret（Gitea リポジトリ設定 → Actions → Secrets）

| Secret 名 | 内容 | 用途 |
|-----------|------|------|
| `GHCR_TOKEN` | GitHub PAT（`write:packages` 権限） | master push 時の GHCR への docker push |

## 関連ドキュメント

- [CICD_MIGRATION.md](CICD_MIGRATION.md): GHCR + podman + update.sh 構成への移行手順（GitHub Actions 時代）
- サーバー側デプロイスクリプト: `update.sh` / `install.sh`
