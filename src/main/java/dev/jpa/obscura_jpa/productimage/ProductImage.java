package dev.jpa.obscura_jpa.productimage;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PRODUCTIMAGE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "productimage_seq_generator"
    )
    @SequenceGenerator(
        name = "productimage_seq_generator",
        sequenceName = "PRODUCTIMAGE_SEQ",
        allocationSize = 1
    )
    @Column(name = "NO")
    private Long no;

    // PRODUCTIMAGE.PNO → PRODUCT.NO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PNO", nullable = false)
    private Product product;

    @Column(name = "IMAGEURL", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "IMAGETYPE", nullable = false, length = 20)
    private String imageType;

    @Column(name = "DISPLAYYN", nullable = false, length = 1)
    private String displayYn;

    @Column(name = "SEQNO", nullable = false)
    private Integer seqNo;

    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}