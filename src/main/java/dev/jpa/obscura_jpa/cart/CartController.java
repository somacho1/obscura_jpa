package dev.jpa.obscura_jpa.cart;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts")
@CrossOrigin(origins = "*")
public class CartController {

    private final CartService cartService;

    public CartController(
        CartService cartService
    ) {
        this.cartService = cartService;
    }

    // =====================================================
    // 장바구니 생성
    // =====================================================
    @PostMapping
    public ResponseEntity<?> createCart(
        @RequestBody CartDTO dto
    ) {

        try {

            CartDTO result =
                cartService.createCart(dto);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // =====================================================
    // CART 번호로 조회
    // =====================================================
    @GetMapping("/{no}")
    public ResponseEntity<?> findByNo(
        @PathVariable("no") Long no
    ) {

        try {

            return ResponseEntity.ok(
                cartService.findByNo(no)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // =====================================================
    // 회원번호로 장바구니 조회
    // =====================================================
    @GetMapping("/member/{mno}")
    public ResponseEntity<?> findByMember(
        @PathVariable("mno") Long mno
    ) {

        try {

            return ResponseEntity.ok(
                cartService.findByMember(mno)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }
}