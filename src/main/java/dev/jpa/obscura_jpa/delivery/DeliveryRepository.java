package dev.jpa.obscura_jpa.delivery;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    /**
     * 주문번호에 속한 배송목록.
     *
     * 부분배송이 가능하기 때문에 List로 반환한다.
     */
    List<Delivery> findAllByOrderNoOrderByNoAsc(Long ordno);
}