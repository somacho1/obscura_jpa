package dev.jpa.obscura_jpa.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// React의 결제 인증 성공 화면에서 호출하는 API입니다.
@RestController
@RequestMapping("/api/payments/toss")

public class TossPaymentController {

    private final TossPaymentService tossPaymentService;

    public TossPaymentController(TossPaymentService tossPaymentService) {
        this.tossPaymentService = tossPaymentService;
    }

    // POST /api/payments/toss/confirm
    // 주문 검증 → Toss 승인 → 결제·주문 완료 저장 순서로 처리합니다.
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody TossConfirmRequest request) {
        try {
            return ResponseEntity.ok(tossPaymentService.confirm(request));
        } catch (IllegalArgumentException e) {
            // 회원번호·주문 상태·금액 등 요청 검증 오류입니다.
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            // 승인 결과를 확인하지 못했거나 완료 저장 조건이 맞지 않는 경우입니다.
            // 이 응답만으로 결제 실패를 확정하거나 주문을 자동 취소하지 않습니다.
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
