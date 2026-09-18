package dev.jpa.obscura_jpa.delivery;

import java.time.LocalDateTime;

import dev.jpa.obscura_jpa.memberaddress.MemberAddress;
import dev.jpa.obscura_jpa.order.Order;
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
 * 배송 Entity.
 *
 * ORDERS 1 : N DELIVERY
 * 부분배송을 고려하여 하나의 주문에 여러 배송이 생성될 수 있다.
 *
 * STATUSNO
 * 0 : 배송준비
 * 1 : 배송중
 * 2 : 배송완료
 */
@Entity
@Table(name = "DELIVERY")
@Getter
@Setter
@NoArgsConstructor
@SequenceGenerator(
    name = "deliverySeqGenerator",
    sequenceName = "DELIVERY_SEQ",
    allocationSize = 1
)
public class Delivery {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "deliverySeqGenerator"
    )
    @Column(name = "NO")
    private Long no;

    // DELIVERY.ORDNO → ORDERS.NO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDNO", nullable = false)
    private Order order;

    /*
     * DELIVERY.MADNO → MEMBERADDRESS.NO
     *
     * 회원배송지를 이용한 경우 연결한다.
     * 직접입력 배송지는 NULL 가능.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MADNO")
    private MemberAddress memberAddress;

    // 아래 주소정보는 주문 당시 배송지 스냅샷
    @Column(name = "RECEIVER", nullable = false, length = 50)
    private String receiver;

    @Column(name = "PHONE", nullable = false, length = 20)
    private String phone;

    @Column(name = "ZIPCODE", nullable = false, length = 10)
    private String zipcode;

    @Column(name = "ADDRESS1", nullable = false, length = 255)
    private String address1;

    @Column(name = "ADDRESS2", length = 255)
    private String address2;

    @Column(name = "COMPANY", length = 50)
    private String company;

    @Column(name = "TRACKINGNO", length = 100)
    private String trackingNo;

    @Column(name = "STATUSNO", nullable = false)
    private Integer statusNo;

    @Column(name = "SHIPDATE")
    private LocalDateTime shipDate;

    @Column(name = "DELIVERYDATE")
    private LocalDateTime deliveryDate;

    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}