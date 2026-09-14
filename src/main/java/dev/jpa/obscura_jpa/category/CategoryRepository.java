package dev.jpa.obscura_jpa.category;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 상품 카테고리 Repository
 *
 * 카테고리 조회, 이름 중복검사,
 * 활성 카테고리 조회 기능을 담당한다.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 카테고리명 중복 검사
     */
    boolean existsByName(String name);

    /**
     * 카테고리명으로 단건 조회
     */
    Optional<Category> findByName(String name);

    /**
     * 전체 카테고리 노출순서 조회
     *
     * 관리자 화면에서 사용
     * SEQNO 오름차순
     */
    List<Category> findAllByOrderBySeqNoAsc();

    /**
     * 활성 카테고리만 노출순서 조회
     *
     * 사용자 화면에서 사용
     * STATUSNO = 1
     * SEQNO 오름차순
     */
    List<Category> findAllByStatusNoOrderBySeqNoAsc(Integer statusNo);
}