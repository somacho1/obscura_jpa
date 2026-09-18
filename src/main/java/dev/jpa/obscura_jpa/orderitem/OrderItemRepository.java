package dev.jpa.obscura_jpa.orderitem;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository
    extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findAllByOrderNoOrderByNoAsc(
        Long ordno
    );
}