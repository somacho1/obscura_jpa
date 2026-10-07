package dev.jpa.obscura_jpa.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 무통장입금 신청을 받는 API입니다.
@RestController
@RequestMapping("/api/payments/bank")
@CrossOrigin(origins = "*")
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
}