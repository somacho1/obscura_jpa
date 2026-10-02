package dev.jpa.obscura_jpa.product;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// JpaRepository: 기존 저장·조회 기능
// JpaSpecificationExecutor: 카테고리·할인 조건을 조합하고 페이지 단위로 조회하는 기능
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // 전체 상품 최신순
    List<Product> findAllByOrderByNoDesc();

    // 판매중 상품
    List<Product> findAllByStatusNoOrderByNoDesc(Integer statusNo);

    // 특정 브랜드 상품
    List<Product> findAllByBrandNoOrderByNoDesc(Long bno);

    // 특정 카테고리 상품
    List<Product> findAllByCategoryNoOrderByNoDesc(Long cno);

    // 브랜드 + 카테고리 상품
    List<Product> findAllByBrandNoAndCategoryNoOrderByNoDesc(Long bno, Long cno);

    // 상품명 검색
    List<Product> findByNameContainingIgnoreCaseOrderByNoDesc(String name);

    // 특정 브랜드의 전체 상품 수
    long countByBrandNo(Long bno);

    // 특정 브랜드의 할인 상품 수
    long countByBrandNoAndDiscountRateGreaterThan(Long bno, Integer discountRate);

    // 상품 등록 시 같은 CODE가 이미 있는지 확인합니다.
    boolean existsByCodeIgnoreCase(String code);

    // 상품 수정 시 현재 상품을 제외하고 같은 CODE가 있는지 확인합니다.
    boolean existsByCodeIgnoreCaseAndNoNot(String code, Long no);
}