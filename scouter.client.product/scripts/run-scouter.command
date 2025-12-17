#!/bin/bash
#
# Scouter Client Launcher for macOS
# 이 스크립트는 quarantine 속성을 제거하고 Scouter Client를 실행합니다.
#

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="scouter.client.app"
APP_PATH="$SCRIPT_DIR/$APP_NAME"

# 앱이 존재하는지 확인
if [ ! -d "$APP_PATH" ]; then
    echo "❌ 오류: $APP_NAME 을 찾을 수 없습니다."
    echo "이 스크립트를 $APP_NAME 과 같은 폴더에 위치시켜 주세요."
    read -p "Press Enter to exit..."
    exit 1
fi

# quarantine 속성 제거
echo "🔓 보안 속성을 제거하는 중..."
xattr -cr "$APP_PATH"

# 앱 실행
echo "🚀 Scouter Client를 실행합니다..."
open "$APP_PATH"

