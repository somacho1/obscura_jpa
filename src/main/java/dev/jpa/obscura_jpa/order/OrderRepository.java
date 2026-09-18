package dev.jpa.obscura_jpa.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository
    extends JpaRepository<Order, Long> {

    // 회원별 주문내역 - 최근 주문부터
    List<Order> findAllByMemberNoOrderByCdateDesc(
        Long mno
    );
}