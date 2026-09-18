package dev.jpa.obscura_jpa.deliveryitem;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryItemRepository
    extends JpaRepository<DeliveryItem, Long> {

    /**
     * 특정 배송에 포함된 상품 조회.
     */
    List<DeliveryItem> findAllByDeliveryNoOrderByNoAsc(Long dno);

    /**
     * 특정 주문상품의 모든 배송기록 조회.
     *
     * 부분배송 수량 계산에 사용한다.
     */
    List<DeliveryItem> findAllByOrderItemNo(Long oino);

    /**
     * 같은 배송에 같은 주문상품이 이미 등록되어 있는지 확인.
     */
    boolean existsByDeliveryNoAndOrderItemNo(
        Long dno,
        Long oino
    );
}