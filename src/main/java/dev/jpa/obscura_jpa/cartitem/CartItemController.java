package dev.jpa.obscura_jpa.cartitem;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart-items")
@CrossOrigin(origins = "*")
public class CartItemController {

    private final CartItemService cartItemService;

    public CartItemController(
        CartItemService cartItemService
    ) {
        this.cartItemService = cartItemService;
    }

    // 장바구니 상품 추가
    @PostMapping
    public ResponseEntity<?> addItem(
        @RequestBody CartItemDTO dto
    ) {

        try {

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                    cartItemService.addItem(dto)
                );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 회원별 장바구니 상품 조회
    @GetMapping("/member/{mno}")
    public ResponseEntity<?> findByMember(
        @PathVariable("mno") Long mno
    ) {

        try {

            List<CartItemDTO> result =
                cartItemService.findByMember(mno);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }
    
 // =====================================================
 // 회원 장바구니 화면 상세 조회
 // =====================================================
 @GetMapping("/member/{mno}/detail")
 public ResponseEntity<?> findDetailByMember(
     @PathVariable("mno") Long mno
 ) {

     try {

         return ResponseEntity.ok(
             cartItemService.findDetailByMember(mno)
         );

     } catch (IllegalArgumentException e) {

         return ResponseEntity
             .badRequest()
             .body(e.getMessage());
     }
 }

    // CARTITEM 단건 조회
    @GetMapping("/{no}")
    public ResponseEntity<?> findByNo(
        @PathVariable("no") Long no
    ) {

        try {

            return ResponseEntity.ok(
                cartItemService.findByNo(no)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 수량 변경
    @PutMapping("/{no}")
    public ResponseEntity<?> updateQty(
        @PathVariable("no") Long no,
        @RequestBody CartItemDTO dto
    ) {

        try {

            return ResponseEntity.ok(
                cartItemService.updateQty(
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

    // 장바구니 상품 삭제
    @DeleteMapping("/{no}")
    public ResponseEntity<?> deleteItem(
        @PathVariable("no") Long no
    ) {

        try {

            cartItemService.deleteItem(no);

            return ResponseEntity.ok(
                Map.of(
                    "message",
                    "장바구니 상품이 삭제되었습니다."
                )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }
}