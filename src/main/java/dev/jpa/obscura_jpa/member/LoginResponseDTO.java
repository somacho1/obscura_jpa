package dev.jpa.obscura_jpa.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로그인 성공 응답 DTO
 *
 * 로그인 성공 후 React에서 필요한
 * 최소 회원정보만 전달한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    /** 회원번호 */
    private Long no;

    /** 로그인 아이디 */
    private String id;

    /** 회원 이름 */
    private String name;

    /** 이메일 */
    private String email;

    /** 회원 권한 */
    private String role;
}