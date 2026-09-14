package dev.jpa.obscura_jpa.category;

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
 * 상품 카테고리 REST API Controller
 *
 * 카테고리 등록, 조회, 수정, 비활성 처리 API를 제공한다.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 카테고리 등록
     *
     * POST /api/categories
     */
    @PostMapping
    public ResponseEntity<?> createCategory(
        @RequestBody CategoryDTO dto
    ) {

        try {
            CategoryDTO createdCategory =
                categoryService.createCategory(dto);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdCategory);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    /**
     * 전체 카테고리 조회
     *
     * 관리자 화면에서 사용
     * GET /api/categories
     */
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> findAll() {

        return ResponseEntity.ok(
            categoryService.findAll()
        );
    }

    /**
     * 활성 카테고리 조회
     *
     * 사용자 화면에서 사용
     * GET /api/categories/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<CategoryDTO>> findActiveCategories() {

        return ResponseEntity.ok(
            categoryService.findActiveCategories()
        );
    }

    /**
     * 카테고리명 단건 조회
     *
     * GET /api/categories/search?name=MEN
     */
    @GetMapping("/search")
    public ResponseEntity<?> findByName(
        @RequestParam("name") String name
    ) {

        Optional<CategoryDTO> category =
            categoryService.findByName(name);

        if (category.isEmpty()) {
            return ResponseEntity
                .badRequest()
                .body("카테고리 정보를 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(
            category.get()
        );
    }

    /**
     * 카테고리번호 단건 조회
     *
     * GET /api/categories/1
     */
    @GetMapping("/{no}")
    public ResponseEntity<?> findByNo(
        @PathVariable("no") Long no
    ) {

        Optional<CategoryDTO> category =
            categoryService.findByNo(no);

        if (category.isEmpty()) {
            return ResponseEntity
                .badRequest()
                .body("카테고리 정보를 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(
            category.get()
        );
    }

    /**
     * 카테고리 수정
     *
     * PUT /api/categories/1
     */
    @PutMapping("/{no}")
    public ResponseEntity<?> updateCategory(
        @PathVariable("no") Long no,
        @RequestBody CategoryDTO dto
    ) {

        try {
            CategoryDTO updatedCategory =
                categoryService.updateCategory(no, dto);

            return ResponseEntity.ok(updatedCategory);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    /**
     * 카테고리 비활성 처리
     *
     * 실제 DELETE 하지 않고 STATUSNO = 0
     *
     * DELETE /api/categories/1
     */
    @DeleteMapping("/{no}")
    public ResponseEntity<?> disableCategory(
        @PathVariable("no") Long no
    ) {

        try {
            categoryService.disableCategory(no);

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