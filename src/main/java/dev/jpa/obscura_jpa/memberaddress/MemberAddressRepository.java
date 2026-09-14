package dev.jpa.obscura_jpa.memberaddress;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원 배송지 Repository
 *
 * MEMBERADDRESS 테이블의 배송지 조회 및
 * 기본배송지 조회 기능을 담당한다.
 */
public interface MemberAddressRepository extends JpaRepository<MemberAddress, Long> {

    /**
     * 특정 회원의 배송지 전체 조회
     *
     * 최신 등록순으로 조회한다.
     */
    List<MemberAddress> findAllByMemberNoOrderByNoDesc(Long mno);

    /**
     * 특정 회원의 기본배송지 조회
     *
     * DEFAULTYN = 'Y' 인 배송지를 조회한다.
     */
    Optional<MemberAddress> findByMemberNoAndDefaultYn(Long mno, String defaultYn);

    /**
     * 특정 회원의 배송지 개수 조회
     *
     * 첫 배송지 등록 시 자동으로 기본배송지로
     * 설정할 때 사용할 수 있다.
     */
    long countByMemberNo(Long mno);
}