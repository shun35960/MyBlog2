# Cloud Run 自動デプロイ セットアップ手順

`.github/workflows/deploy.yml` の `deploy` ジョブは、`master` への push で
ghcr.io に push されたイメージをそのまま Cloud Run に反映します。

```
build (test) → publish (Trivy scan → ghcr.io push) → deploy (Cloud Run)
```

デプロイするイメージは `ghcr.io/<owner>/myblog2:<commit SHA>` です。
Cloud Run は ghcr.io などの外部レジストリの公開イメージを **最大 1 時間キャッシュする** ため、
`:latest` ではなく不変のコミット SHA タグを指定しています。

認証は Workload Identity Federation（WIF）で行うため、
サービスアカウントの JSON キーを GitHub に保存する必要はありません。

---

## 前提条件

- ghcr.io のパッケージ `myblog2` が **public** であること
  （Cloud Run が直接 pull できるのは公開イメージのみ。private にする場合は
  Artifact Registry のリモートリポジトリ経由に切り替える必要があります）
- Cloud Run サービスが既に作成済みで、`SPRING_DATA_MONGODB_URI` などの
  環境変数・シークレットが設定済みであること
  （`gcloud run deploy --image` はイメージ以外の既存設定を引き継ぐため、
  ワークフロー側で環境変数を指定していません）
- `gcloud` CLI でプロジェクトの管理者権限があること

---

## 1. 変数を決める

以下は自分の環境に合わせて置き換えて下さい。

```bash
export PROJECT_ID="your-gcp-project-id"
export REGION="asia-northeast1"          # Cloud Run サービスのリージョン
export SERVICE="myblog"                  # Cloud Run サービス名
export GITHUB_REPO="shun35960/MyBlog2"   # owner/repo
export SA_NAME="github-actions-deployer"
export POOL="github-pool"
export PROVIDER="github-provider"

gcloud config set project "$PROJECT_ID"
export PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
```

## 2. 必要な API を有効化

```bash
gcloud services enable \
  run.googleapis.com \
  iamcredentials.googleapis.com \
  sts.googleapis.com
```

## 3. デプロイ用サービスアカウントを作成

```bash
gcloud iam service-accounts create "$SA_NAME" \
  --display-name="GitHub Actions Cloud Run deployer"

export SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
```

必要なロールを付与します。

```bash
# Cloud Run サービスの更新権限
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/run.developer"

# Cloud Run サービスが使うランタイムサービスアカウントを「なりすます」権限。
# これが無いと deploy 時に iam.serviceAccounts.actAs のエラーになる。
export RUNTIME_SA="$(gcloud run services describe "$SERVICE" --region "$REGION" \
  --format='value(spec.template.spec.serviceAccountName)')"
: "${RUNTIME_SA:=${PROJECT_NUMBER}-compute@developer.gserviceaccount.com}"

gcloud iam service-accounts add-iam-policy-binding "$RUNTIME_SA" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/iam.serviceAccountUser"
```

## 4. Workload Identity Pool / Provider を作成

```bash
gcloud iam workload-identity-pools create "$POOL" \
  --location="global" \
  --display-name="GitHub Actions Pool"

gcloud iam workload-identity-pools providers create-oidc "$PROVIDER" \
  --location="global" \
  --workload-identity-pool="$POOL" \
  --display-name="GitHub Actions Provider" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner" \
  --attribute-condition="assertion.repository == '${GITHUB_REPO}'"
```

`--attribute-condition` は **必須** です。これを省くと任意の GitHub リポジトリから
このサービスアカウントを借用できてしまいます。

## 5. リポジトリからサービスアカウントの借用を許可

```bash
gcloud iam service-accounts add-iam-policy-binding "$SA_EMAIL" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL}/attribute.repository/${GITHUB_REPO}"
```

## 6. GitHub に登録する値を取得

```bash
gcloud iam workload-identity-pools providers describe "$PROVIDER" \
  --location="global" \
  --workload-identity-pool="$POOL" \
  --format='value(name)'
# => projects/<PROJECT_NUMBER>/locations/global/workloadIdentityPools/github-pool/providers/github-provider

echo "$SA_EMAIL"
# => github-actions-deployer@<PROJECT_ID>.iam.gserviceaccount.com
```

---

## 7. GitHub 側の設定（ユーザー操作が必要）

`Settings → Secrets and variables → Actions` で以下を登録します。

### Variables（機密ではないので Variables タブ）

| 名前 | 値の例 |
| --- | --- |
| `GCP_PROJECT_ID` | `your-gcp-project-id` |
| `CLOUD_RUN_SERVICE` | `myblog` |
| `CLOUD_RUN_REGION` | `asia-northeast1` |

### Secrets

| 名前 | 値 |
| --- | --- |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | 手順 6 で取得した `projects/.../providers/github-provider` |
| `GCP_SERVICE_ACCOUNT` | 手順 6 で取得したサービスアカウントのメールアドレス |

いずれかが未登録の場合、`deploy` ジョブは最初の
`Validate deployment settings` ステップで不足している名前を表示して停止します。

---

## 動作確認

1. `master` に push する
2. Actions で `build → publish → deploy` が順に成功することを確認
3. ジョブサマリに表示される Cloud Run の URL を開いて反映を確認

## ロールバック

Cloud Run のリビジョンを戻すのが最短です。

```bash
gcloud run services update-traffic "$SERVICE" \
  --region "$REGION" \
  --to-revisions=<戻したいリビジョン名>=100
```

特定コミットのイメージを再デプロイする場合は以下です。

```bash
gcloud run deploy "$SERVICE" \
  --region "$REGION" \
  --image "ghcr.io/shun35960/myblog2:<commit SHA>"
```

## トラブルシューティング

| 症状 | 原因と対処 |
| --- | --- |
| `Permission 'iam.serviceAccounts.actAs' denied` | 手順 3 の `roles/iam.serviceAccountUser` が未付与 |
| `unable to impersonate` / OIDC 交換に失敗 | 手順 5 の `principalSet` の `GITHUB_REPO` の大文字小文字を確認（`assertion.repository` の値と完全一致が必要） |
| `Failed to fetch image` | ghcr.io のパッケージが private になっている |
| デプロイは成功したが内容が古い | `:latest` を手動で指定していないか確認（外部レジストリの公開イメージは最大 1 時間キャッシュされる） |
