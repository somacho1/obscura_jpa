package dev.jpa.obscura_jpa.productimage;

import java.util.List;

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
@RequestMapping("/api/product-images")
@CrossOrigin(origins = "*")
public class ProductImageController {

    private final ProductImageService productImageService;

    public ProductImageController(
        ProductImageService productImageService
    ) {
        this.productImageService = productImageService;
    }

    // 이미지 등록
    @PostMapping
    public ResponseEntity<?> createImage(
        @RequestBody ProductImageDTO dto
    ) {

        try {
            ProductImageDTO result =
                productImageService.createImage(dto);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 이미지 단건 조회
    @GetMapping("/{no}")
    public ResponseEntity<?> findByNo(
        @PathVariable("no") Long no
    ) {

        try {
            return ResponseEntity.ok(
                productImageService.findByNo(no)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 특정 상품 전체 이미지
    @GetMapping("/product/{pno}")
    public ResponseEntity<List<ProductImageDTO>> findByProduct(
        @PathVariable("pno") Long pno
    ) {

        return ResponseEntity.ok(
            productImageService.findByProduct(pno)
        );
    }

    // 특정 상품 노출 이미지
    @GetMapping("/product/{pno}/visible")
    public ResponseEntity<List<ProductImageDTO>> findVisibleByProduct(
        @PathVariable("pno") Long pno
    ) {

        return ResponseEntity.ok(
            productImageService.findVisibleByProduct(pno)
        );
    }

    // 특정 상품 대표 이미지
    @GetMapping("/product/{pno}/main")
    public ResponseEntity<?> findMainImage(
        @PathVariable("pno") Long pno
    ) {

        try {
            return ResponseEntity.ok(
                productImageService.findMainImage(pno)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 이미지 수정
    @PutMapping("/{no}")
    public ResponseEntity<?> updateImage(
        @PathVariable("no") Long no,
        @RequestBody ProductImageDTO dto
    ) {

        try {
            return ResponseEntity.ok(
                productImageService.updateImage(no, dto)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 이미지 삭제
    @DeleteMapping("/{no}")
    public ResponseEntity<?> deleteImage(
        @PathVariable("no") Long no
    ) {

        try {
            productImageService.deleteImage(no);

            return ResponseEntity
                .noContent()
                .build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }
}