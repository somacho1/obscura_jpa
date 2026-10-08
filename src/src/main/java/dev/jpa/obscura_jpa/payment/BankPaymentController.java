package dev.jpa.obscura_jpa.payment;

import dev.jpa.obscura_jpa.auth.RequireRole;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

// 무통장입금 신청을 받는 API입니다.
@RestController
@RequestMapping("/api/payments/bank")

public class BankPaymentController {

    private final BankPaymentService bankPaymentService;

    public BankPaymentController(BankPaymentService bankPaymentService) {
        this.bankPaymentService = bankPaymentService;
    }

    // POST /api/payments/bank/apply
    // 신청만 저장하며 실제 입금 확인은 관리자 기능에서 처리합니다.
    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody BankPaymentRequest request) {
        try {
            return ResponseEntity.ok(bankPaymentService.apply(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // 관리자 입금 확인: PUT /api/payments/bank/order/7/confirm
    // 서버 세션과 DB 권한을 확인한 관리자만 조회할 수 있습니다.
    @RequireRole
    @PutMapping("/order/{ordno}/confirm")
    public ResponseEntity<?> confirmDeposit(@PathVariable("ordno") Long ordno) {
        try {
            // Service에서 주문 상태·결제수단·금액을 검증하고 함께 완료 처리합니다.
            return ResponseEntity.ok(bankPaymentService.confirmDeposit(ordno));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
