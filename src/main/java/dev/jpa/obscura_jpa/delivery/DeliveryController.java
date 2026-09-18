package dev.jpa.obscura_jpa.delivery;

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
@RequestMapping("/api/deliveries")
@CrossOrigin(origins = "*")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(
        DeliveryService deliveryService
    ) {
        this.deliveryService = deliveryService;
    }

    /**
     * 배송 생성.
     */
    @PostMapping
    public ResponseEntity<?> create(
        @RequestBody DeliveryDTO dto
    ) {

        try {

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(deliveryService.create(dto));

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
     * 전체 배송 조회.
     */
    @GetMapping
    public ResponseEntity<List<DeliveryDTO>> findAll() {

        return ResponseEntity.ok(
            deliveryService.findAll()
        );
    }

    /**
     * 배송번호 조회.
     */
    @GetMapping("/{no}")
    public ResponseEntity<?> findByNo(
        @PathVariable("no") Long no
    ) {

        try {

            return ResponseEntity.ok(
                deliveryService.findByNo(no)
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
     * 주문번호별 배송 조회.
     */
    @GetMapping("/order/{ordno}")
    public ResponseEntity<?> findByOrder(
        @PathVariable("ordno") Long ordno
    ) {

        try {

            return ResponseEntity.ok(
                deliveryService.findByOrder(ordno)
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
     * 배송 시작.
     * 택배사 + 송장번호 등록.
     */
    @PutMapping("/{no}/shipping")
    public ResponseEntity<?> startShipping(
        @PathVariable("no") Long no,
        @RequestBody DeliveryDTO dto
    ) {

        try {

            return ResponseEntity.ok(
                deliveryService.startShipping(
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

    /**
     * 배송완료.
     */
    @PutMapping("/{no}/complete")
    public ResponseEntity<?> completeShipping(
        @PathVariable("no") Long no
    ) {

        try {

            return ResponseEntity.ok(
                deliveryService.completeShipping(no)
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