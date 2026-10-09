#!/usr/bin/env bash
# 로컬에서 JAR을 빌드해 bastion을 거쳐 앱 서버로 올리고 재시작한다. 실패하면 이전 JAR로 되돌린다.
# 사용: BASTION=사용자@bastion공인IP APP_HOSTS="사용자@앱서버사설IP [사용자@다른서버IP ...]" bash deploy/deploy.sh
# 필요: 로컬에 ssh/scp(Git Bash, WSL, macOS/Linux), JDK 21. 서버에는 deploy/README.md의 최초 설정이 되어 있어야 한다.
set -euo pipefail

: "${BASTION:?BASTION=사용자@bastion공인IP 를 지정하세요}"
: "${APP_HOSTS:?APP_HOSTS=\"사용자@앱서버사설IP ...\" 를 지정하세요}"
SSH_OPTS="-o ServerAliveInterval=30"

cd "$(dirname "$0")/.."
./gradlew bootJar
JAR=$(ls build/libs/campus_table-*-SNAPSHOT.jar | grep -v plain | head -1)
echo "배포할 JAR: $JAR"

for HOST in $APP_HOSTS; do
  echo "== $HOST"
  scp $SSH_OPTS -J "$BASTION" "$JAR" "$HOST:/tmp/campus-table-new.jar"
  ssh $SSH_OPTS -J "$BASTION" "$HOST" 'bash -s' <<'REMOTE'
set -euo pipefail
sudo install -d -o campus -g campus /opt/campus-table
[ -f /opt/campus-table/app.jar ] && sudo cp -p /opt/campus-table/app.jar /opt/campus-table/app.jar.prev
sudo install -m 644 -o campus -g campus /tmp/campus-table-new.jar /opt/campus-table/app.jar
rm -f /tmp/campus-table-new.jar
sudo systemctl restart campus-table
for i in $(seq 1 30); do
  if curl -fsS http://localhost:8080/api/health >/dev/null 2>&1; then echo "헬스체크 통과 (${i}회 시도)"; exit 0; fi
  sleep 3
done
echo "헬스체크 실패: 이전 JAR로 되돌립니다. 로그: sudo journalctl -u campus-table -n 100"
if [ -f /opt/campus-table/app.jar.prev ]; then
  sudo cp -p /opt/campus-table/app.jar.prev /opt/campus-table/app.jar
  sudo systemctl restart campus-table
fi
exit 1
REMOTE
done
echo "완료"
