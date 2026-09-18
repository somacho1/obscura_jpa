package dev.jpa.obscura_jpa.order;

import java.util.List;

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
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    public OrderController(
        OrderService orderService
    ) {
        this.orderService = orderService;
    }

    // 주문 생성
    @PostMapping
    public ResponseEntity<?> createOrder(
        @RequestBody OrderDTO dto
    ) {

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

    // 회원별 주문내역
    @GetMapping("/member/{mno}")
    public ResponseEntity<?> findByMember(
        @PathVariable("mno") Long mno
    ) {

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
        @PathVariable("no") Long no
    ) {

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

    // 주문상태 변경
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