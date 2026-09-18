package dev.jpa.obscura_jpa.product;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // 상품 등록
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductDTO dto) {

        try {
            ProductDTO result = productService.createProduct(dto);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 전체 상품 조회
    @GetMapping
    public ResponseEntity<List<ProductDTO>> findAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    // 판매중 상품 조회
    @GetMapping("/active")
    public ResponseEntity<List<ProductDTO>> findActiveProducts() {
        return ResponseEntity.ok(productService.findActiveProducts());
    }

    // 상품명 검색
    @GetMapping("/search")
    public ResponseEntity<?> searchByName(
        @RequestParam("name") String name
    ) {

        try {
            return ResponseEntity.ok(
                productService.searchByName(name)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 브랜드별 상품 조회
    @GetMapping("/brand/{bno}")
    public ResponseEntity<List<ProductDTO>> findByBrand(
        @PathVariable("bno") Long bno
    ) {
        return ResponseEntity.ok(
            productService.findByBrand(bno)
        );
    }

    // 카테고리별 상품 조회
    @GetMapping("/category/{cno}")
    public ResponseEntity<List<ProductDTO>> findByCategory(
        @PathVariable("cno") Long cno
    ) {
        return ResponseEntity.ok(
            productService.findByCategory(cno)
        );
    }

    // 브랜드 + 카테고리 상품 조회
    @GetMapping("/filter")
    public ResponseEntity<List<ProductDTO>> findByBrandAndCategory(
        @RequestParam("bno") Long bno,
        @RequestParam("cno") Long cno
    ) {
        return ResponseEntity.ok(
            productService.findByBrandAndCategory(bno, cno)
        );
    }

    // 상품 단건 조회
    @GetMapping("/{no}")
    public ResponseEntity<?> findByNo(
        @PathVariable("no") Long no
    ) {

        try {
            return ResponseEntity.ok(
                productService.findByNo(no)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 상품 수정
    @PutMapping("/{no}")
    public ResponseEntity<?> updateProduct(
        @PathVariable("no") Long no,
        @RequestBody ProductDTO dto
    ) {

        try {
            return ResponseEntity.ok(
                productService.updateProduct(no, dto)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 상품 비활성화
    @DeleteMapping("/{no}")
    public ResponseEntity<?> disableProduct(
        @PathVariable("no") Long no
    ) {

        try {
            productService.disableProduct(no);

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }
}