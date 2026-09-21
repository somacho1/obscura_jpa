package dev.jpa.obscura_jpa.cartitem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 장바구니 화면 조회용 DTO
 *
 * CARTITEM뿐만 아니라
 * 상품 / 브랜드 / 옵션 / 이미지 정보를
 * React 장바구니 화면에서 사용할 수 있도록 함께 전달한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDetailDTO {

    // CARTITEM.NO
    private Long cartItemNo;

    // PRODUCT.NO
    private Long productNo;

    // PRODUCTOPTION.NO
    private Long optionNo;

    // 브랜드명
    private String brandName;

    // 상품명
    private String productName;

    // 대표 이미지
    private String mainImageUrl;

    // 옵션
    private String color;
    private String sizeValue;

    // 정상가격
    private Long price;

    // 할인율
    private Integer discountRate;

    // 실제 판매가격
    private Long salePrice;

    // 장바구니 수량
    private Long qty;

    // 현재 재고
    private Long stockQty;

    // 품절 여부
    private boolean soldOut;
}