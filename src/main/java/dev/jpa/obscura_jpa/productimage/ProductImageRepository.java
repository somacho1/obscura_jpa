package dev.jpa.obscura_jpa.productimage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    // 특정 상품의 전체 이미지 - 노출순서대로
    List<ProductImage> findAllByProductNoOrderBySeqNoAsc(Long pno);

    // 특정 상품에서 노출중인 이미지
    List<ProductImage> findAllByProductNoAndDisplayYnOrderBySeqNoAsc(
        Long pno,
        String displayYn
    );

    // 특정 상품의 MAIN 이미지
    Optional<ProductImage> findFirstByProductNoAndImageTypeAndDisplayYnOrderBySeqNoAsc(
        Long pno,
        String imageType,
        String displayYn
    );
}