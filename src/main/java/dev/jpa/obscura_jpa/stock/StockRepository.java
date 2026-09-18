package dev.jpa.obscura_jpa.stock;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    // 특정 상품옵션의 재고 조회
    Optional<Stock> findByProductOptionNo(Long pono);

    // 해당 상품옵션에 이미 STOCK이 존재하는지 확인
    boolean existsByProductOptionNo(Long pono);
}