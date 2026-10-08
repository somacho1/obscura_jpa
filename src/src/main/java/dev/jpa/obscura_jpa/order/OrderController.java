package dev.jpa.obscura_jpa.order;

import dev.jpa.obscura_jpa.auth.RequireRole;
import java.util.List;
import dev.jpa.obscura_jpa.auth.SessionAuthService;
import dev.jpa.obscura_jpa.auth.OrderAccessService;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/orders")

public class OrderController {

    private final OrderService orderService;
    private final SessionAuthService auth;
    private final OrderAccessService orderAccess;

    public OrderController(
        OrderService orderService, SessionAuthService auth, OrderAccessService orderAccess
    ) {
        this.orderService = orderService;
        this.auth = auth;
        this.orderAccess = orderAccess;
    }

    // 주문 생성
    @PostMapping
    public ResponseEntity<?> createOrder(
        @RequestBody OrderDTO dto, HttpServletRequest request
    ) {
        auth.requireOwner(request, dto.getMno());

        try {

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                    orderService.createOrder(dto)
                );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }
    
    // 관리자 주문 목록: GET /api/orders/admin/page?page=1&size=20
    // 서버 세션과 DB 권한을 확인한 관리자만 조회할 수 있습니다.
    @RequireRole
    @GetMapping("/admin/page")
    public ResponseEntity<?> findAdminOrderPage(
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        try {
            // 주문 목록과 전체 주문 수·페이지 수를 함께 반환합니다.
            return ResponseEntity.ok(orderService.findAdminOrderPage(page, size));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    // 회원별 주문내역
    @GetMapping("/member/{mno}")
    public ResponseEntity<?> findByMember(
        @PathVariable("mno") Long mno, HttpServletRequest request
    ) {
        auth.requireOwner(request, mno);

        try {

            List<OrderDTO> result =
                orderService.findByMember(mno);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 주문 상세
    @GetMapping("/{no}")
    public ResponseEntity<?> findByNo(
        @PathVariable("no") Long no, HttpServletRequest request
    ) {
        orderAccess.requireView(request, no);

        try {

            return ResponseEntity.ok(
                orderService.findByNo(no)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }
    
 // 결제 대기 주문을 취소합니다.
 // 요청 예: PUT /api/orders/2/cancel-pending
 // 요청 본문: { "mno": 1 }
 @PutMapping("/{no}/cancel-pending")
 public ResponseEntity<?> cancelPendingOrder(
     @PathVariable("no") Long no,
     @RequestBody OrderDTO dto, HttpServletRequest request
 ) {
     auth.requireOwner(request, dto.getMno());
     try {
         // Service에서 주문 상태·회원번호를 확인하고 재고를 복구합니다.
         return ResponseEntity.ok(orderService.cancelPendingOrder(no, dto.getMno()));
     } catch (IllegalArgumentException e) {
         return ResponseEntity.badRequest().body(e.getMessage());
     }
 }

    // 주문상태 변경
    @RequireRole
    @PutMapping("/{no}/status")
    public ResponseEntity<?> updateStatus(
        @PathVariable("no") Long no,
        @RequestBody OrderDTO dto
    ) {

        try {

            return ResponseEntity.ok(
                orderService.updateStatus(
                    no,
                    dto
                )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }
}
