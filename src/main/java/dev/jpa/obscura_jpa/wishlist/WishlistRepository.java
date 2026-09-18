package dev.jpa.obscura_jpa.wishlist;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // 특정 회원의 찜 목록
    List<Wishlist> findAllByMemberNoOrderByCdateDesc(Long mno);

    // 특정 회원이 특정 상품을 찜했는지 조회
    Optional<Wishlist> findByMemberNoAndProductNo(
        Long mno,
        Long pno
    );

    // 특정 회원 + 상품의 찜 존재 여부
    boolean existsByMemberNoAndProductNo(
        Long mno,
        Long pno
    );
}