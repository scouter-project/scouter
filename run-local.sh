#!/usr/bin/env bash
# macOS local server + embedded webapp. Stop with Ctrl+C.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUNTIME="$ROOT/target/local-scouter"
BOOT="$ROOT/scouter.server.boot/target"

usage() {
    cat <<'EOF'
Usage: ./run-local.sh [--skip-build | --help]

Default: build server + webapp and run in the foreground (Ctrl+C to stop).
--skip-build  Run existing packaged artifacts.

Requires JDK 21 and Maven. JAVA_HOME selects the JDK for both build and run.
First-run ports: TCP/UDP 6100, HTTP 6188.
Config: target/local-scouter/conf/scouter.conf (preserved between runs).
Data/logs: target/local-scouter/database, target/local-scouter/logs.
This runs the SCOUTER web API and Swagger; Paper UI is a separate project.
EOF
}

BUILD=true
case "${1:-}" in
    '') ;;
    --skip-build) BUILD=false ;;
    --help|-h) usage; exit 0 ;;
    *) usage >&2; exit 1 ;;
esac
[[ $# -le 1 ]] || { usage >&2; exit 1; }

if [[ -n "${JAVA_HOME:-}" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi
command -v java >/dev/null || { echo 'JDK 21 is required.' >&2; exit 1; }

# A directory lock prevents two copies from sharing the same local database.
mkdir -p "$RUNTIME"
if ! mkdir "$RUNTIME/.run-lock" 2>/dev/null; then
    echo "Already running, or a stale lock exists: $RUNTIME/.run-lock" >&2
    echo 'After confirming the old process has stopped, remove that empty directory.' >&2
    exit 1
fi
trap 'rmdir "$RUNTIME/.run-lock"' EXIT

if "$BUILD"; then
    command -v mvn >/dev/null || { echo 'Maven is required.' >&2; exit 1; }
    (cd "$ROOT" && mvn -pl scouter.server.boot -am -Dmaven.test.skip=true -Dmaven.javadoc.skip=true package)
fi
[[ -f "$BOOT/scouter-server-boot.jar" && -d "$BOOT/lib" ]] || {
    echo 'Packaged artifacts missing. Run ./run-local.sh first.' >&2
    exit 1
}
mkdir -p "$RUNTIME/conf" "$RUNTIME/database" "$RUNTIME/logs"
if [[ ! -f "$RUNTIME/conf/scouter.conf" ]]; then
    cat > "$RUNTIME/conf/scouter.conf" <<'EOF'
net_tcp_listen_ip=127.0.0.1
net_udp_listen_ip=127.0.0.1
net_tcp_listen_port=6100
net_udp_listen_port=6100
net_http_server_enabled=true
net_http_port=6188
net_http_api_enabled=true
net_http_api_swagger_enabled=true
net_http_api_auth_ip_enabled=true
net_http_api_allow_ips=localhost,127.0.0.1,0:0:0:0:0:0:0:1,::1
net_http_api_auth_session_enabled=true
db_dir=./database
log_dir=./logs
EOF
fi

echo "Config: $RUNTIME/conf/scouter.conf"
echo 'Default API: http://localhost:6188/scouter/v1'
echo 'Default Swagger: http://localhost:6188/swagger/'
echo 'Starting server + embedded webapp. Stop with Ctrl+C.'
cd "$RUNTIME"
java --add-opens=java.base/java.lang=ALL-UNNAMED -Xmx1024m \
    -Dscouter.config="$RUNTIME/conf/scouter.conf" \
    -classpath "$BOOT/scouter-server-boot.jar" scouter.boot.Boot "$BOOT/lib"
