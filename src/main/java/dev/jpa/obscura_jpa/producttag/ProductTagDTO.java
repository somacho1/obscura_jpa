package dev.jpa.obscura_jpa.producttag;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PRODUCTTAG 요청/응답 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductTagDTO {

    private Long no;

    // 상품번호
    private Long pno;

    // AI 스타일 태그
    private String tag;

    // AI 분석 점수 0 ~ 1
    private BigDecimal score;

    private LocalDateTime cdate;
}