package dev.jpa.obscura_jpa.cart;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // 회원번호로 장바구니 조회
    Optional<Cart> findByMemberNo(Long mno);

    // 회원에게 장바구니가 존재하는지 확인
    boolean existsByMemberNo(Long mno);
}