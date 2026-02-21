#!/bin/bash
#
# Scouter Client Launcher for Linux
# 번들 JRE(JustJ)가 scouter.ini에 설정되어 있으므로
# 네이티브 런처가 자동으로 번들 JRE를 사용합니다.
# 이 스크립트는 번들 JRE가 없는 경우에만 시스템 Java를 확인합니다.
#

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# 번들 JRE 확인 (plugins 하위에 JustJ JRE가 배치됨)
BUNDLED_JRE=$(find "$SCRIPT_DIR/plugins" -path "*/justj*/jre/bin/java" 2>/dev/null | head -1)
if [ -z "$BUNDLED_JRE" ]; then
    # 시스템 Java 확인
    if ! command -v java &> /dev/null; then
        echo "Error: Java not found. Please install Java 21+."
        echo "  Recommended: https://adoptium.net/"
        exit 1
    fi
fi

exec "$SCRIPT_DIR/scouter" "$@"
