package dev.jpa.obscura_jpa.payment;

import java.time.LocalDateTime;

import dev.jpa.obscura_jpa.order.Order;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 결제 Entity.
 *
 * 주문 1건당 결제정보는 최대 1건만 생성한다.
 *
 * METHOD
 * TOSS : 토스페이먼츠
 * BANK : 무통장입금
 *
 * STATUSNO
 * 0 : 결제대기
 * 1 : 결제완료
 * 2 : 부분취소
 * 3 : 전체취소
 * 4 : 결제실패
 */
@Entity
@Table(name = "PAYMENT")
@Getter
@Setter
@NoArgsConstructor
@SequenceGenerator(
    name = "paymentSeqGenerator",
    sequenceName = "PAYMENT_SEQ",
    allocationSize = 1
)
public class Payment {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "paymentSeqGenerator"
    )
    @Column(name = "NO")
    private Long no;

    /**
     * 결제가 속한 주문.
     * PAYMENT.ORDNO → ORDERS.NO
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDNO", nullable = false, unique = true)
    private Order order;

    @Column(name = "METHOD", nullable = false, length = 20)
    private String method;

    @Column(name = "AMOUNT", nullable = false)
    private Long amount;

    @Column(name = "STATUSNO", nullable = false)
    private Integer statusNo;

    @Column(name = "PAYMENTKEY", length = 200)
    private String paymentKey;

    @Column(name = "DEPOSITOR", length = 50)
    private String depositor;

    @Column(name = "APPROVEDATE")
    private LocalDateTime approveDate;

    @Column(name = "CANCELDATE")
    private LocalDateTime cancelDate;

    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}