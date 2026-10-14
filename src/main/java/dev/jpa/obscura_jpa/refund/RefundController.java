package dev.jpa.obscura_jpa.refund;

import java.util.function.Supplier;
import dev.jpa.obscura_jpa.auth.SessionAuthService;
import dev.jpa.obscura_jpa.auth.RequireRole;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {
    private final SessionAuthService auth;
    private final RefundStore store;
    private final RefundService service;

    @GetMapping("/order/{orderNo}")
    public ResponseEntity<?> state(@PathVariable("orderNo") Long orderNo, HttpServletRequest request) {
        return respond(() -> {
            store.owner(orderNo, auth.requireMember(request).getNo());
            return store.state(orderNo);
        });
    }
    @PostMapping("/order/{orderNo}")
    public ResponseEntity<?> create(@PathVariable("orderNo") Long orderNo, @RequestBody RefundRequest body, HttpServletRequest request) {
        return respond(() -> service.request(orderNo, auth.requireMember(request).getNo(), body));
    }
    @PostMapping("/{no}/execute")
    public ResponseEntity<?> execute(@PathVariable("no") Long no, HttpServletRequest request) {
        return respond(() -> {
            var info = store.find(no);
            store.owner(info.orderNo(), auth.requireMember(request).getNo());
            if (!"TOSS".equals(info.method())) throw new IllegalArgumentException("무통장 환불은 관리자가 완료 처리합니다.");
            return service.execute(no);
        });
    }
    @RequireRole
    @GetMapping("/admin/order/{orderNo}")
    public ResponseEntity<?> adminState(@PathVariable("orderNo") Long orderNo) { return respond(() -> store.state(orderNo)); }
    @RequireRole
    @PostMapping("/admin/{no}/complete")
    public ResponseEntity<?> adminComplete(@PathVariable("no") Long no, HttpServletRequest request) {
        return respond(() -> {
            auth.requireRole(request, "ADMIN");
            if (!"BANK".equals(store.find(no).method())) throw new IllegalArgumentException("Toss 환불은 고객 화면에서 재시도해주세요.");
            // 이 호출은 실제 송금 기능이 아닙니다. 관리자가 송금 완료를 확인한 후 호출합니다.
            return service.execute(no);
        });
    }
    private ResponseEntity<?> respond(Supplier<?> action) {
        try { return ResponseEntity.ok(action.get()); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(e.getMessage()); }
        catch (IllegalStateException | ArithmeticException e) { return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); }
    }
}
