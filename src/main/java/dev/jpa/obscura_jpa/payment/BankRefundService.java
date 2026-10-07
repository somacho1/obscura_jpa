package dev.jpa.obscura_jpa.payment;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
@Transactional
public class BankRefundService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockRepository stockRepository;

    public BankRefundService(
        OrderRepository orderRepository,
        PaymentRepository paymentRepository,
        DeliveryRepository deliveryRepository,
        OrderItemRepository orderItemRepository,
        StockRepository stockRepository
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.deliveryRepository = deliveryRepository;
        this.orderItemRepository = orderItemRepository;
        this.stockRepository = stockRepository;
    }

    // 환불 화면에 필요한 정보만 반환합니다.
    public record RefundInfo(
        Long orderNo,
        Long amount,
        Integer orderStatusNo,
        Integer cancelStatusNo,
        Integer paymentStatusNo,
        String reason,
        String bank,
        String account,
        String holder,
        LocalDateTime cancelDate
    ) {
    }

    @Transactional(readOnly = true)
    public RefundInfo find(Long orderNo, Long mno, boolean admin) {
        Order order = orderRepository.findById(orderNo)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if (!admin) validateMember(order, mno);
        return toInfo(order, findPayment(orderNo));
    }

    // 고객 환불 요청: 입금 확인 완료·출고 전 주문만 가능합니다.
    public RefundInfo request(Long orderNo, BankRefundRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("환불 요청 정보가 필요합니다.");
        }

        Order order = lockOrder(orderNo);
        validateMember(order, request.mno());
        Payment payment = findPayment(orderNo);

        String reason = required(request.reason(), "취소 사유", 200);
        String bank = required(request.bank(), "은행명", 50);
        String holder = required(request.holder(), "예금주", 50);
        String account = request.account() == null
            ? "" : request.account().replaceAll("[\\s-]", "");

        // 기존 CANCELREASON 컬럼의 바이트 길이도 확인합니다.
        if (reason.getBytes(StandardCharsets.UTF_8).length > 200) {
            throw new IllegalArgumentException("취소 사유는 UTF-8 기준 200바이트 이내로 입력해주세요.");
        }
        if (!account.matches("[0-9]{6,30}")) {
            throw new IllegalArgumentException("계좌번호는 숫자 6~30자리로 입력해주세요.");
        }

        validateBeforeShipping(order);

        if (!Integer.valueOf(1).equals(payment.getStatusNo())) {
            throw new IllegalArgumentException("입금 확인 완료 주문만 환불을 요청할 수 있습니다.");
        }

        // 동일한 요청이 반복돼도 요청 내용을 덮어쓰지 않습니다.
        if (Integer.valueOf(3).equals(order.getCancelStatusNo())) {
            if (!Objects.equals(payment.getCancelReason(), reason)
                || !Objects.equals(payment.getRefundBank(), bank)
                || !Objects.equals(payment.getRefundAccount(), account)
                || !Objects.equals(payment.getRefundHolder(), holder)) {
                throw new IllegalArgumentException("이미 접수된 환불 요청이 있습니다.");
            }
            return toInfo(order, payment);
        }

        if (!Integer.valueOf(0).equals(order.getCancelStatusNo())) {
            throw new IllegalArgumentException("기존 취소 상태를 확인해주세요.");
        }

        validateItems(orderItemRepository.findAllByOrderNoOrderByNoAsc(orderNo));

        payment.setCancelKey(UUID.randomUUID().toString());
        payment.setCancelReason(reason);
        payment.setRefundBank(bank);
        payment.setRefundAccount(account);
        payment.setRefundHolder(holder);
        order.setCancelStatusNo(3);

        // 아직 실제 송금 전이므로 결제 취소·재고 복구는 하지 않습니다.
        return toInfo(order, payment);
    }

    // 관리자: 실제 송금 완료 후 호출합니다.
    public RefundInfo complete(Long orderNo) {
        Order order = lockOrder(orderNo);
        Payment payment = findPayment(orderNo);

        // 완료 요청을 반복해도 재고를 다시 복구하지 않습니다.
        if (Integer.valueOf(0).equals(order.getStatusNo())
            && Integer.valueOf(2).equals(order.getCancelStatusNo())
            && Integer.valueOf(3).equals(payment.getStatusNo())
            && payment.getRefundAccount() != null) {
            return toInfo(order, payment);
        }

        validateBeforeShipping(order);

        if (!Integer.valueOf(3).equals(order.getCancelStatusNo())
            || !Integer.valueOf(1).equals(payment.getStatusNo())
            || payment.getCancelKey() == null
            || payment.getRefundBank() == null
            || payment.getRefundAccount() == null
            || payment.getRefundHolder() == null
            || payment.getCancelReason() == null) {
            throw new IllegalArgumentException("접수된 무통장입금 환불 요청이 없습니다.");
        }

        List<OrderItem> items = orderItemRepository.findAllByOrderNoOrderByNoAsc(orderNo);
        validateItems(items);

        List<OrderItem> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparing(item -> item.getProductOption().getNo()));

        LocalDateTime now = LocalDateTime.now();

        for (OrderItem item : sorted) {
            Stock stock = stockRepository.findByProductOptionNoForUpdate(item.getProductOption().getNo())
                .orElseThrow(() -> new IllegalStateException("복구할 재고정보가 없습니다."));

            stock.setQty(Math.addExact(stock.getQty(), item.getQty()));
            stock.setUdate(now);

            item.setStatusNo(0);
            item.setCancelQty(item.getQty());
            item.setCancelReason(payment.getCancelReason());
            item.setCancelDate(now);
        }

        payment.setStatusNo(3);
        payment.setCancelDate(now);
        order.setStatusNo(0);
        order.setCancelStatusNo(2);

        // 주문·결제·주문상품·재고를 같은 트랜잭션에서 저장합니다.
        return toInfo(order, payment);
    }

    private Order lockOrder(Long no) {
        if (no == null || no <= 0) {
            throw new IllegalArgumentException("올바른 주문번호가 필요합니다.");
        }
        return orderRepository.findByNoForUpdate(no)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
    }

    private Payment findPayment(Long orderNo) {
        Payment payment = paymentRepository.findByOrderNo(orderNo)
            .orElseThrow(() -> new IllegalArgumentException("결제정보가 없습니다."));

        if (!"BANK".equals(payment.getMethod())) {
            throw new IllegalArgumentException("무통장입금 주문만 처리할 수 있습니다.");
        }
        if (!Objects.equals(payment.getAmount(), payment.getOrder().getTotalPrice())) {
            throw new IllegalStateException("주문금액과 결제금액이 다릅니다.");
        }
        return payment;
    }

    private void validateMember(Order order, Long mno) {
        if (mno == null || !order.getMember().getNo().equals(mno)) {
            throw new IllegalArgumentException("본인의 주문만 처리할 수 있습니다.");
        }
    }

    private void validateBeforeShipping(Order order) {
        if (!Integer.valueOf(2).equals(order.getStatusNo())
            && !Integer.valueOf(3).equals(order.getStatusNo())) {
            throw new IllegalArgumentException("출고 전 주문만 환불 처리할 수 있습니다.");
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
            if (item.getQty() == null || item.getQty() <= 0
                || (item.getCancelQty() != null && item.getCancelQty() != 0L)) {
                throw new IllegalStateException("주문상품의 취소 수량을 확인해주세요.");
            }
        }
    }

    private String required(String value, String label, int max) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || text.length() > max) {
            throw new IllegalArgumentException(label + "은 1~" + max + "자로 입력해주세요.");
        }
        return text;
    }

    private RefundInfo toInfo(Order order, Payment payment) {
        return new RefundInfo(
            order.getNo(), payment.getAmount(), order.getStatusNo(),
            order.getCancelStatusNo(), payment.getStatusNo(),
            payment.getCancelReason(), payment.getRefundBank(),
            payment.getRefundAccount(), payment.getRefundHolder(),
            payment.getCancelDate()
        );
    }
}