#!/bin/bash
#
# Scouter Client Launcher for macOS
# 번들 JRE(JustJ)가 scouter.ini에 설정되어 있으므로
# 네이티브 런처가 자동으로 번들 JRE를 사용합니다.
# 이 스크립트는 번들 JRE가 없는 경우에만 시스템 Java를 확인합니다.
#

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="scouter.client.app"
APP_PATH="$SCRIPT_DIR/$APP_NAME"

# 앱이 존재하는지 확인
if [ ! -d "$APP_PATH" ]; then
    echo "Error: $APP_NAME not found."
    echo "Please place this script in the same folder as $APP_NAME."
    read -p "Press Enter to exit..."
    exit 1
fi

# 번들 JRE 확인 (plugins 하위에 JustJ JRE가 배치됨)
BUNDLED_JRE=$(find "$APP_PATH/Contents/Eclipse/plugins" -path "*/justj*/jre/bin/java" 2>/dev/null | head -1)
if [ -z "$BUNDLED_JRE" ]; then
    # 시스템 Java 확인
    if ! command -v java &> /dev/null; then
        echo "Error: Java not found. Please install Java 21+."
        echo "  Recommended: https://adoptium.net/"
        read -p "Press Enter to exit..."
        exit 1
    fi
    JAVA_VER=$(java -version 2>&1 | head -1 | sed 's/.*"\(.*\)".*/\1/' | cut -d. -f1)
    if [ "$JAVA_VER" -lt 21 ] 2>/dev/null; then
        echo "Error: Java 21+ required. Current: Java $JAVA_VER"
        echo "  Recommended: https://adoptium.net/"
        read -p "Press Enter to exit..."
        exit 1
    fi
fi

# quarantine 속성 제거
xattr -cr "$APP_PATH"

# 앱 실행
open "$APP_PATH"
