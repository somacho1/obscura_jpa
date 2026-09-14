package dev.jpa.obscura_jpa.member;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원 Repository
 *
 * OBMEMBER 테이블의 회원 조회 및
 * 회원가입/수정 시 중복 검사 기능을 담당한다.
 */
public interface ObMemberRepository extends JpaRepository<ObMember, Long> {

    /** 아이디 중복 검사 */
    boolean existsById(String id);

    /** 이메일 중복 검사 */
    boolean existsByEmail(String email);

    /**
     * 회원 수정 시 이메일 중복 검사
     *
     * 현재 회원번호(no)는 제외하고
     * 동일한 이메일을 사용하는 다른 회원이 있는지 확인한다.
     */
    boolean existsByEmailAndNoNot(String email, Long no);

    /** 로그인 아이디로 회원 조회 */
    Optional<ObMember> findById(String id);

    /** 회원번호 내림차순으로 전체 회원 조회 */
    List<ObMember> findAllByOrderByNoDesc();
}