# Cloud Run 自動デプロイのセットアップ

`master` への push で以下が自動実行される（`.github/workflows/deploy.yml`）。

```
build (テスト) → publish (Trivy → ghcr / Artifact Registry へ push) → deploy (Cloud Run)
```

- GCP への認証は Workload Identity Federation（サービスアカウントキー不要）
- デプロイは Trivy でスキャンしたイメージを **digest 指定** で行う
- 変更するのはイメージだけ。環境変数・Secret・メモリ・ドメインマッピング等は既存サービスの設定がそのまま残る
- デプロイ後に Cloud Run の URL と `https://p49e.com/` が 200 を返すか確認する

以下は一度だけ行う GCP / GitHub 側の準備。

## 0. 変数

```bash
PROJECT_ID=<GCPプロジェクトID>
REGION=asia-northeast1            # 既存サービスのリージョン
SERVICE=<既存の Cloud Run サービス名>
AR_REPO=myblog
GITHUB_REPO=shun35960/MyBlog2
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')
```

既存サービスのリージョン・名前は `gcloud run services list --project "$PROJECT_ID"` で確認できる。

## 1. API の有効化

```bash
gcloud services enable \
  run.googleapis.com artifactregistry.googleapis.com \
  iamcredentials.googleapis.com sts.googleapis.com \
  --project "$PROJECT_ID"
```

## 2. Artifact Registry リポジトリ

```bash
gcloud artifacts repositories create "$AR_REPO" \
  --repository-format=docker --location="$REGION" --project "$PROJECT_ID"
```

既にある場合はその名前を `GCP_AR_REPOSITORY` に設定する（未設定時は `myblog`）。

## 3. デプロイ用サービスアカウント

```bash
gcloud iam service-accounts create github-deployer \
  --display-name="GitHub Actions deployer" --project "$PROJECT_ID"
DEPLOY_SA=github-deployer@${PROJECT_ID}.iam.gserviceaccount.com

# Cloud Run のデプロイ
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${DEPLOY_SA}" --role=roles/run.developer
# Artifact Registry への push
gcloud artifacts repositories add-iam-policy-binding "$AR_REPO" \
  --location="$REGION" --project "$PROJECT_ID" \
  --member="serviceAccount:${DEPLOY_SA}" --role=roles/artifactregistry.writer
```

Cloud Run サービスの **実行用** サービスアカウントに対して actAs 権限を付ける。

```bash
RUNTIME_SA=$(gcloud run services describe "$SERVICE" --region "$REGION" \
  --project "$PROJECT_ID" --format='value(spec.template.spec.serviceAccountName)')
# 空なら Compute Engine のデフォルト SA が使われている
RUNTIME_SA=${RUNTIME_SA:-${PROJECT_NUMBER}-compute@developer.gserviceaccount.com}

gcloud iam service-accounts add-iam-policy-binding "$RUNTIME_SA" \
  --project "$PROJECT_ID" \
  --member="serviceAccount:${DEPLOY_SA}" --role=roles/iam.serviceAccountUser
```

## 4. Workload Identity Federation

このリポジトリからのトークンだけを受け付けるよう `attribute-condition` で絞る。

```bash
gcloud iam workload-identity-pools create github \
  --location=global --display-name="GitHub Actions" --project "$PROJECT_ID"

gcloud iam workload-identity-pools providers create-oidc myblog2 \
  --location=global --workload-identity-pool=github --project "$PROJECT_ID" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" \
  --attribute-condition="assertion.repository=='${GITHUB_REPO}'"

gcloud iam service-accounts add-iam-policy-binding "$DEPLOY_SA" \
  --project "$PROJECT_ID" --role=roles/iam.workloadIdentityUser \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github/attribute.repository/${GITHUB_REPO}"

# GitHub に登録するプロバイダ名
echo "projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github/providers/myblog2"
```

## 5. GitHub の Repository variables

Settings → Secrets and variables → Actions → **Variables** に登録する（いずれも秘密情報ではない）。

| 名前 | 値 |
|---|---|
| `GCP_PROJECT_ID` | `$PROJECT_ID` |
| `GCP_REGION` | `$REGION`（`asia-northeast1` なら省略可） |
| `GCP_AR_REPOSITORY` | `$AR_REPO`（`myblog` なら省略可） |
| `GCP_WIF_PROVIDER` | 手順 4 の最後に出力された値 |
| `GCP_DEPLOY_SA` | `$DEPLOY_SA` |
| `CLOUD_RUN_SERVICE` | `$SERVICE` |

`deploy` ジョブは GitHub の Environment `production` を使う。Settings → Environments で
承認者（Required reviewers）を設定すれば、本番デプロイ前に手動承認を挟める（任意）。

## 6. MongoDB Atlas のアクセス許可

Cloud Run の送信元 IP は固定されないため、Atlas の Network Access で `0.0.0.0/0` を許可する。
（`0.0.0.0/24` は 0.0.0.0〜0.0.0.255 しか許可しないので接続できない点に注意）
DB ユーザーのパスワードは十分に強いものにし、接続は TLS（`mongodb+srv://`）を使う。

## トラブルシューティング

| 症状 | 確認すること |
|---|---|
| `auth` で失敗 | `GCP_WIF_PROVIDER` の値、手順 4 の `workloadIdentityUser` 付与 |
| AR への push が 403 | 手順 3 の `artifactregistry.writer` |
| デプロイが `iam.serviceaccounts.actAs` で失敗 | 手順 3 の実行用 SA への `serviceAccountUser` |
| Smoke test が失敗 | Cloud Run のログ（`gcloud run services logs read $SERVICE --region $REGION`） |

問題のあるリビジョンを戻すときは、前のリビジョンへトラフィックを切り替える。

```bash
gcloud run revisions list --service "$SERVICE" --region "$REGION"
gcloud run services update-traffic "$SERVICE" --region "$REGION" --to-revisions=<リビジョン名>=100
```
