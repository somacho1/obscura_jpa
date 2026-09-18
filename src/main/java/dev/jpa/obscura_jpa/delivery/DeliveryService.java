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

    /**
     * 배송정보 생성.
     *
     * MADNO가 존재하면 MEMBERADDRESS에서 주소정보를 가져와
     * DELIVERY에 주문 당시 주소 스냅샷으로 복사한다.
     *
     * MADNO가 null이면 DTO의 직접입력 주소를 사용한다.
     */
    public DeliveryDTO create(DeliveryDTO dto) {

        if (dto.getOrdno() == null) {
            throw new IllegalArgumentException("주문번호는 필수입니다.");
        }

        Order order = orderRepository.findById(dto.getOrdno())
            .orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 주문입니다.")
            );

        Delivery delivery = new Delivery();

        delivery.setOrder(order);

        /*
         * 회원이 저장해둔 배송지를 선택한 경우.
         */
        if (dto.getMadno() != null) {

            MemberAddress address =
                memberAddressRepository.findById(dto.getMadno())
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                            "존재하지 않는 회원배송지입니다."
                        )
                    );

            /*
             * 다른 회원의 배송지를 사용할 수 없도록 확인.
             *
             * 주문 회원번호와 배송지 회원번호가 같아야 한다.
             */
            if (!address.getMember().getNo()
                .equals(order.getMember().getNo())) {

                throw new IllegalArgumentException(
                    "해당 회원의 배송지가 아닙니다."
                );
            }

            delivery.setMemberAddress(address);

            /*
             * MEMBERADDRESS를 그대로 참조만 하는 것이 아니라
             * 주문 당시 주소를 DELIVERY에 복사한다.
             */
            delivery.setReceiver(address.getReceiver());
            delivery.setPhone(address.getPhone());
            delivery.setZipcode(address.getZipcode());
            delivery.setAddress1(address.getAddress1());
            delivery.setAddress2(address.getAddress2());

        } else {

            /*
             * 직접입력 배송지.
             */
            validateDirectAddress(dto);

            delivery.setMemberAddress(null);
            delivery.setReceiver(dto.getReceiver().trim());
            delivery.setPhone(dto.getPhone().trim());
            delivery.setZipcode(dto.getZipcode().trim());
            delivery.setAddress1(dto.getAddress1().trim());

            if (dto.getAddress2() != null &&
                !dto.getAddress2().isBlank()) {

                delivery.setAddress2(
                    dto.getAddress2().trim()
                );
            }
        }

        /*
         * 배송 생성 시에는 아직 택배사/송장번호가 없을 수 있다.
         */
        delivery.setCompany(null);
        delivery.setTrackingNo(null);

        // 최초 상태 = 배송준비
        delivery.setStatusNo(0);

        delivery.setShipDate(null);
        delivery.setDeliveryDate(null);
        delivery.setCdate(LocalDateTime.now());

        return toDTO(
            deliveryRepository.save(delivery)
        );
    }

    /**
     * 전체 배송 조회.
     * 관리자 화면에서 사용할 수 있다.
     */
    @Transactional(readOnly = true)
    public List<DeliveryDTO> findAll() {

        return deliveryRepository.findAll()
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * 배송번호 조회.
     */
    @Transactional(readOnly = true)
    public DeliveryDTO findByNo(Long no) {

        Delivery delivery =
            deliveryRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 배송정보입니다."
                    )
                );

        return toDTO(delivery);
    }

    /**
     * 주문번호에 속한 배송 조회.
     *
     * 부분배송 때문에 List로 반환한다.
     */
    @Transactional(readOnly = true)
    public List<DeliveryDTO> findByOrder(Long ordno) {

        if (!orderRepository.existsById(ordno)) {
            throw new IllegalArgumentException(
                "존재하지 않는 주문입니다."
            );
        }

        return deliveryRepository
            .findAllByOrderNoOrderByNoAsc(ordno)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * 배송 시작 처리.
     *
     * 택배사와 송장번호를 등록하고
     * STATUSNO를 배송중(1)으로 변경한다.
     */
    public DeliveryDTO startShipping(
        Long no,
        DeliveryDTO dto
    ) {

        Delivery delivery =
            deliveryRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 배송정보입니다."
                    )
                );

        if (dto.getCompany() == null ||
            dto.getCompany().isBlank()) {

            throw new IllegalArgumentException(
                "택배사는 필수입니다."
            );
        }

        if (dto.getTrackingNo() == null ||
            dto.getTrackingNo().isBlank()) {

            throw new IllegalArgumentException(
                "송장번호는 필수입니다."
            );
        }

        delivery.setCompany(
            dto.getCompany().trim()
        );

        delivery.setTrackingNo(
            dto.getTrackingNo().trim()
        );

        delivery.setStatusNo(1);
        delivery.setShipDate(LocalDateTime.now());

        return toDTO(delivery);
    }

    /**
     * 배송완료 처리.
     */
    public DeliveryDTO completeShipping(Long no) {

        Delivery delivery =
            deliveryRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 배송정보입니다."
                    )
                );

        if (delivery.getStatusNo() != 1) {
            throw new IllegalArgumentException(
                "배송중인 배송만 완료처리할 수 있습니다."
            );
        }

        delivery.setStatusNo(2);
        delivery.setDeliveryDate(LocalDateTime.now());

        return toDTO(delivery);
    }

    /**
     * 직접입력 배송지 검증.
     */
    private void validateDirectAddress(DeliveryDTO dto) {

        if (dto.getReceiver() == null ||
            dto.getReceiver().isBlank()) {

            throw new IllegalArgumentException(
                "수령인은 필수입니다."
            );
        }

        if (dto.getPhone() == null ||
            dto.getPhone().isBlank()) {

            throw new IllegalArgumentException(
                "연락처는 필수입니다."
            );
        }

        if (dto.getZipcode() == null ||
            dto.getZipcode().isBlank()) {

            throw new IllegalArgumentException(
                "우편번호는 필수입니다."
            );
        }

        if (dto.getAddress1() == null ||
            dto.getAddress1().isBlank()) {

            throw new IllegalArgumentException(
                "기본주소는 필수입니다."
            );
        }
    }

    /**
     * Entity → DTO.
     */
    private DeliveryDTO toDTO(Delivery delivery) {

        DeliveryDTO dto = new DeliveryDTO();

        dto.setNo(delivery.getNo());
        dto.setOrdno(
            delivery.getOrder().getNo()
        );

        if (delivery.getMemberAddress() != null) {
            dto.setMadno(
                delivery.getMemberAddress().getNo()
            );
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
        dto.setDeliveryDate(
            delivery.getDeliveryDate()
        );
        dto.setCdate(delivery.getCdate());

        return dto;
    }
}