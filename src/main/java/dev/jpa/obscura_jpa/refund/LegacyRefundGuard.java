package dev.jpa.obscura_jpa.refund;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LegacyRefundGuard {
    private final JdbcTemplate jdbc;
    public void requireLegacy(Long orderNo) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM REFUND WHERE ORDNO=?", Integer.class, orderNo);
        if (count != null && count > 0) throw new IllegalArgumentException("상품별 환불 화면에서 남은 수량을 취소해주세요.");
    }
}
