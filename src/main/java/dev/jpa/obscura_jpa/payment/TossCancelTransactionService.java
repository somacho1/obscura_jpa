package dev.jpa.obscura_jpa.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jpa.obscura_jpa.delivery.DeliveryRepository;
import dev.jpa.obscura_jpa.order.Order;
import dev.jpa.obscura_jpa.order.OrderRepository;
import dev.jpa.obscura_jpa.orderitem.OrderItem;
import dev.jpa.obscura_jpa.orderitem.OrderItemRepository;
import dev.jpa.obscura_jpa.stock.Stock;
import dev.jpa.obscura_jpa.stock.StockRepository;

@Service
public class TossCancelTransactionService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockRepository stockRepository;
    private final DeliveryRepository deliveryRepository;

    public TossCancelTransactionService(
        OrderRepository orderRepository,
        PaymentRepository paymentRepository,
        OrderItemRepository orderItemRepository,
        StockRepository stockRepository,
        DeliveryRepository deliveryRepository
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderItemRepository = orderItemRepository;
        this.stockRepository = stockRepository;
        this.deliveryRepository = deliveryRepository;
    }

    // 외부 호출에는 Entity 대신 저장된 요청값만 전달합니다.
    public record PreparedCancel(
        Long orderNo,
        String paymentKey,
        String orderId,
        Long amount,
        String cancelKey,
        String reason,
        boolean completed
    ) {
    }

    @Transactional
    public PreparedCancel prepare(TossCancelRequest request) {
        if (request == null || request.orderNo() == null || request.orderNo() <= 0) {
            throw new IllegalArgumentException("올바른 주문번호가 필요합니다.");
        }
        if (request.mno() == null || request.mno() <= 0) {
            throw new IllegalArgumentException("회원번호가 필요합니다.");
        }

        Order order = orderRepository.findByNoForUpdate(request.orderNo())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if (!order.getMember().getNo().equals(request.mno())) {
            throw new IllegalArgumentException("본인의 주문만 취소할 수 있습니다.");
        }

        Payment payment = paymentRepository.findByOrderNo(order.getNo())
            .orElseThrow(() -> new IllegalArgumentException("결제정보가 없습니다."));

        if (!"TOSS".equals(payment.getMethod())
            || payment.getPaymentKey() == null
            || payment.getTossOrderId() == null) {
            throw new IllegalArgumentException("Toss 결제 주문만 처리할 수 있습니다.");
        }
        if (!Objects.equals(order.getTotalPrice(), payment.getAmount())) {
            throw new IllegalStateException("주문금액과 결제금액이 다릅니다.");
        }

        // 이 기능으로 이미 취소한 주문은 그대로 반환합니다.
        if (Integer.valueOf(0).equals(order.getStatusNo())
            && Integer.valueOf(2).equals(order.getCancelStatusNo())
            && Integer.valueOf(3).equals(payment.getStatusNo())
            && payment.getCancelKey() != null) {
            return toPrepared(payment, true);
        }

        validateBeforeShipping(order);

        if (!Integer.valueOf(1).equals(payment.getStatusNo())) {
            throw new IllegalArgumentException("결제 완료 주문만 취소할 수 있습니다.");
        }

        List<OrderItem> items = orderItemRepository.findAllByOrderNoOrderByNoAsc(order.getNo());
        validateItems(items);

        if (payment.getCancelKey() == null) {
            if (!Integer.valueOf(0).equals(order.getCancelStatusNo())) {
                throw new IllegalStateException("기존 취소 상태를 확인해주세요.");
            }

            String reason = request.reason() == null ? "" : request.reason().trim();
            if (reason.isEmpty() || reason.length() > 200) {
                throw new IllegalArgumentException("취소 사유는 1~200자로 입력해주세요.");
            }

            payment.setCancelKey(UUID.randomUUID().toString());
            payment.setCancelReason(reason);
            order.setCancelStatusNo(3);
        } else {
            // 재시도에는 처음 저장한 사유와 멱등키를 그대로 사용합니다.
            if (!Integer.valueOf(3).equals(order.getCancelStatusNo())
                || payment.getCancelReason() == null) {
                throw new IllegalStateException("저장된 취소 요청을 확인해주세요.");
            }
        }

        paymentRepository.saveAndFlush(payment);
        return toPrepared(payment, false);
    }

    // 출고 처리도 같은 주문 잠금을 사용하므로 취소 준비와 순서대로 처리됩니다.
    private void validateBeforeShipping(Order order) {
        if (!Integer.valueOf(2).equals(order.getStatusNo())
            && !Integer.valueOf(3).equals(order.getStatusNo())) {
            throw new IllegalArgumentException("출고 전 주문만 전체 취소할 수 있습니다.");
        }

        boolean shipped = deliveryRepository.findAllByOrderNoOrderByNoAsc(order.getNo())
            .stream()
            .anyMatch(delivery ->
                !Integer.valueOf(0).equals(delivery.getStatusNo())
                || delivery.getShipDate() != null
            );

        if (shipped) {
            throw new IllegalArgumentException("이미 출고된 상품이 있습니다. 반품 절차가 필요합니다.");
        }
    }

    private void validateItems(List<OrderItem> items) {
        if (items.isEmpty()) {
            throw new IllegalStateException("주문상품 정보가 없습니다.");
        }

        for (OrderItem item : items) {
            long cancelled = item.getCancelQty() == null ? 0L : item.getCancelQty();

            if (item.getQty() == null || item.getQty() <= 0 || cancelled != 0) {
                throw new IllegalStateException("부분 취소 또는 잘못된 주문 수량을 확인해주세요.");
            }
        }
    }

    // 외부 결제 조회·취소 결과가 저장된 주문과 일치하는지 확인합니다.
    public void validateIdentity(PreparedCancel prepared, Map<String, Object> result) {
        if (result == null
            || !prepared.paymentKey().equals(result.get("paymentKey"))
            || !prepared.orderId().equals(result.get("orderId"))
            || number(result.get("totalAmount")) != prepared.amount().longValue()
            || !"KRW".equals(result.get("currency"))) {
            throw new IllegalStateException("Toss 결제 결과가 저장된 주문과 일치하지 않습니다.");
        }
    }

    private long number(Object value) {
        try {
            if (!(value instanceof Number)) throw new NumberFormatException();
            return new BigDecimal(value.toString()).longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalStateException("Toss 응답의 금액을 확인할 수 없습니다.");
        }
    }

    @Transactional
    public void complete(PreparedCancel prepared, Map<String, Object> result) {
        validateIdentity(prepared, result);

        if (!"CANCELED".equals(result.get("status"))
            || number(result.get("balanceAmount")) != 0L) {
            throw new IllegalStateException("전체 취소가 확인되지 않았습니다. 다시 시도해주세요.");
        }

        // 전체 취소 거래 금액과 실제 취소일시도 확인합니다.
        Object rawCancels = result.get("cancels");
        if (!(rawCancels instanceof List<?> cancels) || cancels.isEmpty()) {
            throw new IllegalStateException("Toss 취소 내역이 없습니다.");
        }

        long cancelledAmount = 0L;
        LocalDateTime cancelledAt = null;

        for (Object value : cancels) {
            if (!(value instanceof Map<?, ?> cancel)) {
                throw new IllegalStateException("Toss 취소 내역 형식이 올바르지 않습니다.");
            }
            long amount = number(cancel.get("cancelAmount"));
            if (amount <= 0) throw new IllegalStateException("취소 금액을 확인해주세요.");
            cancelledAmount = Math.addExact(cancelledAmount, amount);

            try {
                LocalDateTime date = OffsetDateTime.parse((String) cancel.get("canceledAt"))
                    .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                    .toLocalDateTime();
                if (cancelledAt == null || date.isAfter(cancelledAt)) cancelledAt = date;
            } catch (RuntimeException e) {
                throw new IllegalStateException("Toss 취소일시를 확인할 수 없습니다.");
            }
        }

        if (cancelledAmount != prepared.amount().longValue()) {
            throw new IllegalStateException("전체 취소 금액이 주문금액과 다릅니다.");
        }

        Order order = orderRepository.findByNoForUpdate(prepared.orderNo())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        Payment payment = paymentRepository.findByOrderNo(order.getNo())
            .orElseThrow(() -> new IllegalArgumentException("결제정보가 없습니다."));

        if (!Objects.equals(payment.getCancelKey(), prepared.cancelKey())
            || !Objects.equals(payment.getPaymentKey(), prepared.paymentKey())
            || !Objects.equals(payment.getTossOrderId(), prepared.orderId())
            || !Objects.equals(payment.getAmount(), prepared.amount())
            || !Objects.equals(order.getTotalPrice(), prepared.amount())) {
            throw new IllegalStateException("저장된 취소 요청이 변경되었습니다.");
        }

        // 동시에 재시도해도 재고 복구는 한 번만 실행합니다.
        if (Integer.valueOf(0).equals(order.getStatusNo())
            && Integer.valueOf(2).equals(order.getCancelStatusNo())
            && Integer.valueOf(3).equals(payment.getStatusNo())) {
            return;
        }

        validateBeforeShipping(order);
        if (!Integer.valueOf(3).equals(order.getCancelStatusNo())
            || !Integer.valueOf(1).equals(payment.getStatusNo())) {
            throw new IllegalStateException("현재 주문 상태에서는 취소 완료 처리할 수 없습니다.");
        }

        List<OrderItem> items = orderItemRepository.findAllByOrderNoOrderByNoAsc(order.getNo());
        validateItems(items);

        // 재고 잠금 순서를 옵션번호로 통일합니다.
        List<OrderItem> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparing(item -> item.getProductOption().getNo()));

        for (OrderItem item : sorted) {
            Stock stock = stockRepository.findByProductOptionNoForUpdate(item.getProductOption().getNo())
                .orElseThrow(() -> new IllegalStateException("복구할 재고정보가 없습니다."));

            stock.setQty(Math.addExact(stock.getQty(), item.getQty()));
            stock.setUdate(LocalDateTime.now());

            item.setStatusNo(0);
            item.setCancelQty(item.getQty());
            item.setCancelReason(payment.getCancelReason());
            item.setCancelDate(cancelledAt);
        }

        payment.setStatusNo(3);
        payment.setCancelDate(cancelledAt);
        order.setStatusNo(0);
        order.setCancelStatusNo(2);
    }

    private PreparedCancel toPrepared(Payment payment, boolean completed) {
        return new PreparedCancel(
            payment.getOrder().getNo(),
            payment.getPaymentKey(),
            payment.getTossOrderId(),
            payment.getAmount(),
            payment.getCancelKey(),
            payment.getCancelReason(),
            completed
        );
    }
}