package com.campustable.campus_table.service;

import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.common.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** 토스페이먼츠 결제 승인 API 호출. */
@Slf4j
@Component
public class TossPaymentClient {

    private final RestClient client = RestClient.builder().baseUrl("https://api.tosspayments.com")
            .requestFactory(requestFactory()).build();
    private final String authHeader;

    private static org.springframework.http.client.JdkClientHttpRequestFactory requestFactory() {
        var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build());
        factory.setReadTimeout(java.time.Duration.ofSeconds(30));
        return factory;
    }

    public TossPaymentClient(@Value("${app.toss.secret-key:}") String secretKey) {
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    public void confirm(String paymentKey, String orderId, int amount) {
        try {
            client.post().uri("/v1/payments/confirm")
                    .header("Authorization", authHeader)
                    .header("Idempotency-Key", orderId)
                    .body(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("토스 결제 승인 실패 {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        } catch (RestClientException e) {
            log.error("토스 호출 오류", e);
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }
    }
}
