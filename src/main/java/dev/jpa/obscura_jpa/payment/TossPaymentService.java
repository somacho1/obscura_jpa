package dev.jpa.obscura_jpa.payment;

import java.util.Map;

import org.springframework.stereotype.Service;

import dev.jpa.obscura_jpa.payment.TossPaymentTransactionService.PreparedPayment;

// 결제 승인 전체 순서를 조정합니다.
// DB 저장 트랜잭션은 TossPaymentTransactionService에서 각각 처리합니다.
@Service
public class TossPaymentService {

    private final TossPaymentTransactionService transactionService;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentService paymentService;

    public TossPaymentService(
        TossPaymentTransactionService transactionService,
        TossPaymentClient tossPaymentClient,
        PaymentService paymentService
    ) {
        this.transactionService = transactionService;
        this.tossPaymentClient = tossPaymentClient;
        this.paymentService = paymentService;
    }

    // 이 메서드에는 @Transactional을 붙이지 않습니다.
    // 승인 요청 정보를 먼저 커밋한 뒤 Toss를 호출해야 재시도 정보를 보존할 수 있습니다.
    public PaymentDTO confirm(TossConfirmRequest request) {

        // 1. 회원·주문 상태·금액을 검증하고 결제키와 멱등키를 저장합니다.
        PreparedPayment prepared = transactionService.prepare(request);

        // 이미 완료된 동일 요청은 Toss를 다시 호출하지 않습니다.
        if (prepared.alreadyApproved()) {
            return paymentService.findByOrder(prepared.orderNo());
        }

        // 2. 저장된 값으로 Toss 승인 API를 호출합니다.
        // 동일 요청의 재시도에는 DB에 저장된 멱등키를 그대로 사용합니다.
        Map<String, Object> result = tossPaymentClient.confirm(
            prepared.paymentKey(),
            prepared.orderId(),
            prepared.amount(),
            prepared.confirmKey()
        );

        // 3. 응답을 검증하고 PAYMENT·ORDERS를 결제 완료 상태로 저장합니다.
        return transactionService.complete(prepared, result);
    }
}