package dev.jpa.obscura_jpa.member;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원 DTO
 *
 * Controller와 Service 사이에서
 * 회원 데이터를 전달하기 위해 사용한다.
 *
 * Entity를 API 요청/응답에 직접 사용하지 않고
 * DTO를 통해 필요한 회원 데이터를 전달한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObMemberDTO {

    /** 회원번호 */
    private Long no;

    /** 로그인 아이디 */
    private String id;

    /** 이메일 */
    private String email;

    /** 비밀번호 */
    private String password;

    /** 회원 이름 */
    private String name;

    /** 전화번호 */
    private String phone;

    /** 회원 권한: ADMIN / USER */
    private String role;

    /** 회원 상태: 0 탈퇴 / 1 정상 / 2 정지 / 3 휴면 */
    private Integer statusNo;

    /** 마지막 로그인 일시 */
    private LocalDateTime lastLogin;

    /** 회원 가입일시 */
    private LocalDateTime cdate;
}