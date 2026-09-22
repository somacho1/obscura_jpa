package dev.jpa.obscura_jpa.product;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 선택 상품 일괄 할인 요청 DTO
 *
 * 여러 상품번호와 하나의 할인율을 받아
 * 선택한 상품에 동일한 할인율을 적용할 때 사용한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductBulkDiscountDTO {

    // 할인 적용 대상 상품번호
    private List<Long> productNos;

    // 선택 상품에 적용할 할인율
    private Integer discountRate;
}