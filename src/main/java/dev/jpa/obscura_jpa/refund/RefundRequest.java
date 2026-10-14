package dev.jpa.obscura_jpa.refund;

import java.util.List;

// 금액은 클라이언트에서 받지 않습니다. 주문 당시 단가와 선택 수량으로 계산합니다.
public record RefundRequest(String requestKey, String reason, String bank, String account,
                            String holder, List<Selection> items) {
    public record Selection(Long itemNo, Long qty) {}
}
