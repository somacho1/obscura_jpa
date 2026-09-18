package dev.jpa.obscura_jpa.delivery;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DELIVERY 요청/응답 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class DeliveryDTO {

    private Long no;

    // 주문번호
    private Long ordno;

    // 회원배송지번호 - 직접입력 시 null 가능
    private Long madno;

    private String receiver;

    private String phone;

    private String zipcode;

    private String address1;

    private String address2;

    private String company;

    private String trackingNo;

    // 0 배송준비 / 1 배송중 / 2 배송완료
    private Integer statusNo;

    private LocalDateTime shipDate;

    private LocalDateTime deliveryDate;

    private LocalDateTime cdate;
}