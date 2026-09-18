package dev.jpa.obscura_jpa.cartitem;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository
    extends JpaRepository<CartItem, Long> {

    // 장바구니에 담긴 상품 목록
    List<CartItem> findAllByCartNoOrderByCdateDesc(
        Long cartno
    );

    // 같은 CART + 같은 옵션 조회
    Optional<CartItem> findByCartNoAndProductOptionNo(
        Long cartno,
        Long pono
    );

    // 같은 옵션이 이미 장바구니에 있는지 확인
    boolean existsByCartNoAndProductOptionNo(
        Long cartno,
        Long pono
    );
}