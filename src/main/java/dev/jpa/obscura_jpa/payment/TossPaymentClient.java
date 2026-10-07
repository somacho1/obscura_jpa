package dev.jpa.obscura_jpa.payment;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

// Toss 외부 API 호출만 담당합니다. 주문 검증과 DB 저장은 Service에서 처리합니다.
@Component
public class TossPaymentClient {

    private final RestClient restClient;

    public TossPaymentClient(@Value("${toss.secret-key}") String secretKey) {
        // 현재 프로젝트는 테스트 결제만 사용합니다.
        if (secretKey == null || !secretKey.trim().startsWith("test_sk")) {
            throw new IllegalStateException("TOSS_SECRET_KEY에 테스트 시크릿 키를 설정해주세요.");
        }

        // 외부 서버 연결과 응답에 제한 시간을 설정합니다.
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
            .baseUrl("https://api.tosspayments.com")
            .requestFactory(factory)
            // 시크릿 키를 사용자명, 빈 문자열을 비밀번호로 사용하는 Basic 인증입니다.
            .defaultHeaders(headers -> headers.setBasicAuth(secretKey.trim(), ""))
            .build();
    }

    // Service에서 검증한 승인 정보를 Toss로 보냅니다.
    // 같은 승인 요청을 재시도할 때는 동일한 idempotencyKey를 사용해야 합니다.
    public Map<String, Object> confirm(
        String paymentKey,
        String orderId,
        Long amount,
        String idempotencyKey
    ) {
        try {
            Map<String, Object> result = restClient.post()
                .uri("/v1/payments/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(Map.of(
                    "paymentKey", paymentKey,
                    "orderId", orderId,
                    "amount", amount
                ))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (result == null) {
                throw new IllegalStateException("결제 승인 응답이 없습니다. 결제 상태를 다시 확인해주세요.");
            }

            return result;
        } catch (RestClientResponseException e) {
            // 키나 외부 응답 전체를 화면에 노출하지 않습니다.
            // 이 오류만으로 주문을 자동 취소하거나 재고를 복구하지 않습니다.
            throw new IllegalStateException(
                "Toss 승인 요청에서 오류가 발생했습니다. 결제 상태를 확인해주세요. ("
                + e.getStatusCode().value() + ")"
            );
        } catch (ResourceAccessException e) {
            // 응답을 못 받았어도 Toss에서는 승인됐을 수 있으므로 실패로 단정하지 않습니다.
            throw new IllegalStateException(
                "결제 승인 결과를 확인하지 못했습니다. 주문을 취소하지 말고 결제 상태를 다시 확인해주세요."
            );
        }
    }
    
 // 결제 조회: 취소 응답을 놓친 경우에도 현재 상태를 확인할 수 있습니다.
    public Map<String, Object> findPayment(String paymentKey) {
        try {
            Map<String, Object> result = restClient.get()
                .uri("/v1/payments/{paymentKey}", paymentKey)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (result == null) {
                throw new IllegalStateException("Toss 결제 조회 응답이 없습니다.");
            }
            return result;
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                "Toss 결제 조회에 실패했습니다. (" + e.getStatusCode().value() + ")"
            );
        } catch (ResourceAccessException e) {
            throw new IllegalStateException("Toss 결제 상태를 확인하지 못했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    // 전체 취소: cancelAmount를 생략해 전액 취소합니다.
    public Map<String, Object> cancelPayment(
        String paymentKey,
        String reason,
        String cancelKey
    ) {
        try {
            Map<String, Object> result = restClient.post()
                .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", cancelKey)
                .body(Map.of("cancelReason", reason))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (result == null) {
                throw new IllegalStateException("취소 결과를 확인하지 못했습니다. 같은 주문으로 다시 시도해주세요.");
            }
            return result;
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                "Toss 취소 요청 결과를 확인해주세요. 같은 주문으로 재시도할 수 있습니다. ("
                + e.getStatusCode().value() + ")"
            );
        } catch (ResourceAccessException e) {
            // 통신 실패여도 Toss에서 취소됐을 수 있으므로 재고를 바로 복구하지 않습니다.
            throw new IllegalStateException(
                "취소 결과를 확인하지 못했습니다. 출고는 보류되며 같은 주문으로 다시 시도해주세요."
            );
        }
    }
}