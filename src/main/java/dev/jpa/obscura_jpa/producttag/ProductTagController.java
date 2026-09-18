package dev.jpa.obscura_jpa.producttag;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-tags")
@CrossOrigin(origins = "*")
public class ProductTagController {

    private final ProductTagService productTagService;

    public ProductTagController(
        ProductTagService productTagService
    ) {
        this.productTagService =
            productTagService;
    }

    /**
     * 상품 AI 태그 등록.
     */
    @PostMapping
    public ResponseEntity<?> create(
        @RequestBody ProductTagDTO dto
    ) {

        try {

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                    productTagService.create(dto)
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
     * 상품별 AI 태그 조회.
     */
    @GetMapping("/product/{pno}")
    public ResponseEntity<?> findByProduct(
        @PathVariable("pno") Long pno
    ) {

        try {

            return ResponseEntity.ok(
                productTagService
                    .findByProduct(pno)
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
     * 태그별 조회.
     *
     * 예)
     * /api/product-tags/search?tag=streetwear
     */
    @GetMapping("/search")
    public ResponseEntity<?> findByTag(
        @RequestParam("tag") String tag
    ) {

        try {

            return ResponseEntity.ok(
                productTagService.findByTag(tag)
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
     * AI 분석점수 수정.
     */
    @PutMapping("/{no}/score")
    public ResponseEntity<?> updateScore(
        @PathVariable("no") Long no,
        @RequestBody ProductTagDTO dto
    ) {

        try {

            return ResponseEntity.ok(
                productTagService.updateScore(
                    no,
                    dto
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

    /**
     * 상품태그 삭제.
     */
    @DeleteMapping("/{no}")
    public ResponseEntity<?> delete(
        @PathVariable("no") Long no
    ) {

        try {

            productTagService.delete(no);

            return ResponseEntity.ok(
                Map.of(
                    "message",
                    "상품태그가 삭제되었습니다."
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