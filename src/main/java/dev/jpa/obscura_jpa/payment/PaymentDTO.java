package dev.jpa.obscura_jpa.payment;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PAYMENT 요청/응답 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class PaymentDTO {

    private Long no;

    // 주문번호
    private Long ordno;

    // TOSS / BANK
    private String method;

    // 결제금액
    private Long amount;

    // 0 대기 / 1 완료 / 2 부분취소 / 3 전체취소 / 4 실패
    private Integer statusNo;

    // Toss 결제키
    private String paymentKey;

    // 무통장입금 입금자명
    private String depositor;

    private LocalDateTime approveDate;

    private LocalDateTime cancelDate;

    private LocalDateTime cdate;
}