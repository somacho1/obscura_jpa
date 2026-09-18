package dev.jpa.obscura_jpa.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 전체 상품 최신순
    List<Product> findAllByOrderByNoDesc();

    // 판매중 상품
    List<Product> findAllByStatusNoOrderByNoDesc(Integer statusNo);

    // 특정 브랜드 상품
    List<Product> findAllByBrandNoOrderByNoDesc(Long bno);

    // 특정 카테고리 상품
    List<Product> findAllByCategoryNoOrderByNoDesc(Long cno);

    // 브랜드 + 카테고리 상품
    List<Product> findAllByBrandNoAndCategoryNoOrderByNoDesc(
        Long bno,
        Long cno
    );

    // 상품명 검색
    List<Product> findByNameContainingIgnoreCaseOrderByNoDesc(String name);
}