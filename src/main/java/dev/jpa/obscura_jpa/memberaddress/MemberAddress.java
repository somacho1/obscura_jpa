package dev.jpa.obscura_jpa.memberaddress;

import java.time.LocalDateTime;

import dev.jpa.obscura_jpa.member.ObMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원 배송지 Entity
 *
 * Oracle MEMBERADDRESS 테이블과 매핑된다.
 * 회원 1명은 여러 개의 배송지를 가질 수 있다.
 */
@Entity
@Table(name = "MEMBERADDRESS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberAddress {

    /** 배송지번호 */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "memberaddress_seq_generator")
    @SequenceGenerator(
        name = "memberaddress_seq_generator",
        sequenceName = "MEMBERADDRESS_SEQ",
        allocationSize = 1
    )
    @Column(name = "NO")
    private Long no;

    /**
     * 회원
     *
     * MEMBERADDRESS.MNO
     *      ↓
     * OBMEMBER.NO
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MNO", nullable = false)
    private ObMember member;

    /** 배송지명: 집, 회사 등 */
    @Column(name = "ADDRESSNAME", nullable = false, length = 50)
    private String addressName;

    /** 받는 사람 */
    @Column(name = "RECEIVER", nullable = false, length = 50)
    private String receiver;

    /** 배송 연락처 */
    @Column(name = "PHONE", nullable = false, length = 20)
    private String phone;

    /** 우편번호 */
    @Column(name = "ZIPCODE", nullable = false, length = 10)
    private String zipcode;

    /** 기본주소 */
    @Column(name = "ADDRESS1", nullable = false, length = 255)
    private String address1;

    /** 상세주소 */
    @Column(name = "ADDRESS2", length = 255)
    private String address2;

    /** 기본배송지 여부: Y / N */
    @Column(name = "DEFAULTYN", nullable = false, length = 1)
    private String defaultYn;

    /** 배송지 등록일시 */
    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}