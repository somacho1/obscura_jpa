package dev.jpa.obscura_jpa.product;

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
public class ProductDTO {

    private Long no;

    // 브랜드번호
    private Long bno;

    // 프론트 상품카드에서 사용할 브랜드명
    private String brandName;

    // 카테고리번호
    private Long cno;

    // 프론트 상품카드/목록에서 사용할 카테고리명
    private String categoryName;

    private String name;
    private String detail;

    // 상품 정가
    private Long price;

    private Integer discountRate;

    // 할인 적용 후 실제 판매가격
    private Long salePrice;

    // 노출중인 MAIN 대표 이미지 URL
    private String mainImageUrl;

    private Integer statusNo;
    private LocalDateTime cdate;
}