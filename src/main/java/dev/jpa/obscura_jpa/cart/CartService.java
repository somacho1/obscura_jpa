package dev.jpa.obscura_jpa.cart;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jpa.obscura_jpa.member.ObMember;
import dev.jpa.obscura_jpa.member.ObMemberRepository;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final ObMemberRepository obMemberRepository;

    public CartService(
        CartRepository cartRepository,
        ObMemberRepository obMemberRepository
    ) {
        this.cartRepository = cartRepository;
        this.obMemberRepository = obMemberRepository;
    }

    // =====================================================
    // 장바구니 생성
    // =====================================================
    public CartDTO createCart(CartDTO dto) {

        if (dto.getMno() == null) {
            throw new IllegalArgumentException(
                "회원번호는 필수입니다."
            );
        }

        ObMember member =
            obMemberRepository.findById(dto.getMno())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 회원입니다."
                    )
                );

        // 정상 회원만 장바구니 사용 가능
        if (member.getStatusNo() == null ||
            member.getStatusNo() != 1) {

            throw new IllegalArgumentException(
                "정상 상태의 회원만 장바구니를 사용할 수 있습니다."
            );
        }

        // 회원 한 명당 CART 하나
        if (cartRepository.existsByMemberNo(dto.getMno())) {
            throw new IllegalArgumentException(
                "이미 장바구니가 존재하는 회원입니다."
            );
        }

        Cart cart = Cart.builder()
            .member(member)
            .cdate(LocalDateTime.now())
            .build();

        return toDTO(
            cartRepository.save(cart)
        );
    }

    // =====================================================
    // 회원의 장바구니 조회
    // =====================================================
    @Transactional(readOnly = true)
    public CartDTO findByMember(Long mno) {

        Cart cart =
            cartRepository.findByMemberNo(mno)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "해당 회원의 장바구니가 없습니다."
                    )
                );

        return toDTO(cart);
    }

    // =====================================================
    // CART 번호로 조회
    // =====================================================
    @Transactional(readOnly = true)
    public CartDTO findByNo(Long no) {

        Cart cart =
            cartRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 장바구니입니다."
                    )
                );

        return toDTO(cart);
    }

    // =====================================================
    // 장바구니 조회 또는 생성
    // CARTITEM에서 사용할 핵심 메서드
    // =====================================================
    public Cart getOrCreateCart(Long mno) {

        if (mno == null) {
            throw new IllegalArgumentException(
                "회원번호는 필수입니다."
            );
        }

        // 이미 CART가 있으면 기존 CART 반환
        return cartRepository.findByMemberNo(mno)
            .orElseGet(() -> {

                ObMember member =
                    obMemberRepository.findById(mno)
                        .orElseThrow(() ->
                            new IllegalArgumentException(
                                "존재하지 않는 회원입니다."
                            )
                        );

                if (member.getStatusNo() == null ||
                    member.getStatusNo() != 1) {

                    throw new IllegalArgumentException(
                        "정상 상태의 회원만 장바구니를 사용할 수 있습니다."
                    );
                }

                Cart cart = Cart.builder()
                    .member(member)
                    .cdate(LocalDateTime.now())
                    .build();

                return cartRepository.save(cart);
            });
    }

    // =====================================================
    // Entity → DTO
    // =====================================================
    private CartDTO toDTO(Cart cart) {

        return CartDTO.builder()
            .no(cart.getNo())
            .mno(cart.getMember().getNo())
            .cdate(cart.getCdate())
            .build();
    }
}