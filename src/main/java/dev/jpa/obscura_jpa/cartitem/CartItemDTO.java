package dev.jpa.obscura_jpa.cartitem;

import java.time.LocalDateTime;

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
public class CartItemDTO {

    private Long no;

    // 회원번호
    // 장바구니 담기 요청 시 사용
    private Long mno;

    // 장바구니번호
    private Long cartno;

    // 상품옵션번호
    private Long pono;

    // 수량
    private Long qty;

    private LocalDateTime cdate;
}