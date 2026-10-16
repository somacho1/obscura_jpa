package dev.jpa.obscura_jpa.payment;

import dev.jpa.obscura_jpa.auth.RequireRole;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/bank")

public class BankRefundController {

    private final BankRefundService bankRefundService;

    public BankRefundController(BankRefundService bankRefundService) {
        this.bankRefundService = bankRefundService;
    }

    // 고객: 본인 환불정보 조회
    @GetMapping("/order/{no}/refund")
    public ResponseEntity<?> find(
        @PathVariable("no") Long no,
        @RequestParam("mno") Long mno
    ) {
        return respond(() -> bankRefundService.find(no, mno, false));
    }

    // 고객: 취소 사유·환불 계좌 접수
    @PostMapping("/order/{no}/refund")
    public ResponseEntity<?> request(
        @PathVariable("no") Long no,
        @RequestBody BankRefundRequest request
    ) {
        return respond(() -> bankRefundService.request(no, request));
    }

    // 관리자 API의 ADMIN 권한 검증은 서버 인증 작업에서 연결해야 합니다.
    @RequireRole
    @GetMapping("/admin/order/{no}/refund")
    public ResponseEntity<?> findAdmin(@PathVariable("no") Long no) {
        return respond(() -> bankRefundService.find(no, null, true));
    }

    // 실제 송금 후 관리자 완료 처리
    @RequireRole
    @PutMapping("/admin/order/{no}/refund/complete")
    public ResponseEntity<?> complete(@PathVariable("no") Long no) {
        return respond(() -> bankRefundService.complete(no));
    }

    private ResponseEntity<?> respond(Supplier<?> action) {
        try {
            return ResponseEntity.ok(action.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
