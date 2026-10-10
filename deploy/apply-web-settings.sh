#!/usr/bin/env bash
# 사용: bash deploy/apply-web-settings.sh 사용자@서버1 사용자@서버2
# bastion 사용 시: BASTION=사용자@bastion bash deploy/apply-web-settings.sh ...
# 기존 DB/Redis/키는 유지하고 웹 관련 변수 3개만 변경한다.
set -euo pipefail
[ "$#" -ge 1 ] || { echo '앱 서버 SSH 대상을 지정하세요.' >&2; exit 1; }
SSH_ARGS=(-o ServerAliveInterval=30)
if [ -n "${BASTION:-}" ]; then SSH_ARGS+=(-J "$BASTION"); fi
REMOTE_PYTHON=$(cat <<'PYTHON'
from pathlib import Path
from datetime import datetime
import shutil
p = Path("/etc/campus-table.env")
s = p.read_text()
backup = str(p) + ".backup-" + datetime.now().strftime("%Y%m%d%H%M%S")
shutil.copy2(p, backup)
Path(backup).chmod(0o600)
updates = {"CORS_ALLOWED_ORIGINS": "https://campus-table.kro.kr,https://www.campus-table.kro.kr", "SPRING_PROFILES_ACTIVE": "prod", "COOKIE_SECURE": "true"}
lines = []
for line in s.splitlines():
    key = line.split("=", 1)[0].strip()
    if key not in updates:
        lines.append(line)
for key, value in updates.items():
    lines.append(key + "=" + value)
p.write_text(chr(10).join(lines) + chr(10))
p.chmod(0o600)
print("웹 설정 적용 완료. 백업: " + backup)
PYTHON
)
# 원격 셸에서도 Python의 따옴표와 줄바꿈을 보존한다.
REMOTE_PYTHON_ESCAPED=${REMOTE_PYTHON//\'/\'\\\'\'}
for APP_HOST in "$@"; do
  ssh -t "${SSH_ARGS[@]}" "$APP_HOST" "sudo python3 -c '$REMOTE_PYTHON_ESCAPED' && sudo systemctl restart campus-table"
  ssh "${SSH_ARGS[@]}" "$APP_HOST" 'for attempt in $(seq 1 30); do if curl -fsS --max-time 3 http://127.0.0.1:8080/api/health; then exit 0; fi; sleep 2; done; echo "헬스체크 실패: 서버 로그 확인 필요" >&2; exit 1'
done
