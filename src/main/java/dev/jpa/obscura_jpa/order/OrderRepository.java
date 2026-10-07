package dev.jpa.obscura_jpa.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Long> {

  // 회원별 주문내역을 최근 주문부터 조회합니다.
  List<Order> findAllByMemberNoOrderByCdateDesc(Long mno);

  // 관리자용 전체 주문 조회: 최신순으로 정렬하고 페이지 단위로 반환합니다.
  Page<Order> findAllByOrderByCdateDescNoDesc(Pageable pageable);
    // 취소 처리 중 해당 주문을 잠급니다.
    // 동시에 들어온 취소 요청은 앞선 트랜잭션이 끝난 뒤 상태를 확인하게 됩니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.no = :no")
    Optional<Order> findByNoForUpdate(@Param("no") Long no);
}