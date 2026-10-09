#!/usr/bin/env python3
"""Campus Table 시연 시뮬레이터 (외부 클라이언트, 표준 라이브러리만 사용).

가상 인물이 실제 API로 주문 -> 도착 인증 -> 조리 -> (자리가 나면) 식사를 하고,
주기적으로 '앉아서 먹는 인원'을 인원 스냅샷(SIMULATION)으로 서버에 보낸다.
시연자가 화면에서 한 실제 주문도 같은 인원/대기열에 합산한다. 사용법은 demo/README.md.
"""
import argparse, collections, http.cookiejar, json, math, os, random, signal, sys, threading, time
import urllib.error, urllib.request
from datetime import datetime, timedelta, timezone

KST = timezone(timedelta(hours=9))
STATE_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".state.json")

# (경과 분, 목표 이용률). 사이는 직선 보간. hold=True면 마지막 값을 계속 유지(Ctrl+C로 종료)
PRESETS = {
    "lunch": dict(points=[(0, .05), (5, .35), (12, .75), (20, .90), (32, .92), (40, .8), (50, .45), (58, .15), (65, .05)], hold=False),
    "quiet": dict(points=[(0, .08), (10, .25), (20, .18)], hold=True),
    "overflow": dict(points=[(0, .10), (6, 1.2)], hold=True),  # 이용률 100% 초과: 자리 대기 시연
    "packed": dict(points=[(0, .10), (8, .90)], hold=True),
}


def now_kst():
    return datetime.now(KST).replace(tzinfo=None)


def log(msg):
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def call(self, method, path, body=None, retries=3):
        data = json.dumps(body).encode() if body is not None else None
        for attempt in range(retries):
            req = urllib.request.Request(self.base + path, data=data, method=method,
                                         headers={"Content-Type": "application/json"})
            try:
                with self.opener.open(req, timeout=30) as r:
                    raw = r.read()
                    return r.status, (json.loads(raw) if raw else None)
            except urllib.error.HTTPError as e:
                raw = e.read()
                try:
                    payload = json.loads(raw) if raw else None
                except ValueError:
                    payload = None
                if e.code == 429 and attempt < retries - 1:
                    time.sleep(min(int(e.headers.get("Retry-After", "2")), 10))
                    continue
                return e.code, payload
            except (urllib.error.URLError, TimeoutError) as e:
                if attempt == retries - 1:
                    return 0, {"code": "NETWORK", "message": str(e)}
                time.sleep(1)


def code_of(payload):
    return (payload or {}).get("code", "")


