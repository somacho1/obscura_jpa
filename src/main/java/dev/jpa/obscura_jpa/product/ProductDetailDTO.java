package dev.jpa.obscura_jpa.product;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상품 상세페이지 전용 DTO
 *
 * PRODUCT의 기본정보와
 * PRODUCTIMAGE의 이미지,
 * PRODUCTOPTION + STOCK을 이용한 옵션 정보를
 * 하나의 응답으로 묶어서 프론트에 전달한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailDTO {

    // PRODUCT.NO - 상품번호
    private Long no;

    // BRAND.NO - 브랜드번호
    private Long bno;

    // 브랜드명
    private String brandName;

    // CATEGORY.NO - 카테고리번호
    private Long cno;

    // 카테고리명
    private String categoryName;

    // 상품명
    private String name;

    // 상품 설명
    private String detail;

    // 정가
    private Long price;

    // 할인율
    private Integer discountRate;

    // 할인 적용 판매가격
    private Long salePrice;

    // MAIN 대표 이미지
    private String mainImageUrl;

    // SUB 이미지 목록
    private List<String> subImages;

    // DETAIL 이미지 목록
    private List<String> detailImages;

    // 색상 / 사이즈 / 품절여부
    private List<ProductDetailOptionDTO> options;

    // 상품 상태
    private Integer statusNo;
}