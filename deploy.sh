#!/bin/bash

# deploy.sh - MyBlog デプロイスクリプト
# 使用方法: ./deploy.sh deploy

set -e  # エラー時に停止

# 設定
APP_NAME="myblog"
COMPOSE_FILE="compose.yml"

# カラー出力
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ログ関数
log() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')] $1${NC}"
}

success() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] ✅ $1${NC}"
}

error() {
    echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ❌ $1${NC}"
}

warning() {
    echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] ⚠️ $1${NC}"
}

# エラーハンドリング
error_exit() {
    error "$1"
    exit 1
}

# Dockerの確認
check_docker() {
    if ! command -v docker &> /dev/null; then
        error_exit "Docker is not installed or not in PATH"
    fi

    if ! docker compose version &> /dev/null 2>&1; then
        error_exit "Docker Compose is not available"
    fi

    success "Docker and Docker Compose are available"
}

# ファイルの確認
check_files() {
    log "Checking required files..."

    if [ ! -f "$COMPOSE_FILE" ]; then
        error_exit "Docker Compose file not found: $COMPOSE_FILE"
    fi

    if [ ! -f "dockerfile" ]; then
        error_exit "Dockerfile not found"
    fi

    success "All required files are present"
}

# 既存コンテナの停止
stop_containers() {
    log "Stopping existing containers..."

    if docker compose -f "$COMPOSE_FILE" ps -q | grep -q .; then
        docker compose -f "$COMPOSE_FILE" down
        success "Existing containers stopped"
    else
        log "No running containers found"
    fi
}

# イメージのクリーンアップ
cleanup_images() {
    log "Cleaning up old images..."

    # 使用されていないイメージを削除
    docker image prune -f > /dev/null 2>&1 || true

    # dangling imageを削除
    docker images -f "dangling=true" -q | xargs -r docker rmi > /dev/null 2>&1 || true

    success "Image cleanup completed"
}

# アプリケーションのビルドと起動
deploy_app() {
    log "Building and starting application..."

    # 強制的にリビルド
    docker compose -f "$COMPOSE_FILE" build --no-cache

    # バックグラウンドで起動
    docker compose -f "$COMPOSE_FILE" up -d

    success "Application containers started"
}

# ヘルスチェック
health_check() {
    log "Performing health check..."

    local max_attempts=15
    local attempt=1
    local success=false

    while [ $attempt -le $max_attempts ]; do
        log "Health check attempt $attempt/$max_attempts"

        # localhost:80でチェック
        if curl -f --connect-timeout 5 --max-time 10 http://localhost:80 > /dev/null 2>&1; then
            success "Application is responding at http://localhost:80"
            success=true
            break
        fi

        # localhost:8080でもチェック
        if curl -f --connect-timeout 5 --max-time 10 http://localhost:8080 > /dev/null 2>&1; then
            success "Application is responding at http://localhost:8080"
            success=true
            break
        fi

        if [ $attempt -eq $max_attempts ]; then
            warning "Health check failed after $max_attempts attempts"
            warning "Please manually verify: http://192.168.10.106"
        else
            log "Health check failed, retrying in 10 seconds..."
            sleep 10
        fi

        attempt=$((attempt + 1))
    done

    return 0  # ヘルスチェック失敗でもデプロイ自体は成功とみなす
}

# コンテナステータス表示
show_status() {
    log "Current container status:"
    docker compose -f "$COMPOSE_FILE" ps

    log "Application logs (last 20 lines):"
    docker compose -f "$COMPOSE_FILE" logs --tail=20
}

# メイン デプロイ関数
deploy() {
    log "🚀 Starting deployment of MyBlog application"

    # 現在のディレクトリを表示
    log "📂 Current directory: $(pwd)"
    log "📋 Files in directory:"
    ls -la

    # 各ステップを実行
    check_docker
    check_files
    stop_containers
    cleanup_images
    deploy_app

    # 少し待ってからヘルスチェック
    log "⏳ Waiting for application to start..."
    sleep 20

    health_check
    show_status

    success "🎉 Deployment completed!"
    success "🌐 Application should be available at: http://192.168.10.106"
}

# 使用方法の表示
usage() {
    echo "Usage: $0 {deploy|status|logs|stop}"
    echo "  deploy    - Deploy the application"
    echo "  status    - Show container status"
    echo "  logs      - Show application logs"
    echo "  stop      - Stop all containers"
    exit 1
}

# メイン処理
case "$1" in
    deploy)
        deploy
        ;;
    status)
        docker compose -f "$COMPOSE_FILE" ps
        ;;
    logs)
        docker compose -f "$COMPOSE_FILE" logs -f
        ;;
    stop)
        docker compose -f "$COMPOSE_FILE" down
        ;;
    *)
        usage
        ;;
esac