package com.campustable.campus_table.controller;

import com.campustable.campus_table.config.AuthUser;
import com.campustable.campus_table.dto.OrderDtos.ArrivalRequest;
import com.campustable.campus_table.dto.OrderDtos.CreateRequest;
import com.campustable.campus_table.dto.OrderDtos.OrderResponse;
import com.campustable.campus_table.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CreateRequest req) {
        return orderService.create(user.userId(), req);
    }

    @GetMapping("/me")
    public List<OrderResponse> myOrders(@AuthenticationPrincipal AuthUser user) {
        return orderService.myOrders(user.userId());
    }

    /** 진행 중인 주문이 있으면 200, 없으면 204. */
    @GetMapping("/me/current")
    public ResponseEntity<OrderResponse> current(@AuthenticationPrincipal AuthUser user) {
        return orderService.current(user.userId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable Long orderId) {
        return orderService.get(user.userId(), orderId);
    }

    @PostMapping("/{orderId}/arrival")
    public OrderResponse arrive(@AuthenticationPrincipal AuthUser user, @PathVariable Long orderId,
                                @Valid @RequestBody ArrivalRequest req) {
        return orderService.arrive(user.userId(), orderId, req.code());
    }

    @PostMapping("/{orderId}/receive")
    public OrderResponse receive(@AuthenticationPrincipal AuthUser user, @PathVariable Long orderId) {
        return orderService.receive(user.userId(), orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal AuthUser user, @PathVariable Long orderId) {
        return orderService.cancel(user.userId(), orderId);
    }
}
