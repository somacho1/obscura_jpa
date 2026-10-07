package dev.jpa.obscura_jpa.product;

import java.time.LocalDateTime;

import dev.jpa.obscura_jpa.brand.Brand;
import dev.jpa.obscura_jpa.category.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PRODUCT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq_generator")
    @SequenceGenerator(name = "product_seq_generator", sequenceName = "PRODUCT_SEQ", allocationSize = 1)
    @Column(name = "NO")
    private Long no;
    
     // 모든 상품에 필수인 고유 상품 코드
    @Column(name = "CODE", nullable = false, length = 50)
    private String code;

    // PRODUCT.BNO → BRAND.NO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BNO", nullable = false)
    private Brand brand;

    // PRODUCT.CNO → CATEGORY.NO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CNO", nullable = false)
    private Category category;

    @Column(name = "NAME", nullable = false, length = 200)
    private String name;

    // 상세페이지 INFO 탭
    @Column(name = "DETAIL", length = 2000)
    private String detail;

    // 상세페이지 SIZE 탭
    // Oracle CLOB에 사이즈 가이드 JSON 문자열 저장
    @Lob
    @Column(name = "SIZEDETAIL")
    private String sizeDetail;

    @Column(name = "PRICE", nullable = false)
    private Long price;

    @Column(name = "DISCOUNTRATE", nullable = false)
    private Integer discountRate;

    @Column(name = "STATUSNO", nullable = false)
    private Integer statusNo;
    
 // 관리자 선정 상품 여부
    @Builder.Default
    @Column(name = "MDPICKYN", nullable = false, length = 1)
    private String mdPickYn = "N";

    // 추천 상품 표시 순서
    @Builder.Default
    @Column(name = "MDSEQNO", nullable = false)
    private Integer mdSeqNo = 0;

    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}