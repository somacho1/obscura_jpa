package dev.jpa.obscura_jpa.deliveryitem;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jpa.obscura_jpa.delivery.Delivery;
import dev.jpa.obscura_jpa.delivery.DeliveryRepository;
import dev.jpa.obscura_jpa.orderitem.OrderItem;
import dev.jpa.obscura_jpa.orderitem.OrderItemRepository;

@Service
@Transactional
public class DeliveryItemService {

    private final DeliveryItemRepository deliveryItemRepository;
    private final DeliveryRepository deliveryRepository;
    private final OrderItemRepository orderItemRepository;

    public DeliveryItemService(
        DeliveryItemRepository deliveryItemRepository,
        DeliveryRepository deliveryRepository,
        OrderItemRepository orderItemRepository
    ) {
        this.deliveryItemRepository = deliveryItemRepository;
        this.deliveryRepository = deliveryRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * 배송상품 등록.
     *
     * 검증:
     * 1. 배송 존재
     * 2. 주문상품 존재
     * 3. 배송과 주문상품이 같은 주문인지 확인
     * 4. 같은 배송에 같은 주문상품 중복등록 방지
     * 5. 총 배송수량이 주문수량을 초과하지 않는지 확인
     */
    public DeliveryItemDTO create(DeliveryItemDTO dto) {

        if (dto.getDno() == null) {
            throw new IllegalArgumentException(
                "배송번호는 필수입니다."
            );
        }

        if (dto.getOino() == null) {
            throw new IllegalArgumentException(
                "주문상품번호는 필수입니다."
            );
        }

        if (dto.getQty() == null || dto.getQty() < 1) {
            throw new IllegalArgumentException(
                "배송수량은 1개 이상이어야 합니다."
            );
        }

        /*
         * 배송 조회.
         */
        Delivery delivery =
            deliveryRepository.findById(dto.getDno())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 배송입니다."
                    )
                );

        /*
         * 주문상품 조회.
         */
        OrderItem orderItem =
            orderItemRepository.findById(dto.getOino())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 주문상품입니다."
                    )
                );

        /*
         * DELIVERY와 ORDERITEM이
         * 같은 ORDERS에 속하는지 확인.
         *
         * DELIVERY → ORDERS
         * ORDERITEM → ORDERS
         */
        Long deliveryOrderNo =
            delivery.getOrder().getNo();

        Long itemOrderNo =
            orderItem.getOrder().getNo();

        if (!deliveryOrderNo.equals(itemOrderNo)) {
            throw new IllegalArgumentException(
                "해당 배송과 주문상품의 주문번호가 일치하지 않습니다."
            );
        }

        /*
         * 같은 배송에 같은 ORDERITEM 중복등록 방지.
         *
         * DB에도 UNIQUE(DNO, OINO)가 있지만
         * Service에서도 먼저 검증한다.
         */
        if (deliveryItemRepository
            .existsByDeliveryNoAndOrderItemNo(
                dto.getDno(),
                dto.getOino()
            )) {

            throw new IllegalArgumentException(
                "이미 해당 배송에 등록된 주문상품입니다."
            );
        }

        /*
         * 기존에 배송된 수량 계산.
         *
         * 예:
         * ORDERITEM.QTY = 3
         *
         * DELIVERY #1 = 1
         * DELIVERY #2 = 1
         *
         * 기존 배송수량 = 2
         */
        int alreadyDeliveredQty =
            deliveryItemRepository
                .findAllByOrderItemNo(dto.getOino())
                .stream()
                .mapToInt(DeliveryItem::getQty)
                .sum();

        /*
         * 기존 배송수량 + 신규 배송수량이
         * 주문수량보다 많으면 안 된다.
         */
        int totalDeliveryQty =
            alreadyDeliveredQty + dto.getQty();

        if (totalDeliveryQty > orderItem.getQty()) {
            throw new IllegalArgumentException(
                "배송수량이 주문수량을 초과합니다."
            );
        }

        DeliveryItem deliveryItem =
            new DeliveryItem();

        deliveryItem.setDelivery(delivery);
        deliveryItem.setOrderItem(orderItem);
        deliveryItem.setQty(dto.getQty());
        deliveryItem.setCdate(LocalDateTime.now());

        return toDTO(
            deliveryItemRepository.save(deliveryItem)
        );
    }

    /**
     * 배송번호별 배송상품 조회.
     */
    @Transactional(readOnly = true)
    public List<DeliveryItemDTO> findByDelivery(
        Long dno
    ) {

        if (!deliveryRepository.existsById(dno)) {
            throw new IllegalArgumentException(
                "존재하지 않는 배송입니다."
            );
        }

        return deliveryItemRepository
            .findAllByDeliveryNoOrderByNoAsc(dno)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * 배송상품번호로 조회.
     */
    @Transactional(readOnly = true)
    public DeliveryItemDTO findByNo(Long no) {

        DeliveryItem deliveryItem =
            deliveryItemRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 배송상품입니다."
                    )
                );

        return toDTO(deliveryItem);
    }

    /**
     * 배송상품 삭제.
     *
     * 배송 준비단계에서 잘못 넣은 상품을 제거하는 용도.
     * 배송이 시작된 뒤에는 삭제할 수 없다.
     */
    public void delete(Long no) {

        DeliveryItem deliveryItem =
            deliveryItemRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 배송상품입니다."
                    )
                );

        /*
         * DELIVERY.STATUSNO
         * 0 = 배송준비
         * 1 = 배송중
         * 2 = 배송완료
         */
        if (deliveryItem.getDelivery().getStatusNo() != 0) {
            throw new IllegalArgumentException(
                "배송이 시작된 상품은 삭제할 수 없습니다."
            );
        }

        deliveryItemRepository.delete(deliveryItem);
    }

    /**
     * Entity → DTO.
     */
    private DeliveryItemDTO toDTO(
        DeliveryItem deliveryItem
    ) {

        DeliveryItemDTO dto =
            new DeliveryItemDTO();

        dto.setNo(deliveryItem.getNo());

        dto.setDno(
            deliveryItem
                .getDelivery()
                .getNo()
        );

        dto.setOino(
            deliveryItem
                .getOrderItem()
                .getNo()
        );

        dto.setQty(deliveryItem.getQty());
        dto.setCdate(deliveryItem.getCdate());

        return dto;
    }
}