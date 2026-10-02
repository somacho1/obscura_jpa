package dev.jpa.obscura_jpa.delivery;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.jpa.obscura_jpa.memberaddress.MemberAddress;
import dev.jpa.obscura_jpa.memberaddress.MemberAddressRepository;
import dev.jpa.obscura_jpa.order.Order;
import dev.jpa.obscura_jpa.order.OrderRepository;

@Service
@Transactional
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final MemberAddressRepository memberAddressRepository;

    public DeliveryService(
        DeliveryRepository deliveryRepository,
        OrderRepository orderRepository,
        MemberAddressRepository memberAddressRepository
    ) {
        this.deliveryRepository = deliveryRepository;
        this.orderRepository = orderRepository;
        this.memberAddressRepository = memberAddressRepository;
    }

    // 배송 생성: 저장된 회원 배송지 또는 직접 입력 주소를 주문 당시 스냅샷으로 보존합니다.
    public DeliveryDTO create(DeliveryDTO dto) {
        if (dto.getOrdno() == null) {
            throw new IllegalArgumentException("주문번호는 필수입니다.");
        }

        Order order = orderRepository.findById(dto.getOrdno())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        Delivery delivery = new Delivery();
        delivery.setOrder(order);

        if (dto.getMadno() != null) {
            // 저장된 배송지는 반드시 주문 회원의 배송지여야 합니다.
            MemberAddress address = memberAddressRepository.findById(dto.getMadno())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원배송지입니다."));

            if (!address.getMember().getNo().equals(order.getMember().getNo())) {
                throw new IllegalArgumentException("해당 회원의 배송지가 아닙니다.");
            }

            // 회원 배송지가 나중에 수정되어도 이번 주문의 주소는 유지됩니다.
            delivery.setMemberAddress(address);
            delivery.setReceiver(address.getReceiver());
            delivery.setPhone(address.getPhone());
            delivery.setZipcode(address.getZipcode());
            delivery.setAddress1(address.getAddress1());
            delivery.setAddress2(address.getAddress2());
        } else {
            // 직접 입력 주소는 서버 검증 후 저장합니다.
            validateDirectAddress(dto);
            delivery.setMemberAddress(null);
            delivery.setReceiver(dto.getReceiver().trim());
            delivery.setPhone(dto.getPhone().trim());
            delivery.setZipcode(dto.getZipcode().trim());
            delivery.setAddress1(dto.getAddress1().trim());
            delivery.setAddress2(dto.getAddress2() == null || dto.getAddress2().isBlank()
                ? null : dto.getAddress2().trim());
        }

        // 최초에는 배송준비 상태이며 택배사·송장번호·출고일은 추후 등록합니다.
        delivery.setCompany(null);
        delivery.setTrackingNo(null);
        delivery.setStatusNo(0);
        delivery.setShipDate(null);
        delivery.setDeliveryDate(null);
        delivery.setCdate(LocalDateTime.now());

        return toDTO(deliveryRepository.save(delivery));
    }

    // 관리자 전체 배송 조회
    @Transactional(readOnly = true)
    public List<DeliveryDTO> findAll() {
        return deliveryRepository.findAll().stream().map(this::toDTO).toList();
    }

    // 배송번호로 단건 조회
    @Transactional(readOnly = true)
    public DeliveryDTO findByNo(Long no) {
        Delivery delivery = deliveryRepository.findById(no)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배송정보입니다."));
        return toDTO(delivery);
    }

    // 하나의 주문에 여러 배송이 있을 수 있으므로 목록으로 반환합니다.
    @Transactional(readOnly = true)
    public List<DeliveryDTO> findByOrder(Long ordno) {
        if (!orderRepository.existsById(ordno)) {
            throw new IllegalArgumentException("존재하지 않는 주문입니다.");
        }
        return deliveryRepository.findAllByOrderNoOrderByNoAsc(ordno)
            .stream().map(this::toDTO).toList();
    }

    // 배송 시작: 택배사와 송장번호를 등록합니다.
    public DeliveryDTO startShipping(Long no, DeliveryDTO dto) {
        Delivery delivery = deliveryRepository.findById(no)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배송정보입니다."));

        if (dto.getCompany() == null || dto.getCompany().isBlank()) {
            throw new IllegalArgumentException("택배사는 필수입니다.");
        }
        if (dto.getTrackingNo() == null || dto.getTrackingNo().isBlank()) {
            throw new IllegalArgumentException("송장번호는 필수입니다.");
        }

        delivery.setCompany(dto.getCompany().trim());
        delivery.setTrackingNo(dto.getTrackingNo().trim());
        delivery.setStatusNo(1);
        delivery.setShipDate(LocalDateTime.now());

        return toDTO(delivery);
    }

    // 배송중 상태인 배송만 완료 처리합니다.
    public DeliveryDTO completeShipping(Long no) {
        Delivery delivery = deliveryRepository.findById(no)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배송정보입니다."));

        if (delivery.getStatusNo() != 1) {
            throw new IllegalArgumentException("배송중인 배송만 완료처리할 수 있습니다.");
        }

        delivery.setStatusNo(2);
        delivery.setDeliveryDate(LocalDateTime.now());

        return toDTO(delivery);
    }

    // 직접 입력 배송지: 필수값·연락처 형식·우편번호·문자 길이를 검증합니다.
    private void validateDirectAddress(DeliveryDTO dto) {

        // 받는 사람
        if (dto.getReceiver() == null || dto.getReceiver().isBlank()) {
            throw new IllegalArgumentException("받는 사람을 입력해주세요.");
        }
        if (dto.getReceiver().trim().length() > 50) {
            throw new IllegalArgumentException("받는 사람은 50자 이내로 입력해주세요.");
        }

        // 연락처: 숫자와 하이픈만 허용하고, 숫자만 남겼을 때 0으로 시작하는 9~11자리인지 확인합니다.
        if (dto.getPhone() == null || dto.getPhone().isBlank()) {
            throw new IllegalArgumentException("연락처를 입력해주세요.");
        }
        String phone = dto.getPhone().trim();
        if (phone.length() > 20 || !phone.matches("[0-9-]+")
            || !phone.replace("-", "").matches("0\\d{8,10}")) {
            throw new IllegalArgumentException("올바른 연락처를 입력해주세요.");
        }

        // 국내 우편번호는 숫자 5자리입니다.
        if (dto.getZipcode() == null || !dto.getZipcode().trim().matches("\\d{5}")) {
            throw new IllegalArgumentException("우편번호는 숫자 5자리로 입력해주세요.");
        }

        // 기본주소
        if (dto.getAddress1() == null || dto.getAddress1().isBlank()) {
            throw new IllegalArgumentException("기본주소를 입력해주세요.");
        }
        if (dto.getAddress1().trim().length() > 255) {
            throw new IllegalArgumentException("기본주소는 255자 이내로 입력해주세요.");
        }

        // 상세주소는 선택값입니다.
        if (dto.getAddress2() != null && dto.getAddress2().trim().length() > 255) {
            throw new IllegalArgumentException("상세주소는 255자 이내로 입력해주세요.");
        }
    }

    // Entity를 API 응답으로 변환합니다.
    private DeliveryDTO toDTO(Delivery delivery) {
        DeliveryDTO dto = new DeliveryDTO();
        dto.setNo(delivery.getNo());
        dto.setOrdno(delivery.getOrder().getNo());

        if (delivery.getMemberAddress() != null) {
            dto.setMadno(delivery.getMemberAddress().getNo());
        }

        dto.setReceiver(delivery.getReceiver());
        dto.setPhone(delivery.getPhone());
        dto.setZipcode(delivery.getZipcode());
        dto.setAddress1(delivery.getAddress1());
        dto.setAddress2(delivery.getAddress2());
        dto.setCompany(delivery.getCompany());
        dto.setTrackingNo(delivery.getTrackingNo());
        dto.setStatusNo(delivery.getStatusNo());
        dto.setShipDate(delivery.getShipDate());
        dto.setDeliveryDate(delivery.getDeliveryDate());
        dto.setCdate(delivery.getCdate());
        return dto;
    }
}