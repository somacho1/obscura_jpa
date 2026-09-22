package dev.jpa.obscura_jpa.brand;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 브랜드 DTO
 *
 * 브랜드 기본정보와 관리자 화면에서 필요한
 * 상품 수, 할인 상품 수를 전달한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandDTO {

    /** 브랜드번호 */
    private Long no;

    /** 브랜드명 */
    private String name;

    /** 브랜드 로고 이미지 URL */
    private String logoUrl;

    /** 브랜드 대표 이미지 URL */
    private String visualUrl;

    /** 브랜드 설명 */
    private String detail;

    /** 브랜드 상태: 0 비활성 / 1 활성 */
    private Integer statusNo;

    /** 브랜드 등록일시 */
    private LocalDateTime cdate;

    /** 해당 브랜드 전체 상품 수 */
    private Long productCount;

    /** 해당 브랜드 할인 상품 수 */
    private Long saleProductCount;
}