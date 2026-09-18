package dev.jpa.obscura_jpa.order;

import java.time.LocalDateTime;
import java.util.List;

import dev.jpa.obscura_jpa.orderitem.OrderItemDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {

    private Long no;

    // 주문 요청 시 회원번호
    private Long mno;

    private Long totalPrice;

    private Integer statusNo;

    private Integer cancelStatusNo;

    private LocalDateTime cdate;

    // 주문 상세 조회 시 주문상품 목록
    private List<OrderItemDTO> items;
}