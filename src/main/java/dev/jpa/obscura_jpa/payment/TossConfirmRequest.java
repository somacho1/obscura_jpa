package dev.jpa.obscura_jpa.payment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 결제 인증 후 React에서 백엔드로 전달하는 승인 요청입니다.
// 금액과 주문번호는 Service에서 DB 주문정보와 대조합니다.
@Getter
@Setter
@NoArgsConstructor
public class TossConfirmRequest {

    // 현재 회원번호: 기존 프로젝트의 회원번호 전달 방식에 맞춥니다.
    // 인증 기능 적용 후에는 서버에서 확인한 회원번호를 사용합니다.
    private Long mno;

    // Toss에서 전달받은 결제 식별키입니다.
    private String paymentKey;

    // Toss 결제창에 전달했던 주문 식별자입니다.
    // ORDERS.NO가 3이면 "OBSCURA_3" 형식으로 사용합니다.
    private String orderId;

    // 결제 인증 결과의 금액입니다.
    // 서버에 저장된 ORDERS.TOTALPRICE와 같아야 승인 요청을 진행합니다.
    private Long amount;
}