package dev.jpa.obscura_jpa.cartitem;

import java.time.LocalDateTime;

import dev.jpa.obscura_jpa.cart.Cart;
import dev.jpa.obscura_jpa.productoption.ProductOption;
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
@Table(name = "CARTITEM")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "cartitem_seq_generator"
    )
    @SequenceGenerator(
        name = "cartitem_seq_generator",
        sequenceName = "CARTITEM_SEQ",
        allocationSize = 1
    )
    @Column(name = "NO")
    private Long no;

    // CARTITEM.CARTNO → CART.NO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CARTNO", nullable = false)
    private Cart cart;

    // CARTITEM.PONO → PRODUCTOPTION.NO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PONO", nullable = false)
    private ProductOption productOption;

    @Column(name = "QTY", nullable = false)
    private Long qty;

    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}