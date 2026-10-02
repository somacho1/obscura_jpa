package dev.jpa.obscura_jpa.order;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 주문자가 전달하는 배송지 정보입니다.
// 택배사·송장번호·배송상태는 관리자가 처리하므로 요청에 포함하지 않습니다.
@Getter
@Setter
@NoArgsConstructor
public class OrderDeliveryDTO {

    // 저장된 회원 배송지를 선택한 경우에만 전달합니다. 직접 입력하면 null입니다.
    private Long madno;

    private String receiver; // 받는 사람
    private String phone;    // 배송 연락처
    private String zipcode;  // 우편번호
    private String address1; // 기본주소
    private String address2; // 상세주소
}