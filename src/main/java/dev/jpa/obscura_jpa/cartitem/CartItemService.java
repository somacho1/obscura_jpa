package dev.jpa.obscura_jpa.cartitem;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jpa.obscura_jpa.cart.Cart;
import dev.jpa.obscura_jpa.cart.CartRepository;
import dev.jpa.obscura_jpa.cart.CartService;
import dev.jpa.obscura_jpa.productoption.ProductOption;
import dev.jpa.obscura_jpa.productoption.ProductOptionRepository;
import dev.jpa.obscura_jpa.stock.Stock;
import dev.jpa.obscura_jpa.product.Product;
import dev.jpa.obscura_jpa.productimage.ProductImageRepository;
import dev.jpa.obscura_jpa.stock.StockRepository;

@Service
@Transactional
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final ProductOptionRepository productOptionRepository;
    private final StockRepository stockRepository;
    private final ProductImageRepository productImageRepository;

    public CartItemService(
        CartItemRepository cartItemRepository,
        CartRepository cartRepository,
        CartService cartService,
        ProductOptionRepository productOptionRepository,
        StockRepository stockRepository,
        ProductImageRepository productImageRepository
    ) {
        this.cartItemRepository = cartItemRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
        this.productOptionRepository = productOptionRepository;
        this.stockRepository = stockRepository;
        this.productImageRepository = productImageRepository;
    }

    // =====================================================
    // 장바구니 상품 추가
    // =====================================================
    public CartItemDTO addItem(CartItemDTO dto) {

        if (dto.getMno() == null) {
            throw new IllegalArgumentException(
                "회원번호는 필수입니다."
            );
        }

        if (dto.getPono() == null) {
            throw new IllegalArgumentException(
                "상품옵션번호는 필수입니다."
            );
        }

        Long requestQty =
            dto.getQty() == null ? 1L : dto.getQty();

        if (requestQty < 1) {
            throw new IllegalArgumentException(
                "장바구니 수량은 1 이상이어야 합니다."
            );
        }

        // 회원 CART 조회 또는 자동 생성
        Cart cart =
            cartService.getOrCreateCart(dto.getMno());

        // 상품옵션 확인
        ProductOption productOption =
            productOptionRepository.findById(dto.getPono())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 상품 옵션입니다."
                    )
                );

        // 판매 중인 옵션인지 확인
        if (!"Y".equals(productOption.getUseYn())) {
            throw new IllegalArgumentException(
                "사용 중지된 상품 옵션입니다."
            );
        }

        // 해당 옵션의 재고 조회
        Stock stock =
            stockRepository
                .findByProductOptionNo(dto.getPono())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "재고정보가 없는 상품 옵션입니다."
                    )
                );

        // 같은 옵션이 이미 CART에 담겨있는지 확인
        CartItem existingItem =
            cartItemRepository
                .findByCartNoAndProductOptionNo(
                    cart.getNo(),
                    dto.getPono()
                )
                .orElse(null);

        // 기존 상품이 있으면 수량 증가
        if (existingItem != null) {

            long newQty =
                existingItem.getQty() + requestQty;

            if (newQty > stock.getQty()) {
                throw new IllegalArgumentException(
                    "재고수량을 초과할 수 없습니다."
                );
            }

            existingItem.setQty(newQty);

            return toDTO(
                cartItemRepository.save(existingItem)
            );
        }

        // 처음 담는 상품의 재고 검사
        if (requestQty > stock.getQty()) {
            throw new IllegalArgumentException(
                "재고수량을 초과할 수 없습니다."
            );
        }

        CartItem cartItem = CartItem.builder()
            .cart(cart)
            .productOption(productOption)
            .qty(requestQty)
            .cdate(LocalDateTime.now())
            .build();

        return toDTO(
            cartItemRepository.save(cartItem)
        );
    }

    // =====================================================
    // 회원의 장바구니 상품 전체 조회
    // =====================================================
    @Transactional(readOnly = true)
    public List<CartItemDTO> findByMember(Long mno) {

        Cart cart =
            cartRepository.findByMemberNo(mno)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "해당 회원의 장바구니가 없습니다."
                    )
                );

        return cartItemRepository
            .findAllByCartNoOrderByCdateDesc(
                cart.getNo()
            )
            .stream()
            .map(this::toDTO)
            .toList();
    }
    
 // =====================================================
 // React 장바구니 화면용 상세 조회
 // =====================================================
 @Transactional(readOnly = true)
 public List<CartItemDetailDTO> findDetailByMember(
     Long mno
 ) {

     Cart cart =
         cartRepository.findByMemberNo(mno)
             .orElseThrow(() ->
                 new IllegalArgumentException(
                     "해당 회원의 장바구니가 없습니다."
                 )
             );

     return cartItemRepository
         .findAllByCartNoOrderByCdateDesc(
             cart.getNo()
         )
         .stream()
         .map(cartItem -> {

             ProductOption option =
                 cartItem.getProductOption();

             Product product =
                 option.getProduct();

             Stock stock =
                 stockRepository
                     .findByProductOptionNo(
                         option.getNo()
                     )
                     .orElse(null);

             String mainImageUrl =
                 productImageRepository
                     .findFirstByProductNoAndImageTypeAndDisplayYnOrderBySeqNoAsc(
                         product.getNo(),
                         "MAIN",
                         "Y"
                     )
                     .map(image ->
                         image.getImageUrl()
                     )
                     .orElse(null);

             int discountRate =
                 product.getDiscountRate() == null
                     ? 0
                     : product.getDiscountRate();

             long salePrice =
                 product.getPrice()
                     * (100L - discountRate)
                     / 100L;

             long stockQty =
                 stock == null
                     ? 0L
                     : stock.getQty();

             return CartItemDetailDTO.builder()
                 .cartItemNo(
                     cartItem.getNo()
                 )
                 .productNo(
                     product.getNo()
                 )
                 .optionNo(
                     option.getNo()
                 )
                 .brandName(
                     product.getBrand().getName()
                 )
                 .productName(
                     product.getName()
                 )
                 .mainImageUrl(
                     mainImageUrl
                 )
                 .color(
                     option.getColor()
                 )
                 .sizeValue(
                     option.getSizeValue()
                 )
                 .price(
                     product.getPrice()
                 )
                 .discountRate(
                     discountRate
                 )
                 .salePrice(
                     salePrice
                 )
                 .qty(
                     cartItem.getQty()
                 )
                 .stockQty(
                     stockQty
                 )
                 .soldOut(
                     stockQty <= 0
                 )
                 .build();
         })
         .toList();
 }

    // =====================================================
    // CARTITEM 단건 조회
    // =====================================================
    @Transactional(readOnly = true)
    public CartItemDTO findByNo(Long no) {

        CartItem cartItem =
            cartItemRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 장바구니 상품입니다."
                    )
                );

        return toDTO(cartItem);
    }

    // =====================================================
    // 장바구니 상품 수량 변경
    // =====================================================
    public CartItemDTO updateQty(
        Long no,
        CartItemDTO dto
    ) {

        CartItem cartItem =
            cartItemRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 장바구니 상품입니다."
                    )
                );

        if (dto.getQty() == null) {
            throw new IllegalArgumentException(
                "수량은 필수입니다."
            );
        }

        if (dto.getQty() < 1) {
            throw new IllegalArgumentException(
                "장바구니 수량은 1 이상이어야 합니다."
            );
        }

        Long pono =
            cartItem.getProductOption().getNo();

        Stock stock =
            stockRepository
                .findByProductOptionNo(pono)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "재고정보가 없는 상품 옵션입니다."
                    )
                );

        if (dto.getQty() > stock.getQty()) {
            throw new IllegalArgumentException(
                "재고수량을 초과할 수 없습니다."
            );
        }

        cartItem.setQty(dto.getQty());

        return toDTO(
            cartItemRepository.save(cartItem)
        );
    }

    // =====================================================
    // 장바구니 상품 삭제
    // =====================================================
    public void deleteItem(Long no) {

        CartItem cartItem =
            cartItemRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 장바구니 상품입니다."
                    )
                );

        cartItemRepository.delete(cartItem);
    }

    // =====================================================
    // Entity → DTO
    // =====================================================
    private CartItemDTO toDTO(
        CartItem cartItem
    ) {

        return CartItemDTO.builder()
            .no(cartItem.getNo())
            .mno(cartItem.getCart().getMember().getNo())
            .cartno(cartItem.getCart().getNo())
            .pono(cartItem.getProductOption().getNo())
            .qty(cartItem.getQty())
            .cdate(cartItem.getCdate())
            .build();
    }
}