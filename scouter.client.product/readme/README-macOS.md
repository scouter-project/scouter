# Scouter Client for macOS

## 🚨 처음 실행 전 필수 작업

macOS의 보안 정책(Gatekeeper)으로 인해 서명되지 않은 앱은 실행이 차단됩니다.
처음 실행하기 전에 아래 명령어를 **한 번만** 실행해주세요.

### 방법 1: 터미널 명령어 (권장)

```bash
cd /Applications  # 또는 scouter.client.app이 있는 디렉토리
xattr -cr scouter.client.app
```

### 방법 2: 실행 스크립트 사용

패키지에 포함된 `run-scouter.command` 파일을 더블클릭하면 자동으로 처리됩니다.

### 방법 3: 시스템 환경설정에서 허용

1. scouter.client.app 더블클릭 (경고 발생)
2. **시스템 환경설정** → **보안 및 개인 정보 보호** → **일반** 탭
3. "scouter.client.app이(가) 차단되었습니다" 옆의 **"확인 없이 열기"** 클릭

## ⚠️ 왜 이런 작업이 필요한가요?

Apple은 인터넷에서 다운로드한 앱에 `quarantine` 속성을 추가합니다.
Apple Developer 인증서로 서명되지 않은 앱은 Gatekeeper가 실행을 차단합니다.
`xattr -cr` 명령어는 이 quarantine 속성을 제거합니다.

## 📋 시스템 요구사항

- macOS 10.14 (Mojave) 이상
- Java 11 이상 (내장됨)

