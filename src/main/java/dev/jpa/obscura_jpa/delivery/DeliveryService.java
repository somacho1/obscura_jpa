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

    // 출고 처리: 송장정보·배송 상태·주문 상태를 함께 저장합니다.
    public DeliveryDTO startShipping(Long no, DeliveryDTO dto) {
        if (no == null || no <= 0) {
            throw new IllegalArgumentException("잘못된 배송번호입니다.");
        }
        if (dto == null || dto.getCompany() == null || dto.getCompany().isBlank()) {
            throw new IllegalArgumentException("택배사를 입력해주세요.");
        }
        if (dto.getTrackingNo() == null || dto.getTrackingNo().isBlank()) {
            throw new IllegalArgumentException("송장번호를 입력해주세요.");
        }

        String company = dto.getCompany().trim();
        String trackingNo = dto.getTrackingNo().trim();

        // DB 컬럼의 최대 길이를 초과하지 않도록 검증합니다.
        if (company.length() > 50) {
            throw new IllegalArgumentException("택배사는 50자 이내로 입력해주세요.");
        }
        if (trackingNo.length() > 100) {
            throw new IllegalArgumentException("송장번호는 100자 이내로 입력해주세요.");
        }

        Long ordno = deliveryRepository.findOrderNoByDeliveryNo(no)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배송정보입니다."));

        // 입금 확인·주문 취소와 동일한 주문 잠금을 사용합니다.
        // 같은 주문의 출고 요청도 순서대로 처리됩니다.
        Order order = orderRepository.findByNoForUpdate(ordno)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        
     // 취소 결과 확인 전에는 출고하지 않습니다.
        if (Integer.valueOf(3).equals(order.getCancelStatusNo())) {
            throw new IllegalArgumentException("취소 처리 중인 주문입니다. 취소 결과를 먼저 확인해주세요.");
        }

        Delivery delivery = deliveryRepository.findById(no)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배송정보입니다."));

        Integer orderStatus = order.getStatusNo();

        // 결제 완료·상품 준비 주문만 출고합니다.
        // 부분배송을 위해 이미 배송 중인 주문의 남은 배송도 허용합니다.
        if (!Integer.valueOf(2).equals(orderStatus)
            && !Integer.valueOf(3).equals(orderStatus)
            && !Integer.valueOf(4).equals(orderStatus)) {
            throw new IllegalArgumentException("결제 완료 후 출고할 수 있습니다.");
        }

        // 동일한 요청이 반복되면 기존 출고일을 유지합니다.
        if (Integer.valueOf(1).equals(delivery.getStatusNo())) {
            if (company.equals(delivery.getCompany())
                && trackingNo.equals(delivery.getTrackingNo())) {
                return toDTO(delivery);
            }
            throw new IllegalArgumentException("이미 출고된 배송입니다. 송장 수정은 별도로 처리해주세요.");
        }

        if (!Integer.valueOf(0).equals(delivery.getStatusNo())) {
            throw new IllegalArgumentException("배송 준비 상태에서만 출고할 수 있습니다.");
        }

        // 주문·배송 엔티티는 같은 트랜잭션에서 함께 저장됩니다.
        delivery.setCompany(company);
        delivery.setTrackingNo(trackingNo);
        delivery.setStatusNo(1);
        delivery.setShipDate(LocalDateTime.now());
        order.setStatusNo(4);

        // 재고는 주문 생성 시 차감했으므로 출고 시 다시 차감하지 않습니다.
        return toDTO(delivery);
    }
    // 개별 배송을 완료하고, 모든 배송이 완료되면 주문도 완료합니다.
    public DeliveryDTO completeShipping(Long no) {
        if (no == null || no <= 0) {
            throw new IllegalArgumentException("잘못된 배송번호입니다.");
        }

        // 출고 처리에서 추가한 조회 메서드를 그대로 사용합니다.
        Long ordno = deliveryRepository.findOrderNoByDeliveryNo(no)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배송정보입니다."));

        // 같은 주문의 출고·취소·배송 완료 요청을 순서대로 처리합니다.
        Order order = orderRepository.findByNoForUpdate(ordno)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 주문 잠금을 얻은 뒤 최신 배송 상태를 조회합니다.
        Delivery delivery = deliveryRepository.findById(no)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배송정보입니다."));

        Integer orderStatus = order.getStatusNo();

        // 이미 완료된 동일 요청은 완료일시를 덮어쓰지 않습니다.
        if (Integer.valueOf(2).equals(delivery.getStatusNo())) {
            if (!Integer.valueOf(4).equals(orderStatus)
                && !Integer.valueOf(5).equals(orderStatus)) {
                throw new IllegalArgumentException("주문 상태와 배송 상태를 확인해주세요.");
            }
            return toDTO(delivery);
        }

        if (!Integer.valueOf(4).equals(orderStatus)) {
            throw new IllegalArgumentException("배송 중인 주문만 배송 완료 처리할 수 있습니다.");
        }
        if (!Integer.valueOf(1).equals(delivery.getStatusNo())) {
            throw new IllegalArgumentException("배송 중인 배송만 완료 처리할 수 있습니다.");
        }

        delivery.setStatusNo(2);
        delivery.setDeliveryDate(LocalDateTime.now());

        // 이번 배송의 변경사항을 DB에 반영한 뒤 전체 배송 상태를 조회합니다.
        // flush는 커밋이 아니므로 이후 오류가 나면 함께 롤백됩니다.
        deliveryRepository.flush();

        List<Delivery> deliveries = deliveryRepository.findAllByOrderNoOrderByNoAsc(ordno);
        boolean allCompleted = !deliveries.isEmpty()
            && deliveries.stream().allMatch(item -> Integer.valueOf(2).equals(item.getStatusNo()));

        // 배송 준비 또는 배송 중인 항목이 남아 있으면 주문은 배송 중을 유지합니다.
        if (allCompleted) {
            order.setStatusNo(5);
        }

        // 클래스의 @Transactional에 의해 주문·배송이 함께 저장됩니다.
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