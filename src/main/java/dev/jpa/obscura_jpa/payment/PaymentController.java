package dev.jpa.obscura_jpa.payment;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(
        PaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }

    /**
     * 결제정보 생성.
     */
    @PostMapping
    public ResponseEntity<?> create(
        @RequestBody PaymentDTO dto
    ) {

        try {

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentService.create(dto));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(Map.of(
                    "message",
                    e.getMessage()
                ));
        }
    }

    /**
     * 전체 결제정보 조회.
     */
    @GetMapping
    public ResponseEntity<List<PaymentDTO>> findAll() {

        return ResponseEntity.ok(
            paymentService.findAll()
        );
    }

    /**
     * 결제번호로 조회.
     */
    @GetMapping("/{no}")
    public ResponseEntity<?> findByNo(
        @PathVariable("no") Long no
    ) {

        try {

            return ResponseEntity.ok(
                paymentService.findByNo(no)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(Map.of(
                    "message",
                    e.getMessage()
                ));
        }
    }

    /**
     * 주문번호로 결제정보 조회.
     */
    @GetMapping("/order/{ordno}")
    public ResponseEntity<?> findByOrder(
        @PathVariable("ordno") Long ordno
    ) {

        try {

            return ResponseEntity.ok(
                paymentService.findByOrder(ordno)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(Map.of(
                    "message",
                    e.getMessage()
                ));
        }
    }

    /**
     * 결제상태 변경.
     */
    @PutMapping("/{no}/status")
    public ResponseEntity<?> updateStatus(
        @PathVariable("no") Long no,
        @RequestBody PaymentDTO dto
    ) {

        try {

            return ResponseEntity.ok(
                paymentService.updateStatus(
                    no,
                    dto
                )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(Map.of(
                    "message",
                    e.getMessage()
                ));
        }
    }
}