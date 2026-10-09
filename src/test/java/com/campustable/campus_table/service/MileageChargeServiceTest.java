package com.campustable.campus_table.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.dto.MileageDtos.ChargeConfirmRequest;
import com.campustable.campus_table.entity.*;
import com.campustable.campus_table.repository.MileageChargeRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MileageChargeServiceTest {
    @Test void validatesOwnerAndAmountAndDoesNotCreditTwice() {
        var charges = mock(MileageChargeRepository.class);
        var toss = mock(TossPaymentClient.class);
        var mileage = mock(MileageService.class);
        var service = new MileageChargeService(charges, toss, mileage, mock(NotificationService.class));
        ReflectionTestUtils.setField(service, "clientKey", "test_ck_example");
        ReflectionTestUtils.setField(service, "secretKey", "test_sk_example");
        var charge = new MileageCharge("order-id", 1L, 10000);
        when(charges.findForUpdate("order-id")).thenReturn(Optional.of(charge));
        var request = new ChargeConfirmRequest("payment-key", "order-id", 10000);
        assertThrows(CustomException.class, () -> service.charge(2L, request));
        assertThrows(CustomException.class, () -> service.charge(1L, new ChargeConfirmRequest("payment-key", "order-id", 20000)));
        verifyNoInteractions(toss, mileage);
        when(mileage.apply(1L, MileageType.CHARGE, 10000, null)).thenReturn(10000);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            assertEquals(10000, service.charge(1L, request).balance());
            assertEquals(10000, service.charge(1L, request).balance());
        } finally { org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization(); }
        verify(toss, times(1)).confirm("payment-key", "order-id", 10000);
        verify(mileage, times(1)).apply(1L, MileageType.CHARGE, 10000, null);
    }
}
