package dev.jpa.obscura_jpa.producttag;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.jpa.obscura_jpa.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 상품 스타일 태그 Entity.
 *
 * PRODUCT 1 : N PRODUCTTAG
 *
 * 예)
 * 상품 #1
 * - streetwear 0.9200
 * - utility    0.8700
 * - minimal    0.6100
 */
@Entity
@Table(name = "PRODUCTTAG")
@Getter
@Setter
@NoArgsConstructor
@SequenceGenerator(
    name = "productTagSeqGenerator",
    sequenceName = "PRODUCTTAG_SEQ",
    allocationSize = 1
)
public class ProductTag {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "productTagSeqGenerator"
    )
    @Column(name = "NO")
    private Long no;

    // PRODUCTTAG.PNO → PRODUCT.NO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PNO", nullable = false)
    private Product product;

    // AI가 분석한 스타일 태그
    @Column(name = "TAG", nullable = false, length = 50)
    private String tag;

    // AI 분석 점수 0 ~ 1
    @Column(
        name = "SCORE",
        nullable = false,
        precision = 5,
        scale = 4
    )
    private BigDecimal score;

    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}