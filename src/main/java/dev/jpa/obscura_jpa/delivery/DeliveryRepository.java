package dev.jpa.obscura_jpa.delivery;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    // 엔티티를 미리 읽지 않고 주문번호만 조회합니다.
    // 주문을 먼저 잠근 뒤 최신 배송 상태를 확인하기 위해 사용합니다.
    @Query("select d.order.no from Delivery d where d.no = :no")
    Optional<Long> findOrderNoByDeliveryNo(@Param("no") Long no);
}