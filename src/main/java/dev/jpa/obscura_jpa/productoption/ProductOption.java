package dev.jpa.obscura_jpa.productoption;

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
@Table(name = "PRODUCTOPTION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOption {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "productoption_seq_generator"
    )
    @SequenceGenerator(
        name = "productoption_seq_generator",
        sequenceName = "PRODUCTOPTION_SEQ",
        allocationSize = 1
    )
    @Column(name = "NO")
    private Long no;

    // PRODUCTOPTION.PNO → PRODUCT.NO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PNO", nullable = false)
    private Product product;

    @Column(name = "COLOR", length = 50)
    private String color;

    @Column(name = "SIZEVALUE", length = 50)
    private String sizeValue;

    @Column(name = "USEYN", nullable = false, length = 1)
    private String useYn;

    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}