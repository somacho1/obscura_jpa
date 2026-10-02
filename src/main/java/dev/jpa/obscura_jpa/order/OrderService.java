package dev.jpa.obscura_jpa.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jpa.obscura_jpa.cart.Cart;
import dev.jpa.obscura_jpa.cart.CartRepository;
import dev.jpa.obscura_jpa.cartitem.CartItem;
import dev.jpa.obscura_jpa.cartitem.CartItemRepository;
import dev.jpa.obscura_jpa.member.ObMember;
import dev.jpa.obscura_jpa.member.ObMemberRepository;
import dev.jpa.obscura_jpa.orderitem.OrderItem;
import dev.jpa.obscura_jpa.orderitem.OrderItemDTO;
import dev.jpa.obscura_jpa.orderitem.OrderItemRepository;
import dev.jpa.obscura_jpa.product.Product;
import dev.jpa.obscura_jpa.productoption.ProductOption;
import dev.jpa.obscura_jpa.stock.Stock;
import dev.jpa.obscura_jpa.stock.StockRepository;
import dev.jpa.obscura_jpa.delivery.DeliveryDTO;
import dev.jpa.obscura_jpa.delivery.DeliveryService;
import dev.jpa.obscura_jpa.deliveryitem.DeliveryItemDTO;
import dev.jpa.obscura_jpa.deliveryitem.DeliveryItemService;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ObMemberRepository obMemberRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final StockRepository stockRepository;
    // 기존 배송지 검증·주소 스냅샷 저장 기능을 재사용합니다.
    private final DeliveryService deliveryService;
    // 배송과 주문상품을 연결하고 배송수량을 검증합니다.
    private final DeliveryItemService deliveryItemService;

    public OrderService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        ObMemberRepository obMemberRepository,
        CartRepository cartRepository,
        CartItemRepository cartItemRepository,
        StockRepository stockRepository,
        DeliveryService deliveryService,
        DeliveryItemService deliveryItemService
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.obMemberRepository = obMemberRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.stockRepository = stockRepository;
        this.deliveryService = deliveryService;
        this.deliveryItemService = deliveryItemService;
    }

    // =====================================================
    // 주문 생성
    // CART → CARTITEM → ORDERS → ORDERITEM
    // → STOCK 차감 → CARTITEM 삭제
    // =====================================================
    public OrderDTO createOrder(OrderDTO dto) {

        if (dto.getMno() == null) {
            throw new IllegalArgumentException(
                "회원번호는 필수입니다."
            );
        }
        
        // 배송지가 없으면 주문 생성 전에 중단합니다.
        if (dto.getDelivery() == null) {
            throw new IllegalArgumentException("배송지 정보를 입력해주세요.");
        }

        // 1. 회원 확인
        ObMember member =
            obMemberRepository.findById(dto.getMno())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 회원입니다."
                    )
                );

        if (member.getStatusNo() != 1) {
            throw new IllegalArgumentException(
                "정상 상태의 회원만 주문할 수 있습니다."
            );
        }

        // 2. 회원 장바구니 조회
        Cart cart =
            cartRepository.findByMemberNo(dto.getMno())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "장바구니가 존재하지 않습니다."
                    )
                );

        // 3. 주문할 장바구니 항목 번호를 검증합니다.
        if (dto.getCartItemNos() == null || dto.getCartItemNos().isEmpty()) {
            throw new IllegalArgumentException("주문할 상품을 선택해주세요.");
        }

        // 잘못된 번호와 중복 번호는 허용하지 않습니다.
        if (dto.getCartItemNos().stream().anyMatch(no -> no == null || no <= 0)) {
            throw new IllegalArgumentException("잘못된 장바구니 항목 번호입니다.");
        }

        Set<Long> selectedNos = new HashSet<>(dto.getCartItemNos());
        if (selectedNos.size() != dto.getCartItemNos().size()) {
            throw new IllegalArgumentException("주문할 상품 번호가 중복되었습니다.");
        }

        // 해당 회원의 장바구니 안에서만 선택 항목을 찾습니다.
        // 다른 회원의 항목 번호를 전달해도 주문할 수 없습니다.
        List<CartItem> cartItems = cartItemRepository
            .findAllByCartNoOrderByCdateDesc(cart.getNo())
            .stream()
            .filter(item -> selectedNos.contains(item.getNo()))
            .toList();

        // 삭제된 항목 또는 다른 회원의 항목이 포함되면 주문 전체를 중단합니다.
        if (cartItems.size() != selectedNos.size()) {
            throw new IllegalArgumentException("주문할 상품이 장바구니에 없습니다. 장바구니를 다시 확인해주세요.");
        }

        // 주문 생성 전에 모든 상품을 먼저 검증
        long totalPrice = 0L;

        for (CartItem cartItem : cartItems) {

            ProductOption option =
                cartItem.getProductOption();

            if (!"Y".equals(option.getUseYn())) {
                throw new IllegalArgumentException(
                    "판매가 중지된 상품 옵션이 포함되어 있습니다."
                );
            }

            Product product =
                option.getProduct();

            if (product.getStatusNo() != 1) {
                throw new IllegalArgumentException(
                    "판매가 중지된 상품이 포함되어 있습니다."
                );
            }

            Stock stock =
                stockRepository
                    .findByProductOptionNo(option.getNo())
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                            "재고정보가 없는 상품이 포함되어 있습니다."
                        )
                    );

            if (stock.getQty() < cartItem.getQty()) {
                throw new IllegalArgumentException(
                    product.getName()
                    + " 상품의 재고가 부족합니다."
                );
            }

            long salePrice =
                calculateSalePrice(product);

            totalPrice +=
                salePrice * cartItem.getQty();
        }

        // 선택 상품 합계를 기준으로 배송비를 서버에서 계산합니다.
        // 클라이언트가 전달한 금액·배송비는 사용하지 않습니다.
        long shippingFee = totalPrice >= 60000L ? 0L : 3500L;
        long paymentPrice = totalPrice + shippingFee;

        // 4. ORDERS 생성: 최종 금액과 주문 당시 배송비를 함께 보존합니다.
        Order order = Order.builder()
            .member(member)
            .totalPrice(paymentPrice)
            .shippingFee(shippingFee)
            .statusNo(1) // 결제대기
            .cancelStatusNo(0)
            .cdate(LocalDateTime.now())
            .build();

        order =
            orderRepository.save(order);

        // 5. ORDERITEM 생성 + STOCK 차감
        List<OrderItemDTO> orderItemDTOs =
            new ArrayList<>();

        for (CartItem cartItem : cartItems) {

            ProductOption option =
                cartItem.getProductOption();

            Product product =
                option.getProduct();

            Stock stock =
                stockRepository
                    .findByProductOptionNo(option.getNo())
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                            "재고정보를 찾을 수 없습니다."
                        )
                    );

            long salePrice =
                calculateSalePrice(product);

            // 주문 당시 상품정보 저장
            OrderItem orderItem =
                OrderItem.builder()
                    .order(order)
                    .productOption(option)
                    .productName(product.getName())
                    .color(option.getColor())
                    .sizeValue(option.getSizeValue())
                    .price(salePrice)
                    .qty(cartItem.getQty())
                    .statusNo(1)
                    .cancelQty(0L)
                    .cancelReason(null)
                    .cancelDate(null)
                    .cdate(LocalDateTime.now())
                    .build();

            orderItem =
                orderItemRepository.save(orderItem);

            // 실제 재고 차감
            stock.setQty(
                stock.getQty() - cartItem.getQty()
            );

            stock.setUdate(
                LocalDateTime.now()
            );

            stockRepository.save(stock);

            orderItemDTOs.add(
                toOrderItemDTO(orderItem)
            );
        }
        
        // 주문 당시 배송지를 DELIVERY에 저장합니다.
        // 저장된 회원 배송지는 DeliveryService에서 해당 회원의 주소인지 검증합니다.
        OrderDeliveryDTO address = dto.getDelivery();
        DeliveryDTO deliveryRequest = new DeliveryDTO();
        deliveryRequest.setOrdno(order.getNo());
        deliveryRequest.setMadno(address.getMadno());
        deliveryRequest.setReceiver(address.getReceiver());
        deliveryRequest.setPhone(address.getPhone());
        deliveryRequest.setZipcode(address.getZipcode());
        deliveryRequest.setAddress1(address.getAddress1());
        deliveryRequest.setAddress2(address.getAddress2());

        // 저장된 배송번호를 받아 주문상품과 연결합니다.
        DeliveryDTO savedDelivery = deliveryService.create(deliveryRequest);

        // 최초 주문은 한 배송에 선택 상품의 전체 수량을 배정합니다.
        // 실제 출고 전에는 배송준비 상태로 유지합니다.
        for (OrderItemDTO item : orderItemDTOs) {
            DeliveryItemDTO deliveryItemRequest = new DeliveryItemDTO();
            deliveryItemRequest.setDno(savedDelivery.getNo());
            deliveryItemRequest.setOino(item.getNo());
            deliveryItemRequest.setQty(Math.toIntExact(item.getQty()));

            // 기존 서비스에서 주문 일치·중복·배송수량을 검증합니다.
            deliveryItemService.create(deliveryItemRequest);
        }

        // 6. 선택한 주문 상품만 장바구니에서 제거합니다.
        cartItemRepository.deleteAll(cartItems);

        // 7. 결과 반환
        return toDTO(
            order,
            orderItemDTOs
        );
    }

    // =====================================================
    // 회원별 주문 목록
    // =====================================================
    @Transactional(readOnly = true)
    public List<OrderDTO> findByMember(Long mno) {

        return orderRepository
            .findAllByMemberNoOrderByCdateDesc(mno)
            .stream()
            .map(order -> {

                List<OrderItemDTO> items =
                    orderItemRepository
                        .findAllByOrderNoOrderByNoAsc(
                            order.getNo()
                        )
                        .stream()
                        .map(this::toOrderItemDTO)
                        .toList();

                return toDTO(
                    order,
                    items
                );
            })
            .toList();
    }

    // =====================================================
    // 주문 상세 조회
    // =====================================================
    @Transactional(readOnly = true)
    public OrderDTO findByNo(Long no) {

        Order order =
            orderRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 주문입니다."
                    )
                );

        List<OrderItemDTO> items =
            orderItemRepository
                .findAllByOrderNoOrderByNoAsc(no)
                .stream()
                .map(this::toOrderItemDTO)
                .toList();

        return toDTO(
            order,
            items
        );
    }

    // =====================================================
    // 주문 상태 변경
    // 관리자/결제/배송 연동에서 사용
    // =====================================================
    public OrderDTO updateStatus(
        Long no,
        OrderDTO dto
    ) {

        Order order =
            orderRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 주문입니다."
                    )
                );

        if (dto.getStatusNo() == null) {
            throw new IllegalArgumentException(
                "주문상태는 필수입니다."
            );
        }

        if (
            dto.getStatusNo() < 0
            || dto.getStatusNo() > 5
        ) {
            throw new IllegalArgumentException(
                "올바르지 않은 주문상태입니다."
            );
        }
        
     // 취소는 재고 복구를 포함하는 전용 메서드에서 처리합니다.
        if (dto.getStatusNo() == 0) {
            throw new IllegalArgumentException("주문 취소는 전용 취소 API를 사용해주세요.");
        }

        // 취소된 주문을 상태 변경만으로 다시 활성화하지 않습니다.
        if (Integer.valueOf(0).equals(order.getStatusNo())) {
            throw new IllegalArgumentException("취소된 주문의 상태는 변경할 수 없습니다.");
        }

        order.setStatusNo(
            dto.getStatusNo()
        );

        Order saved =
            orderRepository.save(order);

        List<OrderItemDTO> items =
            orderItemRepository
                .findAllByOrderNoOrderByNoAsc(no)
                .stream()
                .map(this::toOrderItemDTO)
                .toList();

        return toDTO(
            saved,
            items
        );
    }
    
 // 결제 대기 주문을 전체 취소하고 차감했던 재고를 복구합니다.
 // 클래스의 @Transactional에 의해 취소 상태와 재고 변경이 함께 저장됩니다.
 public OrderDTO cancelPendingOrder(Long no, Long mno) {
     if (no == null || no <= 0) throw new IllegalArgumentException("올바른 주문번호가 필요합니다.");
     if (mno == null || mno <= 0) throw new IllegalArgumentException("회원번호는 필수입니다.");

     // 동일 주문의 취소 요청을 순서대로 처리하기 위해 주문 행을 잠급니다.
     Order order = orderRepository.findByNoForUpdate(no)
         .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

     // 다른 회원의 주문은 취소할 수 없습니다.
     if (!order.getMember().getNo().equals(mno)) {
         throw new IllegalArgumentException("해당 회원의 주문이 아닙니다.");
     }

     List<OrderItem> items = orderItemRepository.findAllByOrderNoOrderByNoAsc(no);

     // 이 메서드로 이미 전체 취소한 주문은 그대로 반환합니다.
     // 재고 복구를 다시 실행하지 않아 중복 요청에도 수량이 늘지 않습니다.
     if (Integer.valueOf(0).equals(order.getStatusNo())
         && Integer.valueOf(2).equals(order.getCancelStatusNo())) {
         return toDTO(order, items.stream().map(this::toOrderItemDTO).toList());
     }

     // 결제 완료 주문은 환불 처리가 필요하므로 이 메서드에서 취소하지 않습니다.
     if (!Integer.valueOf(1).equals(order.getStatusNo())) {
         throw new IllegalArgumentException("결제 대기 주문만 취소할 수 있습니다.");
     }
     if (items.isEmpty()) throw new IllegalArgumentException("주문상품 정보가 없습니다.");

     // 여러 재고 행을 잠글 때 옵션번호 순서로 처리합니다.
     List<OrderItem> sortedItems = new ArrayList<>(items);
     sortedItems.sort(java.util.Comparator.comparing(item -> item.getProductOption().getNo()));

     LocalDateTime now = LocalDateTime.now();

     for (OrderItem item : sortedItems) {
         long qty = item.getQty();
         long cancelledQty = item.getCancelQty() == null ? 0L : item.getCancelQty();

         if (qty <= 0 || cancelledQty < 0 || cancelledQty > qty) {
             throw new IllegalArgumentException("주문상품의 취소 수량이 올바르지 않습니다.");
         }

         // 이미 취소된 수량을 제외한 나머지만 복구합니다.
         long restoreQty = qty - cancelledQty;
         if (restoreQty > 0) {
             Stock stock = stockRepository.findByProductOptionNoForUpdate(item.getProductOption().getNo())
                 .orElseThrow(() -> new IllegalArgumentException("복구할 재고정보를 찾을 수 없습니다."));

             stock.setQty(Math.addExact(stock.getQty(), restoreQty));
             stock.setUdate(now);
         }

         item.setStatusNo(0);                  // 주문상품 취소
         item.setCancelQty(qty);               // 전체 수량 취소
         item.setCancelReason("결제 전 주문 취소");
         item.setCancelDate(now);
     }

     order.setStatusNo(0);                     // 주문 취소
     order.setCancelStatusNo(2);               // 전체 취소

     // 조회한 Entity는 변경 감지로 저장되므로 save를 각각 호출하지 않습니다.
     return toDTO(order, items.stream().map(this::toOrderItemDTO).toList());
 }

    // =====================================================
    // 할인 적용 실제 판매가격 계산
    // =====================================================
    private long calculateSalePrice(
        Product product
    ) {

        long price =
            product.getPrice();

        int discountRate =
            product.getDiscountRate() == null
                ? 0
                : product.getDiscountRate();

        // 상품 목록·상세와 동일하게 할인 적용 가격의 소수점 이하를 버립니다.
        return price * (100L - discountRate) / 100L;
    }

    // =====================================================
    // Order → DTO
    // =====================================================
    private OrderDTO toDTO(
        Order order,
        List<OrderItemDTO> items
    ) {

        return OrderDTO.builder()
            .no(order.getNo())
            .mno(order.getMember().getNo())
            .totalPrice(order.getTotalPrice())
            .shippingFee(order.getShippingFee())
            .statusNo(order.getStatusNo())
            .cancelStatusNo(
                order.getCancelStatusNo()
            )
            .cdate(order.getCdate())
            .items(items)
            .build();
    }

    // =====================================================
    // OrderItem → DTO
    // =====================================================
    private OrderItemDTO toOrderItemDTO(
        OrderItem item
    ) {

        return OrderItemDTO.builder()
            .no(item.getNo())
            .ordno(item.getOrder().getNo())
            .pono(
                item.getProductOption().getNo()
            )
            .productName(
                item.getProductName()
            )
            .color(item.getColor())
            .sizeValue(
                item.getSizeValue()
            )
            .price(item.getPrice())
            .qty(item.getQty())
            .statusNo(item.getStatusNo())
            .cancelQty(
                item.getCancelQty()
            )
            .cancelReason(
                item.getCancelReason()
            )
            .cancelDate(
                item.getCancelDate()
            )
            .cdate(item.getCdate())
            .build();
    }
}