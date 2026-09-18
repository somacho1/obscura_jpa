package dev.jpa.obscura_jpa.wishlist;

import java.util.List;
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
@RequestMapping("/api/wishlists")
@CrossOrigin(origins = "*")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(
        WishlistService wishlistService
    ) {
        this.wishlistService = wishlistService;
    }

    // =====================================================
    // 찜 등록
    // =====================================================
    @PostMapping
    public ResponseEntity<?> createWishlist(
        @RequestBody WishlistDTO dto
    ) {

        try {

            WishlistDTO result =
                wishlistService.createWishlist(dto);

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
    // 회원별 찜 목록
    // =====================================================
    @GetMapping("/member/{mno}")
    public ResponseEntity<?> findByMember(
        @PathVariable("mno") Long mno
    ) {

        try {

            List<WishlistDTO> result =
                wishlistService.findByMember(mno);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // =====================================================
    // 특정 상품 찜 여부
    // =====================================================
    @GetMapping("/member/{mno}/product/{pno}/check")
    public ResponseEntity<Map<String, Boolean>> isWishlisted(
        @PathVariable("mno") Long mno,
        @PathVariable("pno") Long pno
    ) {

        boolean wishlisted =
            wishlistService.isWishlisted(
                mno,
                pno
            );

        return ResponseEntity.ok(
            Map.of(
                "wishlisted",
                wishlisted
            )
        );
    }

    // =====================================================
    // 찜 삭제
    // =====================================================
    @DeleteMapping("/member/{mno}/product/{pno}")
    public ResponseEntity<?> deleteWishlist(
        @PathVariable("mno") Long mno,
        @PathVariable("pno") Long pno
    ) {

        try {

            wishlistService.deleteWishlist(
                mno,
                pno
            );

            return ResponseEntity.ok(
                Map.of(
                    "message",
                    "찜이 삭제되었습니다."
                )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }
}