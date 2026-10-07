package dev.jpa.obscura_jpa.payment;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jpa.obscura_jpa.order.Order;
import dev.jpa.obscura_jpa.order.OrderRepository;

@Service
public class BankPaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public BankPaymentService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    // 주문 검증과 입금 대기 정보 저장을 함께 처리합니다.
    @Transactional
    public PaymentDTO apply(BankPaymentRequest request) {
        if (request == null) throw new IllegalArgumentException("무통장입금 신청 정보가 필요합니다.");
        if (request.getMno() == null || request.getMno() <= 0) {
            throw new IllegalArgumentException("회원번호는 필수입니다.");
        }
        if (request.getOrdno() == null || request.getOrdno() <= 0) {
            throw new IllegalArgumentException("올바른 주문번호가 필요합니다.");
        }
        if (request.getDepositor() == null || request.getDepositor().isBlank()) {
            throw new IllegalArgumentException("입금자명을 입력해주세요.");
        }

        String depositor = request.getDepositor().trim();
        if (depositor.length() > 50
            || depositor.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 50) {
            throw new IllegalArgumentException("입금자명은 UTF-8 기준 50바이트 이내로 입력해주세요.");
        }

        // Toss 승인 준비·주문 취소와 동일한 주문 잠금을 사용합니다.
        Order order = orderRepository.findByNoForUpdate(request.getOrdno())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if (!order.getMember().getNo().equals(request.getMno())) {
            throw new IllegalArgumentException("해당 회원의 주문이 아닙니다.");
        }
        if (!Integer.valueOf(1).equals(order.getStatusNo())) {
            throw new IllegalArgumentException("결제 대기 주문만 무통장입금을 신청할 수 있습니다.");
        }
        if (order.getTotalPrice() == null || order.getTotalPrice() <= 0) {
            throw new IllegalArgumentException("주문 금액을 확인해주세요.");
        }

        Payment payment = paymentRepository.findByOrderNo(order.getNo()).orElse(null);

        if (payment != null) {
            // 다른 결제수단이나 처리된 결제는 덮어쓰지 않습니다.
            if (!"BANK".equals(payment.getMethod()) || !Integer.valueOf(0).equals(payment.getStatusNo())) {
                throw new IllegalArgumentException("기존 결제정보가 있습니다. 현재 결제 상태를 확인해주세요.");
            }
            if (!order.getTotalPrice().equals(payment.getAmount())) {
                throw new IllegalArgumentException("저장된 결제금액이 주문 금액과 다릅니다.");
            }

            // 동일한 신청은 그대로 반환합니다. 입금자명 변경은 별도 기능으로 처리합니다.
            if (!depositor.equals(payment.getDepositor())) {
                throw new IllegalArgumentException("이미 신청한 입금자명과 다릅니다.");
            }
            return toDTO(payment);
        }

        payment = new Payment();
        payment.setOrder(order);
        payment.setMethod("BANK");
        payment.setAmount(order.getTotalPrice()); // 배송비를 포함한 서버 주문 금액
        payment.setStatusNo(0);                   // 입금 대기
        payment.setDepositor(depositor);
        payment.setCdate(LocalDateTime.now());

        // 실제 입금 확인 전이므로 승인일시와 주문 완료 상태를 설정하지 않습니다.
        return toDTO(paymentRepository.saveAndFlush(payment));
    }
    
    // 관리자 입금 확인: 결제와 주문을 하나의 트랜잭션으로 완료 처리합니다.
    @Transactional
    public PaymentDTO confirmDeposit(Long ordno) {
        if (ordno == null || ordno <= 0) {
            throw new IllegalArgumentException("잘못된 주문번호입니다.");
        }

        // 주문 취소와 입금 확인이 동시에 실행되지 않도록 같은 주문을 잠급니다.
        Order order = orderRepository.findByNoForUpdate(ordno)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        Payment payment = paymentRepository.findByOrderNo(ordno)
            .orElseThrow(() -> new IllegalArgumentException("무통장입금 신청 정보가 없습니다."));

        // Toss 결제는 이 메서드로 완료 처리할 수 없습니다.
        if (!"BANK".equals(payment.getMethod())) {
            throw new IllegalArgumentException("무통장입금 주문만 입금 확인할 수 있습니다.");
        }

        // 결제금액은 화면에서 받지 않고 저장된 주문 금액과 비교합니다.
        if (payment.getAmount() == null || !payment.getAmount().equals(order.getTotalPrice())) {
            throw new IllegalArgumentException("주문 금액과 결제 금액이 일치하지 않습니다.");
        }

        // 같은 요청이 다시 들어와도 승인일시를 덮어쓰지 않습니다.
        if (Integer.valueOf(1).equals(payment.getStatusNo())) {
            if (order.getStatusNo() == null || order.getStatusNo() < 2 || order.getStatusNo() > 5) {
                throw new IllegalArgumentException("결제 상태와 주문 상태를 확인해주세요.");
            }
            return toDTO(payment);
        }

        // 취소·환불된 결제나 결제 대기가 아닌 주문은 완료 처리하지 않습니다.
        if (!Integer.valueOf(0).equals(payment.getStatusNo())) {
            throw new IllegalArgumentException("입금 대기 결제만 확인할 수 있습니다.");
        }
        if (!Integer.valueOf(1).equals(order.getStatusNo())) {
            throw new IllegalArgumentException("결제 대기 주문만 입금 확인할 수 있습니다.");
        }

        payment.setStatusNo(1);
        payment.setApproveDate(LocalDateTime.now());
        order.setStatusNo(2);

        // 조회한 엔티티의 변경사항은 트랜잭션 커밋 시 함께 저장됩니다.
        // 재고는 주문 생성 시 이미 차감했으므로 여기서 다시 차감하지 않습니다.
        return toDTO(payment);
    }

    // 기존 PaymentDTO 응답 형식을 사용합니다.
    private PaymentDTO toDTO(Payment payment) {
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
}