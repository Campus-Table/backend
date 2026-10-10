# 배포 (JAR + systemd)

서버에서 빌드하지 않고, **로컬에서 JAR을 빌드해 서버로 복사**하는 방식입니다. 서버에는 JRE 21만 있으면 됩니다.

```
로컬(./gradlew bootJar) --scp(-J bastion)--> 앱 서버 /opt/campus-table/app.jar --systemd--> :8080 <-- ALB(/api/health)
```

| 파일 | 용도 |
|---|---|
| `campus-table.service` | systemd 서비스 (서버의 `/etc/systemd/system/`) |
| `campus-table.env.example` | 환경변수 예시 (서버의 `/etc/campus-table.env`로 복사해 채움) |
| `deploy.sh` | 빌드 -> 업로드 -> 재시작 -> 헬스체크, 실패 시 이전 JAR로 복구 |

## 1. 서버 최초 설정 (앱 서버마다 한 번)

bastion을 거쳐 접속합니다: `ssh -J 사용자@bastion공인IP 사용자@앱서버사설IP`

```bash
# Java 21 (OS에 맞게 하나). 패키지가 없으면 Eclipse Temurin 21 JRE를 설치하세요.
sudo apt update && sudo apt install -y openjdk-21-jre-headless      # Ubuntu/Debian
# sudo dnf install -y java-21-openjdk-headless                      # Rocky/CentOS/Alma
java -version    # 21 확인

# 실행 전용 계정과 디렉터리
sudo useradd --system --home /opt/campus-table --shell /usr/sbin/nologin campus
sudo install -d -o campus -g campus /opt/campus-table
```

서비스 파일과 환경변수 파일을 올립니다(로컬에서 실행):

```bash
scp -J 사용자@bastion공인IP deploy/campus-table.service 사용자@앱서버사설IP:/tmp/
scp -J 사용자@bastion공인IP deploy/campus-table.env.example 사용자@앱서버사설IP:/tmp/campus-table.env
```

서버에서:

```bash
sudo install -m 644 /tmp/campus-table.service /etc/systemd/system/campus-table.service
sudo install -m 600 -o root -g root /tmp/campus-table.env /etc/campus-table.env
rm /tmp/campus-table.service /tmp/campus-table.env
sudo nano /etc/campus-table.env        # 빈 값(DB, Redis, 관리자, 메일, 토스, 버킷 키...)을 채운다
sudo systemctl daemon-reload
sudo systemctl enable campus-table
```

**비밀 값은 서버의 `/etc/campus-table.env`에만 둡니다.** git, 채팅, 이 저장소 어디에도 올리지 마세요. 첫 기동에 `SEED_ENABLED=true`로 초기 데이터가 들어가면 `false`로 바꾸거나 줄을 지웁니다.

## 2. 배포

```bash
BASTION=사용자@bastion공인IP APP_HOSTS="사용자@앱서버사설IP" bash deploy/deploy.sh
# 서버가 여러 대면 APP_HOSTS="사용자@IP1 사용자@IP2"
```

`deploy.sh`는 JAR을 만들어 올리고, 서비스를 재시작한 뒤 `GET /api/health`가 통과할 때까지 최대 90초 기다립니다. 실패하면 직전 JAR(`app.jar.prev`)로 되돌리고 종료 코드 1을 반환합니다. 최초 설정 뒤 첫 배포가 곧 첫 기동입니다.

Windows에서는 Git Bash 또는 WSL에서 실행하세요(ssh/scp 필요).

## 3. 확인

서버에서:

```bash
curl -s localhost:8080/api/health                 # {"status":"UP"}
sudo systemctl status campus-table
sudo journalctl -u campus-table -f                # 로그
```

그다음 ALB 콘솔의 타깃 그룹이 `Healthy`인지(경로 `/api/health`, 포트 8080, 성공 코드 200), 브라우저에서 `https://www.campus-table.kro.kr/api/health`가 열리는지 확인합니다.

## 4. 문제 해결

| 증상 | 확인 |
|---|---|
| ALB가 `503 Service Unavailable` | 타깃 그룹에 서버가 등록·`Healthy`인지, 앱이 떠 있는지(위 확인), 앱 서버 ACG가 LB 서브넷(`10.20.30.0/24`)에서 8080을 허용하는지 |
| 기동 직후 종료 | `journalctl`에서 DB/Redis 접속 오류(주소, 계정, ACG 3306/6379), 환경변수 누락 |
| Redis 관련 기동 오류 | prod 프로파일은 앱이 `CONFIG`를 하지 않음. 인프라가 `notify-keyspace-events=Egx` 설정 |
| 로그인이 계속 풀림 | HTTPS가 아닌데 `COOKIE_SECURE`가 true(기본)인 경우. HTTP 임시 운영이면 `COOKIE_SECURE=false` |
| 이미지 업로드 503 | `OBJECT_STORAGE_*` 값과 버킷 권한 |

롤백: 서버에서 `sudo cp -p /opt/campus-table/app.jar.prev /opt/campus-table/app.jar && sudo systemctl restart campus-table`

## 5. 참고

- 앱은 기동 시 DB 테이블/컬럼을 자동 생성하므로(`ddl-auto: update`) DB 계정에 DDL 권한이 필요합니다. Flyway 도입 전까지의 임시 방식입니다.
- 서버가 여러 대여도 각 서버에서 같은 환경변수 파일을 쓰면 됩니다. 세션은 Redis, 데이터는 DB에 있습니다.
- 시연 계정/시뮬레이터(`demo/`)는 이 배포와 별개입니다(`demo/README.md`).
- 이 파일들은 실제 서버에서 실행해 검증하지 않았습니다(스크립트 문법만 확인). 첫 배포 때 오류가 나면 알려 주세요.

## 프론트 함께 배포

현재 프론트는 별도 정적 호스팅을 사용하며 백엔드 ALB 대상 포트는 8080을 유지합니다. CORS·운영 환경변수 적용과 대안 Nginx 구성은 [frontend.md](frontend.md)를 참고하세요.
