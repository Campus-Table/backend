# 프론트 정적 호스팅 연동

현재 선택한 방식은 프론트를 별도 정적 호스팅에 배포하고 백엔드 앱 서버 두 대와 ALB 대상 포트 8080을 유지하는 구성이다.

- 프론트: https://campus-table.kro.kr (www를 사용하면 해당 주소도 연결)
- 백엔드 예정 주소: https://api.campus-table.kro.kr
- API 도메인의 DNS와 인증서는 인프라 담당자가 준비한다. 기존 도메인은 새 API 주소 검증 후 변경한다.
- 프론트 팀은 API 기본 주소를 위 API 도메인으로 설정하고 credentials: include를 유지한다.
- SPA 새로고침을 위한 index.html fallback은 정적 호스팅에서 설정한다.

## 백엔드 환경변수 적용

두 서버의 기존 /etc/campus-table.env에 아래 세 항목만 적용한다. DB·Redis·키는 유지한다.

```dotenv
CORS_ALLOWED_ORIGINS=https://campus-table.kro.kr,https://www.campus-table.kro.kr
SPRING_PROFILES_ACTIVE=prod
COOKIE_SECURE=true
```

```bash
# 실제 서버 주소로 변경. 이 명령은 서버 설정 변경 및 재시작을 수행한다.
BASTION=사용자@bastion bash deploy/apply-web-settings.sh 사용자@서버1 사용자@서버2
```

스크립트는 환경변수 파일을 권한 600으로 백업하고 위 세 항목을 바꾼 뒤 한 서버씩 재시작·헬스체크한다. 실패하면 중단한다. 복구는 출력된 백업을 원래 파일로 복사하고 재시작한다. 실제 운영 서버에서 아직 검증하지 않았다.

## 대안: 기존 앱 서버의 Nginx로 프론트 제공

아래 절차와 nginx/campus-table.conf는 별도 정적 호스팅을 사용하지 않을 때만 적용한다. 현재 선택한 방식에는 ALB 포트 전환이나 Nginx 설치가 필요하지 않다.


## 로컬 빌드 및 API 주소

프론트 저장소: https://github.com/Campus-Table/frontend.git
현재 로컬 checkout: `/Users/iseolin-ui/Documents/backend/.local/frontend`

```bash
bash deploy/build-frontend.sh /Users/iseolin-ui/Documents/backend/.local/frontend
```

빌드 결과는 프론트 저장소의 `dist/`다. 운영 API는 기존 `src/api/client.js`의
상대 경로 `/api/...`와 `credentials: include`를 그대로 사용한다.
백엔드 비밀 환경변수는 프론트에 복사하지 않는다.
로컬 `npm run dev`는 `vite.config.js`에서 `/api`를 `127.0.0.1:8080`으로 프록시한다.
로컬 테스트에는 로컬 백엔드가 필요하다. Vite 개발 서버를 운영 서버로 사용하지 않는다.

## 서버 설치 (두 서버에서 동일하게 수행)

Ubuntu/Debian 예시다. 서버에 JAR와 환경변수가 이미 설치된 상태를 전제로 한다.
먼저 프론트 dist를 tar로 묶어서 서버에 복사한다. 접속 대상은 실제 값으로 변경한다.

```bash
tar -czf /tmp/campus-table-frontend.tar.gz -C /Users/iseolin-ui/Documents/backend/.local/frontend/dist .
scp -J 사용자@bastion주소 /tmp/campus-table-frontend.tar.gz 사용자@앱서버주소:/tmp/
scp -J 사용자@bastion주소 deploy/nginx/campus-table.conf 사용자@앱서버주소:/tmp/
```

서버에서 기존 프론트와 Nginx 설정이 있다면 먼저 백업한다.

```bash
sudo apt update
sudo apt install -y nginx
sudo install -d -m 755 /var/www/campus-table
sudo tar -xzf /tmp/campus-table-frontend.tar.gz -C /var/www/campus-table
sudo chmod -R a+rX /var/www/campus-table
sudo install -m 644 /tmp/campus-table.conf /etc/nginx/conf.d/campus-table.conf
sudo nginx -t
sudo systemctl enable nginx
sudo systemctl reload nginx
curl -fsS -H 'Host: www.campus-table.kro.kr' http://127.0.0.1/
curl -fsS -H 'Host: www.campus-table.kro.kr' http://127.0.0.1/api/health
```

다른 가상 호스트가 있다면 충돌 여부를 확인한다.
SPA 경로 새로고침은 index.html로 연결되며, `/api/`는 원래 경로를 유지하여
Spring으로 전달한다. `/test-charge.html`도 기존 Spring 화면으로 전달한다.
업로드 요청 상한은 3MB, 백엔드의 이미지 파일 상한은 2MB다.
HTTPS는 ALB에서 종료한다. Nginx는 ALB의 X-Forwarded-Proto를 유지한다.

## ALB / ACG 전환 (인프라 콘솔 작업)

1. 두 서버에서 Nginx와 `/api/health`가 정상인지 먼저 확인한다.
2. HTTP 포트 80 대상 그룹에 두 앱 서버를 등록한다.
3. 헬스체크: HTTP, 포트 80, GET `/api/health`, 성공 코드 200.
4. 앱 서버 ACG/NACL은 ALB 서브넷에서 포트 80 접근을 허용한다.
   Nginx가 전달 헤더를 사용하므로 인터넷에서 서버 80 직접 접근은 허용하지 않는다.
5. 두 대상 모두 Healthy인 후 HTTPS 443 리스너를 새 대상 그룹으로 연결한다.
   기존 인증서는 유지한다. HTTP 80 리스너는 HTTPS 리다이렉트한다.
6. 외부에서 화면, `/api/health`, 로그인 및 조회, SPA 새로고침을 확인한다.
   이상이 있으면 리스너를 기존 8080 대상 그룹으로 되돌린다.

두 서버는 동일한 프론트/백엔드 버전과 공용 DB/Redis를 사용해야 한다.
ALB의 인증서를 Nginx에 다시 복사할 필요는 없다.

## 현재 프론트 기능 범위

관리자 API 클라이언트는 구현되어 있지만 로그인 페이지는 onLogin 콜백을 호출하는
임시 화면이고, 학생 화면 일부는 mockData를 사용한다.
이 배포 작업은 API 통신 경로를 준비하는 작업이다. 로그인/학생 화면의 실제 API
연동은 별도 구현이 필요하다.

공식 참고:
- https://vite.dev/guide/build
- https://nginx.org/en/docs/http/ngx_http_proxy_module.html
- https://guide.ncloud-docs.com/docs/loadbalancer-application-vpc
