package dev.jpa.obscura_jpa.refund;

import java.util.List;
import java.util.Map;
import java.util.HashSet;

// DB·결제 API와 독립적으로 수량, 금액, 마지막 배송비 환불을 검증합니다.
public final class RefundAmounts {
    private RefundAmounts() {}
    public record Item(long no, long price, long qty, long cancelled) {}
    public record Result(long amount, boolean allRemaining) {}

    public static Result calculate(List<Item> items, Map<Long, Long> selected, long shippingFee) {
        if (items.isEmpty() || selected.isEmpty() || shippingFee < 0) {
            throw new IllegalArgumentException("환불할 상품을 선택해주세요.");
        }
        long amount = 0;
        boolean all = true;
        var found = new HashSet<Long>();
        for (Item item : items) {
            if (item.price() < 0 || item.qty() <= 0 || item.cancelled() < 0 || item.cancelled() > item.qty()) {
                throw new IllegalStateException("주문상품 수량과 금액을 확인해주세요.");
            }
            long remaining = item.qty() - item.cancelled();
            Long value = selected.get(item.no());
            long chosen = value == null ? 0 : value;
            if (value != null) {
                found.add(item.no());
                if (chosen <= 0 || chosen > remaining) {
                    throw new IllegalArgumentException("취소 수량이 남은 주문 수량을 초과합니다.");
                }
            }
            amount = Math.addExact(amount, Math.multiplyExact(item.price(), chosen));
            if (chosen != remaining) all = false;
        }
        if (found.size() != selected.size()) throw new IllegalArgumentException("해당 주문에 없는 상품입니다.");
        if (all) amount = Math.addExact(amount, shippingFee);
        if (amount <= 0) throw new IllegalArgumentException("환불 금액은 0원보다 커야 합니다.");
        return new Result(amount, all);
    }
}
