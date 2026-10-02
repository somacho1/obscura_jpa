package dev.jpa.obscura_jpa.stock;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface StockRepository extends JpaRepository<Stock, Long> {

    // 상품 옵션의 재고를 일반 조회합니다.
    Optional<Stock> findByProductOptionNo(Long pono);

    // 해당 옵션에 재고정보가 등록되어 있는지 확인합니다.
    boolean existsByProductOptionNo(Long pono);

    // 재고 변경 전에 해당 행을 잠급니다.
    // 다른 트랜잭션은 현재 변경이 완료된 뒤 재고를 읽고 변경합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.productOption.no = :pono")
    Optional<Stock> findByProductOptionNoForUpdate(@Param("pono") Long pono);
}