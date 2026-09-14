package dev.jpa.obscura_jpa.category;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * 상품 카테고리 Service
 *
 * 카테고리 등록, 조회, 수정,
 * 활성/비활성 처리 로직을 담당한다.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 카테고리 등록
     *
     * - 카테고리명 중복 검사
     * - 기본 상태 활성(1)
     * - 등록일 자동 설정
     */
    public CategoryDTO createCategory(CategoryDTO dto) {

        if (categoryRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("이미 등록된 카테고리명입니다.");
        }

        Category category = Category.builder()
            .name(dto.getName())
            .statusNo(1)
            .seqNo(dto.getSeqNo())
            .cdate(LocalDateTime.now())
            .build();

        Category savedCategory = categoryRepository.save(category);

        return toDTO(savedCategory);
    }

    /**
     * 전체 카테고리 조회
     *
     * 관리자 화면에서 사용
     * SEQNO 오름차순
     */
    public List<CategoryDTO> findAll() {

        return categoryRepository.findAllByOrderBySeqNoAsc()
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * 활성 카테고리 조회
     *
     * 사용자 화면에서 사용
     * STATUSNO = 1
     */
    public List<CategoryDTO> findActiveCategories() {

        return categoryRepository
            .findAllByStatusNoOrderBySeqNoAsc(1)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * 카테고리번호로 단건 조회
     */
    public Optional<CategoryDTO> findByNo(Long no) {

        return categoryRepository.findById(no)
            .map(this::toDTO);
    }

    /**
     * 카테고리명으로 단건 조회
     */
    public Optional<CategoryDTO> findByName(String name) {

        return categoryRepository.findByName(name)
            .map(this::toDTO);
    }

    /**
     * 카테고리 수정
     *
     * 수정 가능 항목
     * - 카테고리명
     * - 상태
     * - 노출순서
     */
    public CategoryDTO updateCategory(Long no, CategoryDTO dto) {

        Category category = categoryRepository.findById(no)
            .orElseThrow(() ->
                new IllegalArgumentException("카테고리 정보를 찾을 수 없습니다.")
            );

        /*
         * 카테고리명이 변경되는 경우에만
         * 중복검사를 한다.
         */
        if (dto.getName() != null &&
            !dto.getName().isBlank() &&
            !dto.getName().equals(category.getName())) {

            if (categoryRepository.existsByName(dto.getName())) {
                throw new IllegalArgumentException("이미 등록된 카테고리명입니다.");
            }

            category.setName(dto.getName());
        }

        /*
         * 노출순서 수정
         */
        if (dto.getSeqNo() != null) {

            if (dto.getSeqNo() < 1) {
                throw new IllegalArgumentException("노출순서는 1 이상이어야 합니다.");
            }

            category.setSeqNo(dto.getSeqNo());
        }

        /*
         * 상태 수정
         *
         * 0 = 비활성
         * 1 = 활성
         */
        if (dto.getStatusNo() != null) {

            if (dto.getStatusNo() != 0 &&
                dto.getStatusNo() != 1) {

                throw new IllegalArgumentException(
                    "카테고리 상태는 0 또는 1만 가능합니다."
                );
            }

            category.setStatusNo(dto.getStatusNo());
        }

        Category updatedCategory = categoryRepository.save(category);

        return toDTO(updatedCategory);
    }

    /**
     * 카테고리 비활성 처리
     *
     * 실제 DELETE 하지 않고
     * STATUSNO = 0으로 변경한다.
     */
    public void disableCategory(Long no) {

        Category category = categoryRepository.findById(no)
            .orElseThrow(() ->
                new IllegalArgumentException("카테고리 정보를 찾을 수 없습니다.")
            );

        if (category.getStatusNo() == 0) {
            throw new IllegalArgumentException("이미 비활성 상태인 카테고리입니다.");
        }

        category.setStatusNo(0);

        categoryRepository.save(category);
    }

    /**
     * Entity -> DTO 변환
     */
    private CategoryDTO toDTO(Category category) {

        return CategoryDTO.builder()
            .no(category.getNo())
            .name(category.getName())
            .statusNo(category.getStatusNo())
            .seqNo(category.getSeqNo())
            .cdate(category.getCdate())
            .build();
    }
}