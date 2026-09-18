package dev.jpa.obscura_jpa.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ObMemberRepository obMemberRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final StockRepository stockRepository;

    public OrderService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        ObMemberRepository obMemberRepository,
        CartRepository cartRepository,
        CartItemRepository cartItemRepository,
        StockRepository stockRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.obMemberRepository = obMemberRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.stockRepository = stockRepository;
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

        // 3. 장바구니 상품 조회
        List<CartItem> cartItems =
            cartItemRepository
                .findAllByCartNoOrderByCdateDesc(
                    cart.getNo()
                );

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException(
                "장바구니에 주문할 상품이 없습니다."
            );
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

        // 4. ORDERS 생성
        Order order = Order.builder()
            .member(member)
            .totalPrice(totalPrice)
            .statusNo(1)
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

        // 6. 주문이 완료된 CARTITEM 제거
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

        return price
            - (price * discountRate / 100);
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