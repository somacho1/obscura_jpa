package dev.jpa.obscura_jpa.refund;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import dev.jpa.obscura_jpa.payment.TossPaymentClient;
import dev.jpa.obscura_jpa.refund.RefundStore.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundService {
    private final RefundStore store;
    private final TossPaymentClient toss;

    public Info request(Long orderNo, Long memberNo, RefundRequest request) {
        if (request == null) throw new IllegalArgumentException("환불 요청이 필요합니다.");
        String normalizedKey;
        try { normalizedKey = UUID.fromString(request.requestKey()).toString(); }
        catch (RuntimeException e) { throw new IllegalArgumentException("올바른 환불 요청키가 필요합니다."); }
        // 이미 저장된 키는 외부 조회 없이 기존 요청 검증으로 재사용합니다.
        if (!store.hasKey(normalizedKey)) {
            var quote = store.quote(orderNo, memberNo, request);
            if ("TOSS".equals(quote.method())) {
                if (quote.paymentKey() == null) throw new IllegalStateException("Toss 결제정보가 없습니다.");
                var remote = toss.findPayment(quote.paymentKey());
                if (!Objects.equals(remote.get("paymentKey"), quote.paymentKey())
                    || !Objects.equals(remote.get("orderId"), quote.orderId())
                    || number(remote.get("totalAmount")) != quote.originalAmount()
                    || number(remote.get("balanceAmount")) != quote.balance()
                    || !"KRW".equals(remote.get("currency"))
                    || !List.of("DONE", "PARTIAL_CANCELED").contains(remote.get("status"))) {
                    throw new IllegalStateException("Toss 결제 상태와 저장된 주문을 확인해주세요.");
                }
                if (!List.of("카드", "간편결제").contains(remote.get("method"))) {
                    throw new IllegalArgumentException("카드·간편결제만 지원합니다.");
                }
                if (!quote.allRemaining() && !Boolean.TRUE.equals(remote.get("isPartialCancelable"))) {
                    throw new IllegalArgumentException("이 결제는 부분취소를 지원하지 않습니다. 전체취소를 선택해주세요.");
                }
            }
        }
        // 외부 조회 후 실제 저장은 주문 잠금 아래에서 다시 검증합니다.
        return store.request(orderNo, memberNo, request);
    }

    // 외부 통신은 DB 트랜잭션 밖에서 실행합니다.
    public Info execute(Long no) {
        Prepared prepared = store.prepared(no);
        Info info = prepared.info();
        if (info.statusNo() == 1) return info;
        if ("BANK".equals(info.method())) return store.complete(prepared);
        Map<String, Object> result = toss.findPayment(prepared.paymentKey());
        validateIdentity(prepared, result);
        // 외부 취소 내역에서도 이번 환불번호를 식별할 수 있도록 사유에 함께 기록합니다.
        String tossReason = info.reason() + " (환불 #" + info.no() + ")";
        long before = info.beforeAmount();
        long after = Math.subtractExact(before, info.amount());
        long balance = number(result.get("balanceAmount"));
        if (balance == before) {
            if (!List.of("DONE", "PARTIAL_CANCELED").contains(result.get("status"))) {
                throw new IllegalStateException("현재 Toss 결제 상태에서는 환불할 수 없습니다.");
            }
            if (!List.of("카드", "간편결제").contains(result.get("method"))) {
                throw new IllegalStateException("카드·간편결제만 지원합니다.");
            }
            if (after > 0 && !Boolean.TRUE.equals(result.get("isPartialCancelable"))) {
                throw new IllegalStateException("이 결제는 부분취소를 지원하지 않습니다.");
            }
            // Toss 멱등키는 15일간 유효하므로 오래된 미확정 요청을 새 취소로 보내지 않습니다.
            if (info.cdate().isBefore(LocalDateTime.now().minusDays(14))) {
                throw new IllegalStateException("오래된 환불 요청입니다. Toss 거래내역을 확인해주세요.");
            }
            validateCanceledTotal(prepared, result, before);
            toss.cancelPayment(prepared.paymentKey(), tossReason, info.requestKey(), info.amount());
            result = toss.findPayment(prepared.paymentKey());
            validateIdentity(prepared, result);
        } else if (balance != after) {
            throw new IllegalStateException("Toss 잔액이 환불 요청과 다릅니다. 거래내역을 확인해주세요.");
        }
        // 잔액 전체를 여러 번 나누어 취소하면 잔액 0에도 PARTIAL_CANCELED가 올 수 있습니다.
        // 잔액과 아래의 누적 취소금액·이번 취소 거래 검증을 모두 통과해야 완료합니다.
        String actualStatus = String.valueOf(result.get("status"));
        boolean validStatus = after == 0
            ? List.of("CANCELED", "PARTIAL_CANCELED").contains(actualStatus)
            : "PARTIAL_CANCELED".equals(actualStatus);
        if (number(result.get("balanceAmount")) != after || !validStatus) {
            throw new IllegalStateException("환불 완료가 확인되지 않았습니다. 같은 요청으로 다시 확인해주세요.");
        }
        // 외부 성공 후 DB 저장에 실패해도 잔액과 완료된 거래를 확인하고 한 번만 복구합니다.
        validateCanceledTotal(prepared, result, after);
        if (!(result.get("cancels") instanceof List<?> cancels)
            || cancels.stream().noneMatch(value -> value instanceof Map<?, ?> cancel
                && tossReason.equals(cancel.get("cancelReason"))
                && "DONE".equals(cancel.get("cancelStatus"))
                && number(cancel.get("cancelAmount")) == info.amount())) {
            throw new IllegalStateException("이번 환불 요청과 일치하는 Toss 취소 거래가 없습니다.");
        }
        return store.complete(prepared);
    }
    private void validateIdentity(Prepared prepared, Map<String, Object> result) {
        if (result == null || !Objects.equals(prepared.paymentKey(), result.get("paymentKey"))
            || !Objects.equals(prepared.tossOrderId(), result.get("orderId"))
            || number(result.get("totalAmount")) != prepared.originalAmount()
            || !"KRW".equals(result.get("currency"))) {
            throw new IllegalStateException("Toss 결제정보가 주문과 일치하지 않습니다.");
        }
    }
    private void validateCanceledTotal(Prepared prepared, Map<String, Object> result, long balance) {
        long expected = Math.subtractExact(prepared.originalAmount(), balance);
        Object raw = result.get("cancels");
        long sum = 0;
        if (raw != null) {
            if (!(raw instanceof List<?> list)) throw new IllegalStateException("Toss 취소 내역 형식이 다릅니다.");
            for (Object value : list) {
                if (!(value instanceof Map<?, ?> cancel) || !"DONE".equals(cancel.get("cancelStatus"))) {
                    throw new IllegalStateException("완료되지 않은 Toss 취소 거래가 있습니다.");
                }
                long amount = number(cancel.get("cancelAmount"));
                if (amount <= 0) throw new IllegalStateException("Toss 취소 금액이 올바르지 않습니다.");
                sum = Math.addExact(sum, amount);
            }
        }
        if (sum != expected) throw new IllegalStateException("Toss 누적 취소 금액이 일치하지 않습니다.");
    }
    private long number(Object value) {
        try {
            if (!(value instanceof Number)) throw new NumberFormatException();
            return new BigDecimal(value.toString()).longValueExact();
        } catch (RuntimeException e) { throw new IllegalStateException("Toss 응답 금액을 확인할 수 없습니다."); }
    }
}
