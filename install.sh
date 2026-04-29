#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_ENV_FILE="$ROOT_DIR/.env"
FALLBACK_ENV_FILE="$HOME/.env"
ENV_FILE="${ENV_FILE:-}"
IMAGE_NAME="${IMAGE_NAME:-ghcr.io/shun35960/myblog2:latest}"
CONTAINER_NAME="${CONTAINER_NAME:-myblog-app}"
APP_PORT="${APP_PORT:-8080}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
JAVA_OPTS="${JAVA_OPTS:--Dfile.encoding=UTF-8 -Duser.timezone=Asia/Tokyo -XX:+UseG1GC -XX:MaxRAMPercentage=75}"

usage() {
    cat <<'EOF'
Usage: ./install.sh

Environment variables:
  ENV_FILE        Optional. Path to env file. Defaults to ./.env, fallback ~/.env
  IMAGE_NAME      Optional. Defaults to ghcr.io/shun35960/myblog2:latest
  CONTAINER_NAME  Optional. Defaults to myblog-app
  APP_PORT        Optional. Defaults to 8080
  GHCR_USERNAME   Optional. Used for podman login when image is private
  GHCR_TOKEN      Optional. Used for podman login when image is private
EOF
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "Command not found: $1" >&2
        exit 1
    }
}

command_exists() {
    command -v "$1" >/dev/null 2>&1
}

resolve_env_file() {
    if [[ -n "$ENV_FILE" ]]; then
        [[ -f "$ENV_FILE" ]] || {
            echo "Env file not found: $ENV_FILE" >&2
            exit 1
        }
        return 0
    fi

    if [[ -f "$DEFAULT_ENV_FILE" ]]; then
        ENV_FILE="$DEFAULT_ENV_FILE"
        return 0
    fi

    if [[ -f "$FALLBACK_ENV_FILE" ]]; then
        ENV_FILE="$FALLBACK_ENV_FILE"
        return 0
    fi

    echo "Env file not found. Create $DEFAULT_ENV_FILE or set ENV_FILE=/path/to/.env" >&2
    exit 1
}

require_env_key() {
    local key="$1"

    if ! grep -Eq "^[[:space:]]*${key}=" "$ENV_FILE"; then
        echo "Required key $key is missing in $ENV_FILE" >&2
        exit 1
    fi
}

env_value_from_file() {
    local name="$1"

    awk -F= -v key="$name" '
        $0 ~ "^[[:space:]]*" key "=" {
            value = substr($0, index($0, "=") + 1)
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
            print value
        }
    ' "$ENV_FILE" | tail -n1
}

resolved_value() {
    local name="$1"
    local default_value="${2:-}"
    local current_value="${!name:-}"

    if [[ -n "$current_value" ]]; then
        printf '%s\n' "$current_value"
        return 0
    fi

    current_value="$(env_value_from_file "$name")"
    if [[ -n "$current_value" ]]; then
        printf '%s\n' "$current_value"
        return 0
    fi

    printf '%s\n' "$default_value"
}

ensure_dirs() {
    mkdir -p "$ROOT_DIR/logs" "$ROOT_DIR/uploads"
}

port_is_in_use() {
    local port="$1"

    if command_exists ss; then
        ss -H -ltn "sport = :${port}" 2>/dev/null | grep -q .
        return $?
    fi

    if command_exists lsof; then
        lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
        return $?
    fi

    return 1
}

print_port_diagnostics() {
    local port="$1"

    if command_exists ss; then
        ss -ltnp "( sport = :${port} )" 2>/dev/null || true
        return 0
    fi

    if command_exists lsof; then
        lsof -nP -iTCP:"$port" -sTCP:LISTEN 2>/dev/null || true
    fi
}

login_if_configured() {
    if [[ -n "${GHCR_USERNAME:-}" && -n "${GHCR_TOKEN:-}" ]]; then
        printf '%s' "$GHCR_TOKEN" | podman login ghcr.io -u "$GHCR_USERNAME" --password-stdin
    fi
}

remove_existing_container() {
    podman rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
}

main() {
    local app_port mongodb_uri spring_profiles_active java_opts

    require_command podman
    resolve_env_file
    require_env_key MONGODB_URI
    ensure_dirs
    app_port="$(resolved_value APP_PORT "$APP_PORT")"
    mongodb_uri="$(resolved_value MONGODB_URI)"
    spring_profiles_active="$(resolved_value SPRING_PROFILES_ACTIVE "$SPRING_PROFILES_ACTIVE")"
    java_opts="$(resolved_value JAVA_OPTS "$JAVA_OPTS")"

    echo "=== MyBlog initial setup ==="
    echo "Env file: $ENV_FILE"
    echo "Image: $IMAGE_NAME"
    echo "Container: $CONTAINER_NAME"
    echo "Host port: $app_port"

    login_if_configured

    echo "Pulling image from ghcr..."
    podman pull "$IMAGE_NAME"

    echo "Recreating container..."
    remove_existing_container
    if port_is_in_use "$app_port"; then
        echo "Host port ${app_port} is already in use." >&2
        print_port_diagnostics "$app_port" >&2
        echo "Set APP_PORT in $ENV_FILE or run APP_PORT=18080 ./install.sh" >&2
        exit 1
    fi
    if ! podman run -d \
        --name "$CONTAINER_NAME" \
        --restart unless-stopped \
        -p "${app_port}:8080" \
        --env-file "$ENV_FILE" \
        -e SPRING_DATA_MONGODB_URI="$mongodb_uri" \
        -e SPRING_PROFILES_ACTIVE="$spring_profiles_active" \
        -e SERVER_PORT="8080" \
        -e JAVA_OPTS="$java_opts" \
        -v "$ROOT_DIR/logs:/app/logs:Z,U" \
        -v "$ROOT_DIR/uploads:/app/uploads:Z,U" \
        "$IMAGE_NAME"; then
        echo "Failed to start container. If the port is already in use, set APP_PORT in $ENV_FILE or run APP_PORT=18080 ./install.sh" >&2
        exit 1
    fi

    echo "=== Setup complete ==="
    podman ps --filter "name=${CONTAINER_NAME}"
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
    usage
    exit 0
fi

main
