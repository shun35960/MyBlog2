#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/myblog"
QUADLET_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/containers/systemd"
GENERATED_ENV_FILE="$CONFIG_DIR/myblog-app.env"
QUADLET_FILE="$QUADLET_DIR/myblog-app.container"
SERVICE_NAME="myblog-app.service"
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
    mkdir -p "$CONFIG_DIR" "$QUADLET_DIR"
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

check_linger() {
    local linger_state

    if ! command_exists loginctl; then
        return 0
    fi

    linger_state="$(loginctl show-user "$USER" --property=Linger --value 2>/dev/null || true)"
    if [[ "$linger_state" != "yes" ]]; then
        cat <<EOF
WARNING: loginctl enable-linger is not enabled for $USER.
The rootless service can stop when the user session is restarted or logged out.
Run once with sudo:
  sudo loginctl enable-linger $USER
EOF
    fi
}

write_generated_env_file() {
    local mongodb_uri="$1"
    local spring_profiles_active="$2"
    local java_opts="$3"

    cat >"$GENERATED_ENV_FILE" <<EOF
SPRING_MONGODB_URI=$mongodb_uri
SPRING_PROFILES_ACTIVE=$spring_profiles_active
SERVER_PORT=8080
JAVA_OPTS=$java_opts
EOF
    chmod 600 "$GENERATED_ENV_FILE"
}

write_quadlet_file() {
    local app_port="$1"

    cat >"$QUADLET_FILE" <<EOF
[Unit]
Description=MyBlog application container
After=network-online.target
Wants=network-online.target

[Container]
Image=$IMAGE_NAME
ContainerName=$CONTAINER_NAME
PublishPort=$app_port:8080
EnvironmentFile=$GENERATED_ENV_FILE
Volume=$ROOT_DIR/logs:/app/logs:Z,U
Volume=$ROOT_DIR/uploads:/app/uploads:Z,U
Pull=newer

[Service]
Restart=always
TimeoutStopSec=70

[Install]
WantedBy=default.target
EOF
}

restart_service() {
    systemctl --user daemon-reload
    systemctl --user start "$SERVICE_NAME"
    systemctl --user restart "$SERVICE_NAME"
}

stop_existing_service() {
    systemctl --user stop "$SERVICE_NAME" >/dev/null 2>&1 || true
}

main() {
    local app_port mongodb_uri spring_profiles_active java_opts

    require_command podman
    require_command systemctl
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

    check_linger
    login_if_configured

    echo "Pulling image from ghcr..."
    podman pull "$IMAGE_NAME"

    stop_existing_service

    if port_is_in_use "$app_port"; then
        echo "Host port ${app_port} is already in use." >&2
        print_port_diagnostics "$app_port" >&2
        echo "Set APP_PORT in $ENV_FILE or run APP_PORT=18080 ./install.sh" >&2
        exit 1
    fi

    write_generated_env_file "$mongodb_uri" "$spring_profiles_active" "$java_opts"
    write_quadlet_file "$app_port"

    echo "Installing rootless Quadlet service..."
    restart_service

    echo "=== Setup complete ==="
    systemctl --user --no-pager --full status "$SERVICE_NAME" || true
    podman ps --filter "name=${CONTAINER_NAME}" || true
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
    usage
    exit 0
fi

main
