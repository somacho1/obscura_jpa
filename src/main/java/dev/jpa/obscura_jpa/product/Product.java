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
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "product_seq_generator"
    )
    @SequenceGenerator(
        name = "product_seq_generator",
        sequenceName = "PRODUCT_SEQ",
        allocationSize = 1
    )
    @Column(name = "NO")
    private Long no;

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

    @Column(name = "DETAIL", length = 2000)
    private String detail;

    @Column(name = "PRICE", nullable = false)
    private Long price;

    @Column(name = "DISCOUNTRATE", nullable = false)
    private Integer discountRate;

    @Column(name = "STATUSNO", nullable = false)
    private Integer statusNo;

    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}