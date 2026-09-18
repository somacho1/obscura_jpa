package dev.jpa.obscura_jpa.productoption;

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
@RequestMapping("/api/product-options")
@CrossOrigin(origins = "*")
public class ProductOptionController {

    private final ProductOptionService productOptionService;

    public ProductOptionController(
        ProductOptionService productOptionService
    ) {
        this.productOptionService = productOptionService;
    }

    // 옵션 등록
    @PostMapping
    public ResponseEntity<?> createOption(
        @RequestBody ProductOptionDTO dto
    ) {

        try {

            ProductOptionDTO result =
                productOptionService.createOption(dto);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 옵션 단건 조회
    @GetMapping("/{no}")
    public ResponseEntity<?> findByNo(
        @PathVariable("no") Long no
    ) {

        try {

            return ResponseEntity.ok(
                productOptionService.findByNo(no)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 특정 상품의 전체 옵션
    @GetMapping("/product/{pno}")
    public ResponseEntity<List<ProductOptionDTO>> findByProduct(
        @PathVariable("pno") Long pno
    ) {

        return ResponseEntity.ok(
            productOptionService.findByProduct(pno)
        );
    }

    // 특정 상품의 사용 가능한 옵션
    @GetMapping("/product/{pno}/available")
    public ResponseEntity<List<ProductOptionDTO>> findAvailableByProduct(
        @PathVariable("pno") Long pno
    ) {

        return ResponseEntity.ok(
            productOptionService.findAvailableByProduct(pno)
        );
    }

    // 옵션 수정
    @PutMapping("/{no}")
    public ResponseEntity<?> updateOption(
        @PathVariable("no") Long no,
        @RequestBody ProductOptionDTO dto
    ) {

        try {

            return ResponseEntity.ok(
                productOptionService.updateOption(no, dto)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 옵션 비활성화
    @DeleteMapping("/{no}")
    public ResponseEntity<?> disableOption(
        @PathVariable("no") Long no
    ) {

        try {

            productOptionService.disableOption(no);

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