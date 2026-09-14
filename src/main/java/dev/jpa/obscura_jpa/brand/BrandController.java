package dev.jpa.obscura_jpa.brand;

import java.util.List;
import java.util.Optional;

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

import lombok.RequiredArgsConstructor;

/**
 * 브랜드 REST API Controller
 *
 * 브랜드 등록, 조회, 수정, 비활성 처리 API를 제공한다.
 */
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    /**
     * 브랜드 등록
     *
     * POST /api/brands
     */
    @PostMapping
    public ResponseEntity<?> createBrand(@RequestBody BrandDTO dto) {

        try {
            BrandDTO createdBrand = brandService.createBrand(dto);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdBrand);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    /**
     * 전체 브랜드 조회
     *
     * 관리자용
     * GET /api/brands
     */
    @GetMapping
    public ResponseEntity<List<BrandDTO>> findAll() {

        return ResponseEntity.ok(
            brandService.findAll()
        );
    }

    /**
     * 활성 브랜드 조회
     *
     * 사용자 화면용
     * GET /api/brands/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<BrandDTO>> findActiveBrands() {

        return ResponseEntity.ok(
            brandService.findActiveBrands()
        );
    }

    /**
     * 브랜드번호 단건 조회
     *
     * GET /api/brands/1
     */
    @GetMapping("/{no}")
    public ResponseEntity<?> findByNo(
        @PathVariable("no") Long no
    ) {

        Optional<BrandDTO> brand = brandService.findByNo(no);

        if (brand.isEmpty()) {
            return ResponseEntity
                .badRequest()
                .body("브랜드 정보를 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(
            brand.get()
        );
    }

    /**
     * 브랜드명으로 조회
     *
     * GET /api/brands/search?name=032c
     */
    @GetMapping("/search")
    public ResponseEntity<?> findByName(
        @RequestParam("name") String name
    ) {

        Optional<BrandDTO> brand = brandService.findByName(name);

        if (brand.isEmpty()) {
            return ResponseEntity
                .badRequest()
                .body("브랜드 정보를 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(
            brand.get()
        );
    }

    /**
     * 브랜드 수정
     *
     * PUT /api/brands/1
     */
    @PutMapping("/{no}")
    public ResponseEntity<?> updateBrand(
        @PathVariable("no") Long no,
        @RequestBody BrandDTO dto
    ) {

        try {
            BrandDTO updatedBrand =
                brandService.updateBrand(no, dto);

            return ResponseEntity.ok(updatedBrand);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    /**
     * 브랜드 비활성 처리
     *
     * 실제 DELETE 하지 않고 STATUSNO = 0
     *
     * DELETE /api/brands/1
     */
    @DeleteMapping("/{no}")
    public ResponseEntity<?> disableBrand(
        @PathVariable("no") Long no
    ) {

        try {
            brandService.disableBrand(no);

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