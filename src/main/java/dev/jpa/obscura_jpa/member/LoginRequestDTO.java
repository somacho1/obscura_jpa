package dev.jpa.obscura_jpa.member;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로그인 요청 DTO
 *
 * React에서 전달받은
 * 아이디와 비밀번호를 담는다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    /** 로그인 아이디 */
    private String id;

    /** 비밀번호 */
    private String password;
}