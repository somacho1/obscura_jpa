package dev.jpa.obscura_jpa.payment;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jpa.obscura_jpa.order.Order;
import dev.jpa.obscura_jpa.order.OrderRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

// DB 트랜잭션을 담당합니다. Toss 외부 API 호출은 별도 Service에서 진행합니다.
@Service
public class TossPaymentTransactionService {

  private final OrderRepository orderRepository;
  private final PaymentRepository paymentRepository;

  public TossPaymentTransactionService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
    this.orderRepository = orderRepository;
    this.paymentRepository = paymentRepository;
  }

  // 트랜잭션 종료 후에도 사용할 수 있도록 Entity 대신 필요한 값만 반환합니다.
  public record PreparedPayment(Long orderNo, String paymentKey, String orderId, Long amount, String confirmKey,
      boolean alreadyApproved) {
  }

  // 주문 검증과 승인 요청 정보 저장을 하나의 트랜잭션으로 처리합니다.
  @Transactional
  public PreparedPayment prepare(TossConfirmRequest request) {
    if (request == null)
      throw new IllegalArgumentException("결제 승인 정보가 필요합니다.");
    if (request.getMno() == null || request.getMno() <= 0) {
      throw new IllegalArgumentException("회원번호는 필수입니다.");
    }
    if (request.getPaymentKey() == null || request.getPaymentKey().isBlank()
        || request.getPaymentKey().length() > 200) {
      throw new IllegalArgumentException("올바른 결제키가 필요합니다.");
    }
    if (request.getOrderId() == null || request.getOrderId().length() > 64
        || !request.getOrderId().matches("OBSCURA_[1-9][0-9]*")) {
      throw new IllegalArgumentException("올바른 결제 주문번호가 필요합니다.");
    }
    if (request.getAmount() == null || request.getAmount() <= 0) {
      throw new IllegalArgumentException("결제금액은 0보다 커야 합니다.");
    }

    Long orderNo;
    try {
      orderNo = Long.valueOf(request.getOrderId().substring("OBSCURA_".length()));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("잘못된 주문번호입니다.");
    }

    // 취소 메서드와 동일한 주문 잠금을 사용해 승인 준비와 취소가 충돌하지 않게 합니다.
    Order order = orderRepository.findByNoForUpdate(orderNo)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

    if (!order.getMember().getNo().equals(request.getMno())) {
      throw new IllegalArgumentException("해당 회원의 주문이 아닙니다.");
    }

    // 브라우저에서 받은 금액을 DB에 저장된 최종 주문 금액과 비교합니다.
    if (!order.getTotalPrice().equals(request.getAmount())) {
      throw new IllegalArgumentException("주문 금액과 결제 요청 금액이 일치하지 않습니다.");
    }

    Payment payment = paymentRepository.findByOrderNo(orderNo).orElse(null);

    // 이미 승인된 동일 요청은 다시 승인하지 않고 완료된 결과를 사용합니다.
    if (payment != null && Integer.valueOf(1).equals(payment.getStatusNo())) {
      validateSameRequest(payment, request);
      if (!Integer.valueOf(2).equals(order.getStatusNo())) {
        throw new IllegalArgumentException("이미 처리된 결제입니다. 주문 상태를 확인해주세요.");
      }
      return toPreparedPayment(payment, true);
    }

    if (!Integer.valueOf(1).equals(order.getStatusNo())) {
      throw new IllegalArgumentException("결제 대기 주문만 결제할 수 있습니다.");
    }

    if (payment == null) {
      payment = new Payment();
      payment.setOrder(order);
      payment.setMethod("TOSS");
      payment.setAmount(order.getTotalPrice());
      payment.setStatusNo(0);
      payment.setCdate(LocalDateTime.now());
    } else {
      if (!"TOSS".equals(payment.getMethod()) || !Integer.valueOf(0).equals(payment.getStatusNo())) {
        throw new IllegalArgumentException("현재 결제정보로 승인 요청을 진행할 수 없습니다.");
      }
      if (!order.getTotalPrice().equals(payment.getAmount())) {
        throw new IllegalArgumentException("저장된 결제금액이 주문 금액과 다릅니다.");
      }
    }

    // 승인 요청이 이미 저장됐다면 결제키·주문번호를 바꾸지 않습니다.
    if (payment.getPaymentKey() != null) {
      validateSameRequest(payment, request);
      if (payment.getConfirmKey() == null) {
        throw new IllegalArgumentException("저장된 승인 요청 키를 확인해주세요.");
      }
    } else {
      payment.setPaymentKey(request.getPaymentKey());
      payment.setTossOrderId(request.getOrderId());
      payment.setConfirmKey(UUID.randomUUID().toString());
    }

    // 이 메서드가 정상 종료되어 커밋된 후 외부 승인 API를 호출합니다.
    paymentRepository.saveAndFlush(payment);
    return toPreparedPayment(payment, false);
  }

  // 재시도 요청이 처음 저장한 승인 요청과 같은지 확인합니다.
  private void validateSameRequest(Payment payment, TossConfirmRequest request) {
    if (!"TOSS".equals(payment.getMethod()) || !request.getPaymentKey().equals(payment.getPaymentKey())
        || !request.getOrderId().equals(payment.getTossOrderId()) || !request.getAmount().equals(payment.getAmount())) {
      throw new IllegalArgumentException("기존 결제 승인 요청과 다른 정보입니다.");
    }
  }

  // Toss 승인 결과를 검증하고 PAYMENT와 ORDERS를 함께 완료 처리합니다.
  @Transactional
  public PaymentDTO complete(PreparedPayment prepared, Map<String, Object> result) {
    if (prepared == null || result == null) {
      throw new IllegalArgumentException("결제 승인 결과가 필요합니다.");
    }

    Order order = orderRepository.findByNoForUpdate(prepared.orderNo())
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

    Payment payment = paymentRepository.findByOrderNo(prepared.orderNo())
        .orElseThrow(() -> new IllegalArgumentException("저장된 결제 요청이 없습니다."));

    // 외부 호출 전에 저장한 요청과 현재 결제정보가 같은지 확인합니다.
    if (!"TOSS".equals(payment.getMethod()) || !prepared.paymentKey().equals(payment.getPaymentKey())
        || !prepared.orderId().equals(payment.getTossOrderId()) || !prepared.amount().equals(payment.getAmount())
        || !prepared.confirmKey().equals(payment.getConfirmKey())) {
      throw new IllegalArgumentException("저장된 결제 요청 정보가 일치하지 않습니다.");
    }

    // Toss에서 받은 값도 저장된 주문번호·결제키·금액과 대조합니다.
    if (!prepared.paymentKey().equals(result.get("paymentKey")) || !prepared.orderId().equals(result.get("orderId"))) {
      throw new IllegalStateException("Toss 승인 결과의 결제정보가 일치하지 않습니다.");
    }

    long approvedAmount;
    try {
      Object value = result.get("totalAmount");
      if (!(value instanceof Number))
        throw new NumberFormatException();
      approvedAmount = new BigDecimal(value.toString()).longValueExact();
    } catch (ArithmeticException | NumberFormatException e) {
      throw new IllegalStateException("Toss 승인 금액을 확인할 수 없습니다.");
    }

    if (approvedAmount != prepared.amount().longValue() || !order.getTotalPrice().equals(prepared.amount())) {
      throw new IllegalStateException("Toss 승인 금액과 주문 금액이 일치하지 않습니다.");
    }

    // 인증만 끝난 상태나 가상계좌 입금 대기를 결제 완료로 저장하지 않습니다.
    if (!"DONE".equals(result.get("status"))) {
      throw new IllegalStateException("결제 완료 상태가 아닙니다. 결제 상태를 확인해주세요.");
    }

    // 동일 승인 결과가 다시 들어와도 승인일시와 주문 상태를 다시 변경하지 않습니다.
    if (Integer.valueOf(1).equals(payment.getStatusNo())) {
      return toPaymentDTO(payment);
    }

    if (!Integer.valueOf(0).equals(payment.getStatusNo()) || !Integer.valueOf(1).equals(order.getStatusNo())) {
      throw new IllegalStateException("현재 주문·결제 상태에서는 완료 처리할 수 없습니다.");
    }

    // Toss에서 전달한 실제 승인일시를 저장합니다.
    LocalDateTime approvedAt;
    try {
      Object value = result.get("approvedAt");
      if (!(value instanceof String text)) {
        throw new IllegalArgumentException();
      }
      approvedAt = OffsetDateTime.parse(text).atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul")).toLocalDateTime();
    } catch (java.time.DateTimeException | IllegalArgumentException e) {
      throw new IllegalStateException("Toss 결제 승인일시를 확인할 수 없습니다.");
    }

    payment.setStatusNo(1); // PAYMENT: 결제 완료
    payment.setApproveDate(approvedAt);
    order.setStatusNo(2); // ORDERS: 결제 완료

    // 같은 트랜잭션에서 변경 감지로 두 Entity를 저장합니다.
    return toPaymentDTO(payment);
  }

  // 결제 완료 결과를 기존 PaymentDTO 형식으로 반환합니다.
  private PaymentDTO toPaymentDTO(Payment payment) {
    PaymentDTO dto = new PaymentDTO();
    dto.setNo(payment.getNo());
    dto.setOrdno(payment.getOrder().getNo());
    dto.setMethod(payment.getMethod());
    dto.setAmount(payment.getAmount());
    dto.setStatusNo(payment.getStatusNo());
    dto.setPaymentKey(payment.getPaymentKey());
    dto.setDepositor(payment.getDepositor());
    dto.setApproveDate(payment.getApproveDate());
    dto.setCancelDate(payment.getCancelDate());
    dto.setCdate(payment.getCdate());
    return dto;
  }

  private PreparedPayment toPreparedPayment(Payment payment, boolean alreadyApproved) {
    return new PreparedPayment(payment.getOrder().getNo(), payment.getPaymentKey(), payment.getTossOrderId(),
        payment.getAmount(), payment.getConfirmKey(), alreadyApproved);
  }
}