class Sim:
    def __init__(self, args):
        self.a = args
        self.rng = random.Random(args.seed)
        self.lock = threading.Lock()
        self.stop = threading.Event()
        self.admin = Api(args.target)
        self.user_password = os.environ.get("DEMO_USER_PASSWORD", "Demo!2345")
        self.arrival_code = None
        self.arrival_code_date = None
        # 공유 상태 (lock 보호)
        self.in_flight = 0                    # 주문~조리 중
        self.store_ahead = collections.Counter()  # 가게별 조리 대기 인원(가게 선택에 사용)
        self.queue = collections.deque()      # 음식은 나왔지만 자리를 기다리는 사람 (끝나는 시간 계산용 eat_real)
        self.seated = []                      # 앉아 먹는 사람의 식사 종료 시각(epoch)
        self.real_eaters = 0
        self.busy_until = {}                  # 학번 -> epoch (이 시각까지 사용 불가)
        self.sessions = {}                    # 학번 -> Api
        self.stats = collections.Counter()

    # ---------- 준비 ----------
    def setup(self):
        a = self.a
        sn, pw = os.environ.get("DEMO_ADMIN_SN"), os.environ.get("DEMO_ADMIN_PASSWORD")
        if not sn or not pw:
            sys.exit("환경변수 DEMO_ADMIN_SN, DEMO_ADMIN_PASSWORD(관리자 계정)가 필요합니다.")
        st, p = self.admin.call("POST", "/api/auth/login", {"studentNumber": sn, "password": pw})
        if st != 200 or p.get("role") != "ADMIN":
            sys.exit(f"관리자 로그인 실패: {st} {code_of(p)}")
        st, cafes = self.admin.call("GET", "/api/cafeterias")
        if st != 200 or not cafes:
            sys.exit("식당이 없습니다. 시드 데이터를 먼저 넣어 주세요(SEED_ENABLED=true 또는 seed.sql).")
        self.cafe = cafes[0]
        self.seats, self.dining = self.cafe["seatCount"], self.cafe["diningMinutes"]
        st, stores = self.admin.call("GET", f"/api/cafeterias/{self.cafe['id']}/stores")
        self.stores = []
        for s in stores:
            st, menus = self.admin.call("GET", f"/api/stores/{s['id']}/menus")
            menus = [m for m in menus if m["available"]]
            if menus:
                self.stores.append(dict(id=s["id"], name=s["name"], avg=s["avgWaitMinutes"], menus=menus,
                                        weight=0.3 + self.rng.random() ** 2 * 2))
        # 10,000명의 가상 프로필 (시드로 재현 가능)
        self.profiles = [dict(store=self.rng.choices(range(len(self.stores)), [s["weight"] for s in self.stores])[0],
                              eat_min=min(45, max(8, self.rng.lognormvariate(math.log(a.eat_median), 0.3))),
                              extra=self.rng.random() < 0.35, noshow=self.rng.random() < a.noshow)
                         for _ in range(10000)]
        self.next_profile = 0
        self.accounts = [f"99{i:06d}" for i in range(1, a.pool + 1)]
        self.mean_eat = sum(p["eat_min"] for p in self.profiles) / len(self.profiles)
        if a.avg_wait > 0:
            self.set_avg_wait(a.avg_wait)
        self.refresh_arrival_code()
        log(f"식당 {self.cafe['name']} 좌석 {self.seats}, 가게 {len(self.stores)}개, 계정 풀 {len(self.accounts)}, "
            f"시드 {a.seed}, 프리셋 {a.preset}")

    def set_avg_wait(self, minutes):
        saved = json.load(open(STATE_FILE)) if os.path.exists(STATE_FILE) else {}
        for s in self.stores:
            saved.setdefault(str(s["id"]), s["avg"])  # 이전 실행의 원래 값을 보존
        json.dump(saved, open(STATE_FILE, "w"))
        for s in self.stores:
            self.admin.call("PATCH", f"/api/admin/stores/{s['id']}", {"avgWaitMinutes": minutes})
        log(f"가게 평균 대기시간을 {minutes}분으로 낮춤 (종료 시 복원)")

    def restore_avg_wait(self):
        if not os.path.exists(STATE_FILE):
            return
        for sid, avg in json.load(open(STATE_FILE)).items():
            st, p = self.admin.call("PATCH", f"/api/admin/stores/{sid}", {"avgWaitMinutes": avg})
            if st != 200:
                log(f"가게 {sid} 평균 대기시간 복원 실패 {st} {code_of(p)}")
        os.remove(STATE_FILE)
        log("가게 평균 대기시간을 원래 값으로 복원함")

    def refresh_arrival_code(self):
        st, p = self.admin.call("GET", f"/api/admin/cafeterias/{self.cafe['id']}/arrival-code")
        if st == 200:
            self.arrival_code, self.arrival_code_date = p["code"], p["date"]

    # ---------- 가상 인물 ----------
    def acquire_account(self):
        t = time.time()
        free = [sn for sn in self.accounts if self.busy_until.get(sn, 0) <= t]
        if not free:
            return None
        sn = self.rng.choice(free)
        self.busy_until[sn] = t + 3600 * 24  # 사용 중 (끝나면 release)
        return sn

    def release(self, sn, hold_seconds=0):
        with self.lock:
            self.busy_until[sn] = time.time() + hold_seconds

    def session(self, sn):
        api = self.sessions.get(sn)
        if api is None:
            api = Api(self.a.target)
            st, p = api.call("POST", "/api/auth/login", {"studentNumber": sn, "password": self.user_password})
            if st != 200:
                raise RuntimeError(f"로그인 실패 {sn}: {st} {code_of(p)} (demo/users.sql 실행 여부 확인)")
            self.sessions[sn] = api
        return api

    def pick_store(self):
        """가게 인기도 / (1 + 줄 길이): 사람들은 줄이 긴 가게를 피한다. lock 안에서 호출."""
        w = [s["weight"] / (1 + 0.6 * self.store_ahead[i]) for i, s in enumerate(self.stores)]
        i = self.rng.choices(range(len(self.stores)), w)[0]
        self.store_ahead[i] += 1
        return i

    def person(self, prof, sn, store_idx):
        S = 1.0
        store = self.stores[store_idx]
        try:
            api = self.session(sn)
            items = [{"menuId": self.rng.choice(store["menus"])["id"], "quantity": 1}]
            if prof["extra"]:
                extra = self.rng.choice(store["menus"])["id"]
                if extra != items[0]["menuId"]:
                    items.append({"menuId": extra, "quantity": 1})
            st, o = api.call("POST", "/api/orders", {"storeId": store["id"], "items": items})
            if st != 201:
                c = code_of(o)
                self.stats["order_" + c] += 1
                self.release(sn, 600 if c == "ACTIVE_ORDER_EXISTS" else 3600)
                with self.lock:
                    self.in_flight -= 1
                    self.store_ahead[store_idx] -= 1
                return
            self.stats["orders"] += 1
            if prof["noshow"]:  # 도착 인증을 하지 않음 -> 1시간 뒤 자동 취소/환불
                self.stats["noshow"] += 1
                self.release(sn, 3900)
                with self.lock:
                    self.in_flight -= 1
                    self.store_ahead[store_idx] -= 1
                return
            time.sleep(self.rng.uniform(5, 40) / S)
            for attempt in range(2):
                st, o2 = api.call("POST", f"/api/orders/{o['orderId']}/arrival", {"code": self.arrival_code})
                if code_of(o2) == "INVALID_ARRIVAL_CODE" and attempt == 0:
                    self.refresh_arrival_code()
                    continue
                break
            if st != 200:
                self.stats["arrival_" + code_of(o2)] += 1
                self.release(sn, 3900)
                with self.lock:
                    self.in_flight -= 1
                    self.store_ahead[store_idx] -= 1
                return
            time.sleep(max(0, o2.get("remainingSeconds") or 0) + 1)
            api.call("GET", f"/api/orders/{o['orderId']}")  # 조회 시점에 RECEIVED로 정리되고 알림이 생성됨
            self.release(sn)
            with self.lock:
                self.in_flight -= 1
                self.store_ahead[store_idx] -= 1
                self.queue.append(prof["eat_min"] * 60 / S)
            self.stats["served"] += 1
        except Exception as e:  # 한 사람의 실패가 전체를 멈추지 않게 한다
            self.stats["error"] += 1
            self.release(sn, 60)
            with self.lock:
                self.in_flight -= 1
                self.store_ahead[store_idx] -= 1
            log(f"가상 인물 오류: {e}")

    # ---------- 좌석/스냅샷 ----------
    def seat_step(self):
        t = time.time()
        with self.lock:
            self.seated = [e for e in self.seated if e > t]
            while self.queue and len(self.seated) + self.real_eaters < self.seats:
                self.seated.append(t + self.queue.popleft())

    def poll_real(self):
        """시연자가 실제로 한 주문: 음식이 나온 뒤 식사 시간 동안 앉아 있는 사람 수."""
        st, orders = self.admin.call("GET", f"/api/admin/orders?date={now_kst().date()}")
        if st != 200:
            return
        n, t = 0, now_kst()
        for o in orders:
            if o["studentNumber"].startswith("99") or o["status"] == "CANCELLED" or not o.get("expectedReadyAt"):
                continue
            out = datetime.fromisoformat(o["expectedReadyAt"][:19])
            if out <= t < out + timedelta(minutes=self.dining):
                n += 1
        with self.lock:
            self.real_eaters = n

    def snapshot_loop(self):
        while not self.stop.is_set():
            self.poll_real()
            self.seat_step()
            with self.lock:
                n = min(self.seats, len(self.seated) + self.real_eaters)
            st, p = self.admin.call("POST", f"/api/admin/cafeterias/{self.cafe['id']}/occupancy",
                                    {"currentPeople": n, "source": "SIMULATION"})
            if st == 200:
                self.stats["snapshots"] += 1
            else:
                log(f"스냅샷 전송 실패 {st} {code_of(p)}")
            self.stop.wait(self.a.interval)

    # ---------- 메인 루프 ----------
    def target(self, minutes):
        pr = PRESETS[self.a.preset]
        pts = pr["points"]
        if minutes >= pts[-1][0]:
            return pts[-1][1] if pr["hold"] else None
        for (t0, v0), (t1, v1) in zip(pts, pts[1:]):
            if t0 <= minutes <= t1:
                return v0 + (v1 - v0) * (minutes - t0) / (t1 - t0)

    def poisson(self, mean):
        limit, k, p = math.exp(-mean), 0, 1.0
        while True:
            p *= self.rng.random()
            if p <= limit:
                return k
            k += 1

    def run(self):
        self.setup()
        threading.Thread(target=self.snapshot_loop, daemon=True).start()
        S, tick, start, last_log = 1.0, 0.5, time.time(), 0
        # 서버는 가게당 평균 대기시간마다 1건씩 처리하므로 처리량 이상으로 도착시키면 줄이 끝없이 길어진다
        avg = self.a.avg_wait or (sum(s["avg"] for s in self.stores) / len(self.stores))
        cap_real = 0.85 * len(self.stores) * self.a.cooking_capacity / (avg * 60)
        mean_stay = self.mean_eat * 60 + 180  # 가상 초 (식사 + 조리 대기 추정)
        try:
            while not self.stop.is_set():
                self.seat_step()
                elapsed_min = (time.time() - start) * S / 60
                if self.a.minutes and elapsed_min >= self.a.minutes:
                    break
                tgt_frac = self.target(elapsed_min)
                with self.lock:
                    pipeline = self.in_flight + len(self.queue) + len(self.seated)
                if tgt_frac is None:  # 곡선 종료: 새 도착 없음, 모두 나가면 종료
                    if pipeline == 0:
                        break
                    lam = 0
                else:
                    tgt = tgt_frac * self.seats
                    lam = max(0.0, tgt / mean_stay + (tgt - pipeline) / 90)  # 가상 초당 도착
                lam_real = min(lam * S, min(self.a.max_rate * S, cap_real))
                for _ in range(self.poisson(lam_real * tick)):
                    with self.lock:
                        sn = self.acquire_account()
                        if sn:
                            self.in_flight += 1
                            idx = self.pick_store()
                    if not sn:
                        self.stats["pool_exhausted"] += 1
                        continue
                    prof = self.profiles[self.next_profile % len(self.profiles)]
                    self.next_profile += 1
                    threading.Thread(target=self.person, args=(prof, sn, idx), daemon=True).start()
                if time.time() - last_log >= 10:
                    last_log = time.time()
                    with self.lock:
                        log(f"+{elapsed_min:5.1f}분 목표 {0 if tgt_frac is None else tgt_frac:4.0%} | 앉음 {len(self.seated)}"
                            f"+실제 {self.real_eaters}/{self.seats} | 자리대기 {len(self.queue)} | 주문~조리 {self.in_flight}"
                            f" | 주문 {self.stats['orders']} no-show {self.stats['noshow']} 오류 {self.stats['error']}"
                            f" 풀부족 {self.stats['pool_exhausted']} 스냅샷 {self.stats['snapshots']}")
                time.sleep(tick)
        finally:
            self.stop.set()
            if self.a.avg_wait > 0:
                self.restore_avg_wait()
            log("종료. 스냅샷 전송을 멈췄으므로 5분 뒤 혼잡도는 주문 기반 값으로 돌아갑니다.")


