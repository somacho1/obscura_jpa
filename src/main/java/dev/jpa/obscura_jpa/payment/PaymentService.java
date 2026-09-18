package dev.jpa.obscura_jpa.payment;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jpa.obscura_jpa.order.Order;
import dev.jpa.obscura_jpa.order.OrderRepository;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentService(
        PaymentRepository paymentRepository,
        OrderRepository orderRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * 결제정보 생성.
     *
     * 아직 실제 Toss API를 호출하는 기능은 아니다.
     * 현재 단계에서는 PAYMENT DB 데이터를 생성한다.
     */
    public PaymentDTO create(PaymentDTO dto) {

        if (dto.getOrdno() == null) {
            throw new IllegalArgumentException("주문번호는 필수입니다.");
        }

        if (dto.getMethod() == null || dto.getMethod().isBlank()) {
            throw new IllegalArgumentException("결제수단은 필수입니다.");
        }

        String method = dto.getMethod().trim().toUpperCase();

        if (!method.equals("TOSS") && !method.equals("BANK")) {
            throw new IllegalArgumentException(
                "결제수단은 TOSS 또는 BANK만 가능합니다."
            );
        }

        Order order = orderRepository.findById(dto.getOrdno())
            .orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 주문입니다.")
            );

        if (paymentRepository.existsByOrderNo(order.getNo())) {
            throw new IllegalArgumentException(
                "이미 결제정보가 존재하는 주문입니다."
            );
        }

        /*
         * 결제금액은 클라이언트에서 받은 금액을 믿지 않고
         * ORDERS.TOTALPRICE를 기준으로 저장한다.
         *
         * React에서 금액을 조작해서 보내더라도
         * 실제 PAYMENT 금액은 주문금액 기준으로 생성된다.
         */
        Long amount = order.getTotalPrice();

        Payment payment = new Payment();

        payment.setOrder(order);
        payment.setMethod(method);
        payment.setAmount(amount);
        payment.setStatusNo(0);

        /*
         * BANK 결제라면 입금자명을 받을 수 있다.
         */
        if (method.equals("BANK")) {

            if (dto.getDepositor() == null ||
                dto.getDepositor().isBlank()) {

                throw new IllegalArgumentException(
                    "무통장입금은 입금자명이 필요합니다."
                );
            }

            payment.setDepositor(dto.getDepositor().trim());
        }

        /*
         * TOSS의 PAYMENTKEY는 실제 Toss 연동 시
         * 결제 승인 후 저장한다.
         *
         * 현재 생성 단계에서는 null 상태가 정상이다.
         */
        payment.setPaymentKey(null);
        payment.setApproveDate(null);
        payment.setCancelDate(null);
        payment.setCdate(LocalDateTime.now());

        return toDTO(paymentRepository.save(payment));
    }

    /**
     * 전체 결제정보 조회.
     * 관리자 화면에서 사용할 수 있다.
     */
    @Transactional(readOnly = true)
    public List<PaymentDTO> findAll() {

        return paymentRepository.findAll()
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * 결제번호로 조회.
     */
    @Transactional(readOnly = true)
    public PaymentDTO findByNo(Long no) {

        Payment payment = paymentRepository.findById(no)
            .orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 결제정보입니다.")
            );

        return toDTO(payment);
    }

    /**
     * 주문번호로 결제정보 조회.
     */
    @Transactional(readOnly = true)
    public PaymentDTO findByOrder(Long ordno) {

        Payment payment = paymentRepository.findByOrderNo(ordno)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "해당 주문의 결제정보가 없습니다."
                )
            );

        return toDTO(payment);
    }

    /**
     * 결제상태 변경.
     *
     * 0 : 결제대기
     * 1 : 결제완료
     * 2 : 부분취소
     * 3 : 전체취소
     * 4 : 결제실패
     */
    public PaymentDTO updateStatus(
        Long no,
        PaymentDTO dto
    ) {

        Payment payment = paymentRepository.findById(no)
            .orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 결제정보입니다.")
            );

        if (dto.getStatusNo() == null) {
            throw new IllegalArgumentException("결제상태는 필수입니다.");
        }

        int statusNo = dto.getStatusNo();

        if (statusNo < 0 || statusNo > 4) {
            throw new IllegalArgumentException(
                "결제상태는 0~4만 가능합니다."
            );
        }

        payment.setStatusNo(statusNo);

        /*
         * 결제완료
         */
        if (statusNo == 1) {

            payment.setApproveDate(LocalDateTime.now());

            /*
             * 주문도 결제완료 상태로 변경한다.
             *
             * ORDERS.STATUSNO
             * 2 = 결제완료
             */
            Order order = payment.getOrder();
            order.setStatusNo(2);
        }

        /*
         * 부분취소 또는 전체취소
         */
        if (statusNo == 2 || statusNo == 3) {
            payment.setCancelDate(LocalDateTime.now());
        }

        /*
         * 전체취소라면 주문도 취소상태로 변경.
         */
        if (statusNo == 3) {

            Order order = payment.getOrder();

            order.setStatusNo(0);
            order.setCancelStatusNo(2);
        }

        return toDTO(payment);
    }

    /**
     * Entity → DTO 변환.
     */
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