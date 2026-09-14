package dev.jpa.obscura_jpa.memberaddress;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원 배송지 DTO
 *
 * 회원 배송지 등록, 조회, 수정 시
 * Controller와 Service 사이에서 데이터를 전달한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberAddressDTO {

    /** 배송지번호 */
    private Long no;

    /** 회원번호 */
    private Long mno;

    /** 배송지명: 집, 회사 등 */
    private String addressName;

    /** 받는 사람 */
    private String receiver;

    /** 배송 연락처 */
    private String phone;

    /** 우편번호 */
    private String zipcode;

    /** 기본주소 */
    private String address1;

    /** 상세주소 */
    private String address2;

    /** 기본배송지 여부: Y / N */
    private String defaultYn;

    /** 배송지 등록일시 */
    private LocalDateTime cdate;
}