package dev.jpa.obscura_jpa.producttag;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductTagRepository
    extends JpaRepository<ProductTag, Long> {

    /**
     * 특정 상품의 AI 태그 전체 조회.
     *
     * SCORE가 높은 순서로 반환한다.
     */
    List<ProductTag> findAllByProductNoOrderByScoreDesc(
        Long pno
    );

    /**
     * 동일 상품 + 동일 태그 중복 확인.
     */
    boolean existsByProductNoAndTag(
        Long pno,
        String tag
    );

    /**
     * 특정 태그를 가진 상품 조회.
     *
     * 나중에 추천 후보상품 검색에 활용 가능.
     */
    List<ProductTag> findAllByTagOrderByScoreDesc(
        String tag
    );
}