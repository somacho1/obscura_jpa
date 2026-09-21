package dev.jpa.obscura_jpa.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상품 상세페이지에서 사용할 옵션 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailOptionDTO {

    // 상품옵션번호
    private Long optionNo;

    // 색상
    private String color;

    // 사이즈
    private String sizeValue;

    // 품절 여부
    private boolean soldOut;
}