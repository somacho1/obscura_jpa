package dev.jpa.obscura_jpa.cart;

import java.time.LocalDateTime;

import dev.jpa.obscura_jpa.member.ObMember;
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
@Table(name = "CART")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "cart_seq_generator"
    )
    @SequenceGenerator(
        name = "cart_seq_generator",
        sequenceName = "CART_SEQ",
        allocationSize = 1
    )
    @Column(name = "NO")
    private Long no;

    // CART.MNO → OBMEMBER.NO
    // 회원 한 명당 CART 하나이므로 OneToOne
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MNO", nullable = false, unique = true)
    private ObMember member;

    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}