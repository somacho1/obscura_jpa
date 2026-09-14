package dev.jpa.obscura_jpa.member;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원 Entity
 * Oracle의 OBMEMBER 테이블과 매핑
 */
@Entity
@Table(name = "OBMEMBER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObMember {

    /** 회원번호 - OBMEMBER_SEQ 사용 */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "obmember_seq_generator")
    @SequenceGenerator(name = "obmember_seq_generator", sequenceName = "OBMEMBER_SEQ", allocationSize = 1)
    @Column(name = "NO")
    private Long no;

    /** 로그인 아이디 */
    @Column(name = "ID", nullable = false, unique = true, length = 50)
    private String id;

    /** 이메일 */
    @Column(name = "EMAIL", nullable = false, unique = true, length = 100)
    private String email;

    /** 비밀번호 - 추후 암호화된 값 저장 */
    @Column(name = "PASSWORD", nullable = false, length = 255)
    private String password;

    /** 회원 이름 */
    @Column(name = "NAME", nullable = false, length = 50)
    private String name;

    /** 전화번호 */
    @Column(name = "PHONE", nullable = false, length = 20)
    private String phone;

    /** 회원 권한: ADMIN / USER */
    @Column(name = "ROLE", nullable = false, length = 20)
    private String role;

    /** 회원 상태: 0 탈퇴 / 1 정상 / 2 정지 / 3 휴면 */
    @Column(name = "STATUSNO", nullable = false)
    private Integer statusNo;

    /** 마지막 로그인 일시 */
    @Column(name = "LASTLOGIN")
    private LocalDateTime lastLogin;

    /** 회원 가입일시 */
    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}