package dev.jpa.obscura_jpa.orderitem;

import java.time.LocalDateTime;

import dev.jpa.obscura_jpa.order.Order;
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
@Table(name = "ORDERITEM")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "orderitem_seq_generator"
    )
    @SequenceGenerator(
        name = "orderitem_seq_generator",
        sequenceName = "ORDERITEM_SEQ",
        allocationSize = 1
    )
    @Column(name = "NO")
    private Long no;

    // ORDERITEM.ORDNO → ORDERS.NO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDNO", nullable = false)
    private Order order;

    // ORDERITEM.PONO → PRODUCTOPTION.NO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PONO", nullable = false)
    private ProductOption productOption;

    // 아래 값들은 주문 당시 상품정보 스냅샷
    @Column(name = "PRODUCTNAME", nullable = false)
    private String productName;

    @Column(name = "COLOR")
    private String color;

    @Column(name = "SIZEVALUE")
    private String sizeValue;

    // 주문 당시 실제 구매 단가
    @Column(name = "PRICE", nullable = false)
    private Long price;

    @Column(name = "QTY", nullable = false)
    private Long qty;

    /*
     * 0 주문취소
     * 1 상품준비
     * 2 배송중
     * 3 배송완료
     */
    @Column(name = "STATUSNO", nullable = false)
    private Integer statusNo;

    @Column(name = "CANCELQTY", nullable = false)
    private Long cancelQty;

    @Column(name = "CANCELREASON")
    private String cancelReason;

    @Column(name = "CANCELDATE")
    private LocalDateTime cancelDate;

    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}