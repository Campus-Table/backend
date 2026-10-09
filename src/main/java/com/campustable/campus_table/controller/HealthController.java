package com.campustable.campus_table.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** ALB 헬스체크용. 인증 없이 200만 반환한다(프로세스가 요청을 받는지만 확인, DB/Redis는 보지 않음). */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
