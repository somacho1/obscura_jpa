package dev.jpa.obscura_jpa.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 주문번호로 결제정보 조회.
     */
    Optional<Payment> findByOrderNo(Long ordno);

    /**
     * 해당 주문에 이미 결제정보가 존재하는지 확인.
     *
     * PAYMENT.ORDNO는 UNIQUE이므로
     * 하나의 주문에 PAYMENT를 중복 생성할 수 없다.
     */
    boolean existsByOrderNo(Long ordno);
}