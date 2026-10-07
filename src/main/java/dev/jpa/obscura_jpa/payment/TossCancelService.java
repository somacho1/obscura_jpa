package dev.jpa.obscura_jpa.payment;

import java.util.Map;

import org.springframework.stereotype.Service;

import dev.jpa.obscura_jpa.order.OrderDTO;
import dev.jpa.obscura_jpa.order.OrderService;
import dev.jpa.obscura_jpa.payment.TossCancelTransactionService.PreparedCancel;

@Service
public class TossCancelService {

    private final TossCancelTransactionService transactionService;
    private final TossPaymentClient tossPaymentClient;
    private final OrderService orderService;

    public TossCancelService(
        TossCancelTransactionService transactionService,
        TossPaymentClient tossPaymentClient,
        OrderService orderService
    ) {
        this.transactionService = transactionService;
        this.tossPaymentClient = tossPaymentClient;
        this.orderService = orderService;
    }

    // 외부 통신 중에는 DB 트랜잭션을 열어두지 않습니다.
    public OrderDTO cancel(TossCancelRequest request) {
        PreparedCancel prepared = transactionService.prepare(request);

        if (prepared.completed()) {
            return orderService.findByNo(prepared.orderNo());
        }

        Map<String, Object> result = tossPaymentClient.findPayment(prepared.paymentKey());
        transactionService.validateIdentity(prepared, result);

        // 앞선 요청이 Toss에서 성공했지만 DB 저장에 실패한 경우 바로 복구합니다.
        if (!"CANCELED".equals(result.get("status"))) {
            if (!"DONE".equals(result.get("status"))) {
                throw new IllegalStateException("현재 Toss 결제 상태에서는 전체 취소할 수 없습니다.");
            }

            // 이번 단계는 카드·간편결제만 지원합니다.
            Object method = result.get("method");
            if (!"카드".equals(method) && !"간편결제".equals(method)) {
                throw new IllegalStateException("현재는 카드·간편결제 전체 취소만 지원합니다.");
            }

            tossPaymentClient.cancelPayment(
                prepared.paymentKey(),
                prepared.reason(),
                prepared.cancelKey()
            );

            // 멱등 응답뿐 아니라 현재 결제 상태도 다시 확인합니다.
            result = tossPaymentClient.findPayment(prepared.paymentKey());
        }

        transactionService.complete(prepared, result);
        return orderService.findByNo(prepared.orderNo());
    }
}