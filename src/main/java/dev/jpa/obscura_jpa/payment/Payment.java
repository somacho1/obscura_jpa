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
    
 // Toss 결제창과 승인 API에서 사용하는 주문 식별자입니다.
    @Column(name = "TOSSORDERID", length = 64, unique = true)
    private String tossOrderId;

    // 첫 승인 시 생성해 저장하고, 동일 요청을 재시도할 때 그대로 사용합니다.
    @Column(name = "CONFIRMKEY", length = 36, unique = true)
    private String confirmKey;

    @Column(name = "DEPOSITOR", length = 50)
    private String depositor;

    @Column(name = "APPROVEDATE")
    private LocalDateTime approveDate;

    @Column(name = "CANCELDATE")
    private LocalDateTime cancelDate;

    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
    
 // 취소 요청을 재시도해도 같은 키와 사유를 사용합니다.
    @Column(name = "CANCELKEY", length = 36)
    private String cancelKey;

    @Column(name = "CANCELREASON", length = 200)
    private String cancelReason;
    
 // 무통장입금 환불 요청 시 저장합니다.
    @Column(name = "REFUNDBANK", length = 50)
    private String refundBank;

    @Column(name = "REFUNDACCOUNT", length = 30)
    private String refundAccount;

    @Column(name = "REFUNDHOLDER", length = 50)
    private String refundHolder;
}