package dev.jpa.obscura_jpa.deliveryitem;

import java.time.LocalDateTime;

import dev.jpa.obscura_jpa.delivery.Delivery;
import dev.jpa.obscura_jpa.orderitem.OrderItem;
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
 * 배송상품 Entity.
 *
 * DELIVERY와 ORDERITEM을 연결한다.
 *
 * 예)
 * ORDERITEM 주문수량 2개
 *
 * DELIVERY #1 → 1개
 * DELIVERY #2 → 1개
 *
 * 와 같은 부분배송을 처리할 수 있다.
 */
@Entity
@Table(name = "DELIVERYITEM")
@Getter
@Setter
@NoArgsConstructor
@SequenceGenerator(
    name = "deliveryItemSeqGenerator",
    sequenceName = "DELIVERYITEM_SEQ",
    allocationSize = 1
)
public class DeliveryItem {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "deliveryItemSeqGenerator"
    )
    @Column(name = "NO")
    private Long no;

    // DELIVERYITEM.DNO → DELIVERY.NO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DNO", nullable = false)
    private Delivery delivery;

    // DELIVERYITEM.OINO → ORDERITEM.NO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OINO", nullable = false)
    private OrderItem orderItem;

    // 이번 배송에 포함되는 수량
    @Column(name = "QTY", nullable = false)
    private Integer qty;

    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}