def main():
    ap = argparse.ArgumentParser(description="Campus Table 시연 시뮬레이터")
    ap.add_argument("--target", default="http://localhost:8080", help="서버 주소")
    ap.add_argument("--preset", choices=PRESETS, default="lunch")
    ap.add_argument("--seed", type=int, default=None, help="같은 시나리오 재생용 시드(없으면 무작위)")
    ap.add_argument("--minutes", type=float, default=0, help="이 시간(가상 분) 뒤 종료. 0이면 프리셋 기준")
    ap.add_argument("--avg-wait", type=int, default=1, help="시연 중 가게 평균 대기시간(분). 0이면 건드리지 않음")
    ap.add_argument("--interval", type=float, default=20, help="스냅샷 전송 간격(초)")
    ap.add_argument("--pool", type=int, default=300, help="사용할 시연 계정 수(최대 300)")
    ap.add_argument("--eat-median", type=float, default=25, help="식사 시간 중앙값(가상 분). 늘리면 같은 도착 속도에서 인원이 더 쌓임")
    ap.add_argument("--cooking-capacity", type=int, default=3, help="가게당 동시 조리 수(서버 stores.cooking_capacity, 시드 기본 3). 도착 속도 상한 계산에 사용")
    ap.add_argument("--noshow", type=float, default=0.05, help="도착 인증을 안 하는 비율")
    ap.add_argument("--max-rate", type=float, default=4 / 30, help="가상 초당 최대 도착 인원(기본 30초에 4명)")
    ap.add_argument("--restore", action="store_true", help="비정상 종료 후 가게 평균 대기시간만 복원하고 끝냄")
    a = ap.parse_args()
    a.seed = a.seed if a.seed is not None else random.randrange(1 << 30)
    sim = Sim(a)
    if a.restore:
        sn, pw = os.environ.get("DEMO_ADMIN_SN"), os.environ.get("DEMO_ADMIN_PASSWORD")
        sim.admin.call("POST", "/api/auth/login", {"studentNumber": sn, "password": pw})
        sim.restore_avg_wait()
        return
    signal.signal(signal.SIGINT, lambda *_: sim.stop.set())
    sim.run()


if __name__ == "__main__":
    main()
