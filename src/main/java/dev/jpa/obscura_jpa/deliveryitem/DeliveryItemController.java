package dev.jpa.obscura_jpa.deliveryitem;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/delivery-items")
@CrossOrigin(origins = "*")
public class DeliveryItemController {

    private final DeliveryItemService deliveryItemService;

    public DeliveryItemController(
        DeliveryItemService deliveryItemService
    ) {
        this.deliveryItemService =
            deliveryItemService;
    }

    /**
     * 배송상품 등록.
     */
    @PostMapping
    public ResponseEntity<?> create(
        @RequestBody DeliveryItemDTO dto
    ) {

        try {

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                    deliveryItemService.create(dto)
                );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(
                    Map.of(
                        "message",
                        e.getMessage()
                    )
                );
        }
    }

    /**
     * 배송상품번호 조회.
     */
    @GetMapping("/{no}")
    public ResponseEntity<?> findByNo(
        @PathVariable("no") Long no
    ) {

        try {

            return ResponseEntity.ok(
                deliveryItemService.findByNo(no)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(
                    Map.of(
                        "message",
                        e.getMessage()
                    )
                );
        }
    }

    /**
     * 특정 배송에 포함된 상품목록.
     */
    @GetMapping("/delivery/{dno}")
    public ResponseEntity<?> findByDelivery(
        @PathVariable("dno") Long dno
    ) {

        try {

            return ResponseEntity.ok(
                deliveryItemService
                    .findByDelivery(dno)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(
                    Map.of(
                        "message",
                        e.getMessage()
                    )
                );
        }
    }

    /**
     * 배송상품 삭제.
     *
     * 배송준비 상태에서만 가능.
     */
    @DeleteMapping("/{no}")
    public ResponseEntity<?> delete(
        @PathVariable("no") Long no
    ) {

        try {

            deliveryItemService.delete(no);

            return ResponseEntity.ok(
                Map.of(
                    "message",
                    "배송상품이 삭제되었습니다."
                )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(
                    Map.of(
                        "message",
                        e.getMessage()
                    )
                );
        }
    }
}