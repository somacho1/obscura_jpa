package dev.jpa.obscura_jpa.product;

import dev.jpa.obscura_jpa.auth.RequireRole;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

public class ProductController {

    private final ProductService productService;
    private final ProductPermanentDeleteService permanentDeleteService;


    public ProductController(ProductService productService, ProductPermanentDeleteService permanentDeleteService) {
        this.productService = productService;
        this.permanentDeleteService = permanentDeleteService;
    }
    // 상품 등록
    @RequireRole
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
    
    // 사용자 상품 목록: 검색·카테고리·SALE·정렬·페이징 조건을 함께 처리합니다.
    @GetMapping("/page")
    public ResponseEntity<?> findProductPage(
        @RequestParam(value = "cno", required = false) Long cno,
        @RequestParam(value = "saleOnly", defaultValue = "false") boolean saleOnly,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "24") int size,
        @RequestParam(value = "sort", defaultValue = "LATEST") String sort,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        try {
            // 검색어까지 전달하여 상품명·브랜드명·CODE를 검색합니다.
            return ResponseEntity.ok(productService.findProductPage(cno, saleOnly, page, size, sort, keyword));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
    @RequireRole
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
    
 // 관리자 선택 상품 할인율 일괄 적용
    @RequireRole
    @PutMapping("/bulk-discount")
    public ResponseEntity<?> updateBulkDiscount(@RequestBody ProductBulkDiscountDTO dto) {
        try {
            productService.updateBulkDiscount(dto);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
 // 주문 이력이 없는 상품만 영구 삭제합니다. 기존 DELETE /{no}는 판매중지 용도로 유지합니다.
    @RequireRole
    @DeleteMapping("/{no}/permanent")
    public ResponseEntity<?> deletePermanently(@PathVariable("no") Long no) {
        try {
            permanentDeleteService.delete(no);
            return ResponseEntity.noContent().build(); // 삭제 성공: 204
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // 주문 이력 있음: 409
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // 상품 없음: 400
        }
    }
    

    // 상품 비활성화
    @RequireRole
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
       
    /**
     * 사용자 상품 상세페이지 조회
     *
     * PRODUCT + PRODUCTIMAGE + PRODUCTOPTION + STOCK 데이터를
     * 상세페이지용 DTO로 조합해서 반환한다.
     */
    @GetMapping("/{no}/detail")
    public ResponseEntity<ProductDetailDTO> findDetailByNo(
        @PathVariable("no") Long no
    ) {
        return ResponseEntity.ok(
            productService.findDetailByNo(no)
        );
    }
    
 // 메인 MD 추천 상품 조회
    @GetMapping("/md-picks")
    public ResponseEntity<?> findMdPicks(
        @RequestParam(value = "size", defaultValue = "5") int size
    ) {
        try {
            return ResponseEntity.ok(productService.findMdPicks(size));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 관리자 MD 추천 여부·표시 순서 변경
    @RequireRole
    @PutMapping("/{no}/md-pick")
    public ResponseEntity<?> updateMdPick(
        @PathVariable("no") Long no,
        @RequestBody ProductDTO dto
    ) {
        try {
            return ResponseEntity.ok(productService.updateMdPick(no, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
