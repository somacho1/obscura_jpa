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

    // 프론트에서 브랜드번호만 전달
    private Long bno;

    // 프론트에서 카테고리번호만 전달
    private Long cno;

    private String name;
    private String detail;
    private Long price;
    private Integer discountRate;
    private Integer statusNo;
    private LocalDateTime cdate;
}