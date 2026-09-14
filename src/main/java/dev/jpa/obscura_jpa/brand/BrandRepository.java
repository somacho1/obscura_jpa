package dev.jpa.obscura_jpa.brand;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 브랜드 Repository
 *
 * BRAND 테이블의 브랜드 조회,
 * 브랜드명 중복검사,
 * 활성 브랜드 조회 기능을 담당한다.
 */
public interface BrandRepository extends JpaRepository<Brand, Long> {

    /**
     * 브랜드명 중복 검사
     *
     * true  : 이미 존재
     * false : 사용 가능
     */
    boolean existsByName(String name);

    /**
     * 브랜드명으로 브랜드 조회
     */
    Optional<Brand> findByName(String name);

    /**
     * 전체 브랜드 최신 등록순 조회
     */
    List<Brand> findAllByOrderByNoDesc();

    /**
     * 활성 브랜드만 조회
     *
     * STATUSNO = 1
     */
    List<Brand> findAllByStatusNoOrderByNoDesc(Integer statusNo);
}