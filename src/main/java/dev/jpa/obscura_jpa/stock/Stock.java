package dev.jpa.obscura_jpa.stock;

import java.time.LocalDateTime;

import dev.jpa.obscura_jpa.productoption.ProductOption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "STOCK")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "stock_seq_generator"
    )
    @SequenceGenerator(
        name = "stock_seq_generator",
        sequenceName = "STOCK_SEQ",
        allocationSize = 1
    )
    @Column(name = "NO")
    private Long no;

    // STOCK.PONO → PRODUCTOPTION.NO
    // PONO가 UNIQUE이므로 하나의 옵션에는 하나의 STOCK만 존재
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PONO", nullable = false, unique = true)
    private ProductOption productOption;

    // 현재 재고수량
    @Column(name = "QTY", nullable = false)
    private Long qty;

    // 재고 최종 수정일시
    @Column(name = "UDATE", nullable = false)
    private LocalDateTime udate;
}