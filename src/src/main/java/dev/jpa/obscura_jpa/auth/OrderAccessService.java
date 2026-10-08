package dev.jpa.obscura_jpa.auth;

import java.util.Objects;
import dev.jpa.obscura_jpa.member.ObMember;
import dev.jpa.obscura_jpa.order.Order;
import dev.jpa.obscura_jpa.order.OrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderAccessService {
    private final SessionAuthService auth;
    private final OrderRepository orders;

    // 주문 상세는 주문한 본인 또는 운영 관리자만 조회할 수 있습니다.
    public void requireView(HttpServletRequest request, Long no) {
        ObMember member = auth.requireMember(request);
        Order order = orders.findById(no).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
        if ("ADMIN".equals(member.getRole()) || "SUPER_ADMIN".equals(member.getRole())) return;
        if (!Objects.equals(order.getMember().getNo(), member.getNo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "다른 회원의 주문은 조회할 수 없습니다.");
        }
    }
}
