package com.campustable.campus_table.controller;

import com.campustable.campus_table.config.AuthUser;
import com.campustable.campus_table.dto.MileageDtos.BalanceResponse;
import com.campustable.campus_table.dto.MileageDtos.ChargeConfirmRequest;
import com.campustable.campus_table.dto.MileageDtos.TransactionResponse;
import com.campustable.campus_table.service.MileageChargeService;
import com.campustable.campus_table.service.MileageService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mileage")
@RequiredArgsConstructor
public class MileageController {

    private final MileageService mileageService;
    private final MileageChargeService chargeService;

    @GetMapping
    public BalanceResponse balance(@AuthenticationPrincipal AuthUser user) {
        return mileageService.balance(user.userId());
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> transactions(@AuthenticationPrincipal AuthUser user) {
        return mileageService.transactions(user.userId());
    }

    @PostMapping("/charge/prepare")
    public com.campustable.campus_table.dto.MileageDtos.ChargePrepareResponse prepare(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody com.campustable.campus_table.dto.MileageDtos.ChargePrepareRequest req) {
        return chargeService.prepare(user.userId(), req.amount());
    }

    @PostMapping("/charge/confirm")
    public BalanceResponse confirmCharge(@AuthenticationPrincipal AuthUser user,
                                         @Valid @RequestBody ChargeConfirmRequest req) {
        return chargeService.charge(user.userId(), req);
    }
}
