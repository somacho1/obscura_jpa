package dev.jpa.obscura_jpa.refund;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import dev.jpa.obscura_jpa.order.*;
import dev.jpa.obscura_jpa.orderitem.*;
import dev.jpa.obscura_jpa.payment.*;
import dev.jpa.obscura_jpa.delivery.DeliveryRepository;
import dev.jpa.obscura_jpa.deliveryitem.*;
import dev.jpa.obscura_jpa.stock.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundStore {
    @org.springframework.beans.factory.annotation.Autowired
    private dev.jpa.obscura_jpa.adminorder.AdminOrderEventService adminEvents;

    private final JdbcTemplate jdbc;
    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final PaymentRepository payments;
    private final DeliveryRepository deliveries;
    private final DeliveryItemRepository deliveryItems;
    private final StockRepository stocks;
    private final EntityManager entities;

    // 상태 0은 접수 또는 외부 결과 확인 필요, 1은 DB 반영까지 완료입니다.
    public record Info(Long no, Long orderNo, String requestKey, String method, Long amount,
                       Long beforeAmount, String reason, String bank, String account, String holder,
                       Integer statusNo, LocalDateTime cdate, LocalDateTime completeDate,
                       List<RefundRequest.Selection> items) {}
    public record State(Long refundedAmount, Long balanceAmount, List<Info> history) {}
    public record Prepared(Info info, String paymentKey, String tossOrderId, Long originalAmount) {}

    private Info map(ResultSet rs, int row) throws SQLException {
        var done = rs.getTimestamp("COMPLETEDATE");
        return new Info(rs.getLong("NO"), rs.getLong("ORDNO"), rs.getString("REQUESTKEY"),
            rs.getString("METHOD"), rs.getLong("AMOUNT"), rs.getLong("BEFOREAMOUNT"),
            rs.getString("REASON"), rs.getString("BANK"), rs.getString("ACCOUNT"), rs.getString("HOLDER"),
            rs.getInt("STATUSNO"), rs.getTimestamp("CDATE").toLocalDateTime(),
            done == null ? null : done.toLocalDateTime(), List.of());
    }
    private Info withItems(Info row) {
        var selected = jdbc.query("SELECT OINO, QTY FROM REFUNDITEM WHERE RNO=? ORDER BY OINO",
            (rs, n) -> new RefundRequest.Selection(rs.getLong(1), rs.getLong(2)), row.no());
        return new Info(row.no(), row.orderNo(), row.requestKey(), row.method(), row.amount(), row.beforeAmount(),
            row.reason(), row.bank(), row.account(), row.holder(), row.statusNo(), row.cdate(), row.completeDate(), selected);
    }
    private List<Info> history(Long orderNo) {
        return jdbc.query("SELECT * FROM REFUND WHERE ORDNO=? ORDER BY NO DESC", this::map, orderNo)
            .stream().map(this::withItems).toList();
    }
    public Info find(Long no) {
        var rows = jdbc.query("SELECT * FROM REFUND WHERE NO=?", this::map, no);
        if (rows.isEmpty()) throw new IllegalArgumentException("환불 요청이 없습니다.");
        return withItems(rows.get(0));
    }
    private long refunded(Long orderNo) {
        return jdbc.queryForObject("SELECT NVL(SUM(AMOUNT),0) FROM REFUND WHERE ORDNO=? AND STATUSNO=1", Long.class, orderNo);
    }
    private Order lock(Long no) {
        if (no == null || no <= 0) throw new IllegalArgumentException("올바른 주문번호가 필요합니다.");
        return orders.findByNoForUpdate(no).orElseThrow(() -> new IllegalArgumentException("주문이 없습니다."));
    }
    private Payment payment(Long no) {
        return payments.findByOrderNo(no).orElseThrow(() -> new IllegalArgumentException("결제정보가 없습니다."));
    }
    // 고객 요청은 전달된 회원번호 대신 서버 세션에서 얻은 회원번호로 검사합니다.
    public void owner(Long orderNo, Long memberNo) {
        var order = orders.findById(orderNo).orElseThrow(() -> new IllegalArgumentException("주문이 없습니다."));
        if (!Objects.equals(order.getMember().getNo(), memberNo)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                "본인의 주문만 환불할 수 있습니다.");
        }
    }
    @Transactional(readOnly = true)
    public State state(Long orderNo) {
        Payment pay = payment(orderNo);
        long refunded = refunded(orderNo);
        // 이전 전체환불 기능으로 취소한 주문도 남은 금액을 0으로 표시합니다.
        if (Integer.valueOf(3).equals(pay.getStatusNo())) refunded = pay.getAmount();
        return new State(refunded, Math.subtractExact(pay.getAmount(), refunded), history(orderNo));
    }
    private void beforeShipping(Order order) {
        if (!List.of(2, 3).contains(order.getStatusNo())) {
            throw new IllegalArgumentException("출고 전 결제 완료 주문만 환불할 수 있습니다.");
        }
        if (deliveries.findAllByOrderNoOrderByNoAsc(order.getNo()).stream()
            .anyMatch(d -> !Integer.valueOf(0).equals(d.getStatusNo()) || d.getShipDate() != null)) {
            throw new IllegalArgumentException("이미 출고된 주문은 반품 절차가 필요합니다.");
        }
    }
    private String required(String value, String label, int max) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || text.length() > max) throw new IllegalArgumentException(label + "을 확인해주세요.");
        return text;
    }
    private Map<Long, Long> selections(List<RefundRequest.Selection> selected) {
        if (selected == null || selected.isEmpty()) throw new IllegalArgumentException("환불할 상품을 선택해주세요.");
        Map<Long, Long> result = new TreeMap<>();
        for (var item : selected) {
            if (item == null || item.itemNo() == null || item.itemNo() <= 0 || item.qty() == null || item.qty() <= 0
                || result.putIfAbsent(item.itemNo(), item.qty()) != null) {
                throw new IllegalArgumentException("상품번호와 취소 수량을 확인해주세요.");
            }
        }
        return result;
    }
    private RefundAmounts.Result calculate(Order order, List<OrderItem> rows, Map<Long, Long> selected) {
        return RefundAmounts.calculate(rows.stream().map(i -> new RefundAmounts.Item(i.getNo(), i.getPrice(),
            i.getQty(), i.getCancelQty() == null ? 0 : i.getCancelQty())).toList(), selected, order.getShippingFee());
    }

    public boolean hasKey(String key) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM REFUND WHERE REQUESTKEY=?", Integer.class, key) > 0;
    }
    public record Quote(String method, String paymentKey, String orderId, long originalAmount,
                        long balance, boolean allRemaining) {}
    @Transactional(readOnly = true)
    public Quote quote(Long orderNo, Long memberNo, RefundRequest request) {
        owner(orderNo, memberNo);
        Order order = orders.findById(orderNo).orElseThrow();
        beforeShipping(order);
        Payment pay = payment(orderNo);
        var result = calculate(order, items.findAllByOrderNoOrderByNoAsc(orderNo), selections(request.items()));
        return new Quote(pay.getMethod(), pay.getPaymentKey(), pay.getTossOrderId(), pay.getAmount(),
            pay.getAmount() - refunded(orderNo), result.allRemaining());
    }

    @Transactional
    public Info request(Long orderNo, Long memberNo, RefundRequest request) {
        if (request == null) throw new IllegalArgumentException("환불 요청이 필요합니다.");
        String key;
        try { key = UUID.fromString(request.requestKey()).toString(); }
        catch (RuntimeException e) { throw new IllegalArgumentException("올바른 환불 요청키가 필요합니다."); }
        Order order = lock(orderNo);
        owner(orderNo, memberNo);
        Payment pay = payment(orderNo);
        String reason = required(request.reason(), "환불 사유", 150);
        // 기존 ORDERITEM.CANCELREASON의 BYTE 길이도 지켜 외부 취소 후 DB 오류를 방지합니다.
        if (reason.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 200) {
            throw new IllegalArgumentException("환불 사유는 UTF-8 기준 200바이트 이내로 입력해주세요.");
        }
        Map<Long, Long> selected = selections(request.items());
        String bank = null, account = null, holder = null;
        if ("BANK".equals(pay.getMethod())) {
            bank = required(request.bank(), "은행명", 50);
            holder = required(request.holder(), "예금주", 50);
            account = request.account() == null ? "" : request.account().replaceAll("[\\s-]", "");
            if (!account.matches("[0-9]{6,30}")) throw new IllegalArgumentException("계좌번호는 숫자 6~30자리입니다.");
        } else if (!"TOSS".equals(pay.getMethod())) {
            throw new IllegalArgumentException("지원하지 않는 결제수단입니다.");
        }
        // 새 요청 전에 기존 키를 조회합니다. 완료된 동일 요청도 새 환불로 만들지 않습니다.
        var existing = jdbc.query("SELECT * FROM REFUND WHERE REQUESTKEY=?", this::map, key);
        if (!existing.isEmpty()) {
            Info saved = withItems(existing.get(0));
            if (!saved.orderNo().equals(orderNo) || !saved.reason().equals(reason)
                || !Objects.equals(saved.bank(), bank) || !Objects.equals(saved.account(), account)
                || !Objects.equals(saved.holder(), holder) || !selections(saved.items()).equals(selected)) {
                throw new IllegalArgumentException("동일한 요청키로 다른 환불을 요청할 수 없습니다.");
            }
            return saved;
        }
        beforeShipping(order);
        if (!List.of(1, 2).contains(pay.getStatusNo()) || !List.of(0, 1).contains(order.getCancelStatusNo())) {
            throw new IllegalArgumentException("기존 환불 처리 또는 결제 상태를 확인해주세요.");
        }
        if (!Objects.equals(order.getTotalPrice(), pay.getAmount())) throw new IllegalStateException("주문·결제 금액이 다릅니다.");
        if (history(orderNo).stream().anyMatch(r -> r.statusNo() == 0)) throw new IllegalArgumentException("처리 중인 환불이 있습니다.");
        var rows = items.findAllByOrderNoOrderByNoAsc(orderNo);
        var result = calculate(order, rows, selected);
        long balance = Math.subtractExact(pay.getAmount(), refunded(orderNo));
        if (result.amount() > balance || (result.allRemaining() && result.amount() != balance)) {
            throw new IllegalStateException("상품 취소 수량과 남은 결제금액이 일치하지 않습니다.");
        }
        if ("TOSS".equals(pay.getMethod()) && (pay.getPaymentKey() == null || pay.getTossOrderId() == null)) {
            throw new IllegalStateException("Toss 결제정보가 없습니다.");
        }
        Long no = jdbc.queryForObject("SELECT REFUND_SEQ.NEXTVAL FROM DUAL", Long.class);
        jdbc.update("""
            INSERT INTO REFUND (NO, ORDNO, REQUESTKEY, METHOD, AMOUNT, BEFOREAMOUNT, REASON, BANK, ACCOUNT, HOLDER, STATUSNO, CDATE)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, SYSTIMESTAMP)
            """, no, orderNo, key, pay.getMethod(), result.amount(), balance, reason, bank, account, holder);
        for (var entry : selected.entrySet()) jdbc.update("""
            INSERT INTO REFUNDITEM (NO, RNO, OINO, QTY) VALUES (REFUNDITEM_SEQ.NEXTVAL, ?, ?, ?)
            """, no, entry.getKey(), entry.getValue());
        // 출고와 기존 전체환불은 이 상태에서 차단됩니다.
        order.setCancelStatusNo(3);
        entities.flush();
        adminEvents.record(orderNo,no,"REFUND_REQUEST:"+no,"REFUND_REQUEST",
            "BANK".equals(pay.getMethod()) ? "무통장 환불 요청 · 송금 대기" : "카드 환불 요청 · 결과 확인 필요",result.amount(),true);
        return find(no);
    }

    @Transactional(readOnly = true)
    public Prepared prepared(Long no) {
        Info info = find(no);
        Payment pay = payment(info.orderNo());
        return new Prepared(info, pay.getPaymentKey(), pay.getTossOrderId(), pay.getAmount());
    }
    @Transactional
    public Info complete(Prepared prepared) {
        Info snapshot = prepared.info();
        Order order = lock(snapshot.orderNo());
        Info saved = find(snapshot.no());
        if (saved.statusNo() == 1) return saved; // 동시에 재시도해도 재고 복구는 한 번만 실행합니다.
        beforeShipping(order);
        Payment pay = payment(order.getNo());
        if (!saved.equals(snapshot) || !Integer.valueOf(3).equals(order.getCancelStatusNo())
            || !List.of(1, 2).contains(pay.getStatusNo()) || !Objects.equals(pay.getAmount(), prepared.originalAmount())
            || !Objects.equals(pay.getPaymentKey(), prepared.paymentKey())
            || !Objects.equals(pay.getTossOrderId(), prepared.tossOrderId())
            || !Objects.equals(pay.getMethod(), saved.method())
            || pay.getAmount() - refunded(order.getNo()) != saved.beforeAmount()) {
            throw new IllegalStateException("저장된 환불 요청 또는 결제정보가 변경되었습니다.");
        }
        var rows = items.findAllByOrderNoOrderByNoAsc(order.getNo());
        Map<Long, Long> selected = selections(saved.items());
        var result = calculate(order, rows, selected);
        if (result.amount() != saved.amount()) throw new IllegalStateException("환불 금액이 변경되었습니다.");
        var sorted = rows.stream().filter(i -> selected.containsKey(i.getNo()))
            .sorted(Comparator.comparing(i -> i.getProductOption().getNo())).toList();
        LocalDateTime now = LocalDateTime.now();
        for (OrderItem item : sorted) {
            long qty = selected.get(item.getNo());
            Stock stock = stocks.findByProductOptionNoForUpdate(item.getProductOption().getNo())
                .orElseThrow(() -> new IllegalStateException("복구할 재고가 없습니다."));
            stock.setQty(Math.addExact(stock.getQty(), qty));
            stock.setUdate(now);
            item.setCancelQty(Math.addExact(item.getCancelQty() == null ? 0L : item.getCancelQty(), qty));
            item.setCancelReason(saved.reason());
            item.setCancelDate(now);
            if (item.getCancelQty().equals(item.getQty())) item.setStatusNo(0);
            // 출고 전 배송 배정 수량도 줄입니다. 0개가 된 배정은 제거합니다.
            var links = deliveryItems.findAllByOrderItemNo(item.getNo());
            long assigned = links.stream().mapToLong(DeliveryItem::getQty).sum();
            long excess = Math.max(0L, assigned - (item.getQty() - item.getCancelQty()));
            for (var link : links.stream().sorted(Comparator.comparing(DeliveryItem::getNo).reversed()).toList()) {
                if (excess == 0) break;
                long removed = Math.min(excess, link.getQty());
                int next = Math.toIntExact(link.getQty() - removed);
                if (next == 0) deliveryItems.delete(link); else link.setQty(next);
                excess -= removed;
            }
        }
        boolean all = result.allRemaining();
        pay.setStatusNo(all ? 3 : 2);
        pay.setCancelDate(now);
        order.setCancelStatusNo(all ? 2 : 1);
        if (all) order.setStatusNo(0);
        jdbc.update("UPDATE REFUND SET STATUSNO=1, COMPLETEDATE=? WHERE NO=?", java.sql.Timestamp.valueOf(now), saved.no());
        entities.flush();
        adminEvents.record(order.getNo(),saved.no(),"REFUND_COMPLETED:"+saved.no(),"REFUND_COMPLETED",
            (all ? "전체환불" : "부분환불")+" 완료 · 선택 수량 재고 복구",saved.amount(),true);
        return find(saved.no());
    }
}
