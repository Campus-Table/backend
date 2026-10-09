package com.campustable.campus_table.service;

import com.campustable.campus_table.repository.OrderRepository;
import java.time.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryJob {
    private final OrderRepository orders;
    private final OrderService service;
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void expire() {
        var now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        for (Long id : orders.findExpiredPaidIds(now.minusHours(1), PageRequest.of(0, 100))) {
            try { service.expireOrder(id, now); }
            catch (RuntimeException e) { log.error("Order expiry failed for {}", id, e); }
        }
    }
}
