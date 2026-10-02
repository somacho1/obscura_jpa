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
    
    // 선택 주문할 CARTITEM 번호 목록입니다. 상품번호·옵션번호와 구분합니다.
    private List<Long> cartItemNos;
    
    // 이번 주문에 사용할 배송지입니다.
    private OrderDeliveryDTO delivery;

    // 주문 요청 시 회원번호
    private Long mno;

    private Long totalPrice;
    
    // 응답용 배송비입니다. 주문 생성 시 클라이언트 값은 사용하지 않습니다.
    private Long shippingFee;

    private Integer statusNo;

    private Integer cancelStatusNo;

    private LocalDateTime cdate;

    // 주문 상세 조회 시 주문상품 목록
    private List<OrderItemDTO> items;
}