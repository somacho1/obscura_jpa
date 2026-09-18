package dev.jpa.obscura_jpa.wishlist;

import java.time.LocalDateTime;

import dev.jpa.obscura_jpa.member.ObMember;
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
@Table(name = "WISHLIST")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "wishlist_seq_generator"
    )
    @SequenceGenerator(
        name = "wishlist_seq_generator",
        sequenceName = "WISHLIST_SEQ",
        allocationSize = 1
    )
    @Column(name = "NO")
    private Long no;

    // WISHLIST.MNO → OBMEMBER.NO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MNO", nullable = false)
    private ObMember member;

    // WISHLIST.PNO → PRODUCT.NO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PNO", nullable = false)
    private Product product;

    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}