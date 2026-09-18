package dev.jpa.obscura_jpa.order;

import java.time.LocalDateTime;

import dev.jpa.obscura_jpa.member.ObMember;
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
@Table(name = "ORDERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "orders_seq_generator"
    )
    @SequenceGenerator(
        name = "orders_seq_generator",
        sequenceName = "ORDERS_SEQ",
        allocationSize = 1
    )
    @Column(name = "NO")
    private Long no;

    // ORDERS.MNO → OBMEMBER.NO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MNO", nullable = false)
    private ObMember member;

    // 주문 전체 금액
    @Column(name = "TOTALPRICE", nullable = false)
    private Long totalPrice;

    /*
     * 주문상태
     * 0 주문취소
     * 1 결제대기
     * 2 결제완료
     * 3 배송준비
     * 4 배송중
     * 5 배송완료
     */
    @Column(name = "STATUSNO", nullable = false)
    private Integer statusNo;

    /*
     * 취소상태
     * 0 취소없음
     * 1 부분취소
     * 2 전체취소
     */
    @Column(name = "CANCELSTATUSNO", nullable = false)
    private Integer cancelStatusNo;

    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}