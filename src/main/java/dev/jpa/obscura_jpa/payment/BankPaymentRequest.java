package dev.jpa.obscura_jpa.payment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 무통장입금 신청 요청입니다.
// 결제금액과 계좌정보는 클라이언트가 전달하지 않고 서버 기준으로 처리합니다.
@Getter
@Setter
@NoArgsConstructor
public class BankPaymentRequest {

    // 기존 회원번호 전달 방식입니다. 인증 적용 후 서버의 로그인 정보로 대체합니다.
    private Long mno;

    // 입금할 주문번호입니다.
    private Long ordno;

    // 실제 입금 시 사용할 입금자명입니다.
    private String depositor;
}