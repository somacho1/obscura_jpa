package dev.jpa.obscura_jpa.deliveryitem;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DELIVERYITEM 요청/응답 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class DeliveryItemDTO {

    private Long no;

    // 배송번호
    private Long dno;

    // 주문상품번호
    private Long oino;

    // 배송수량
    private Integer qty;

    private LocalDateTime cdate;
}