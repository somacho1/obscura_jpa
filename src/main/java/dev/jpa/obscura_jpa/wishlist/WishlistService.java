package dev.jpa.obscura_jpa.wishlist;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jpa.obscura_jpa.member.ObMember;
import dev.jpa.obscura_jpa.member.ObMemberRepository;
import dev.jpa.obscura_jpa.product.Product;
import dev.jpa.obscura_jpa.product.ProductRepository;

@Service
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ObMemberRepository obMemberRepository;
    private final ProductRepository productRepository;

    public WishlistService(
        WishlistRepository wishlistRepository,
        ObMemberRepository obMemberRepository,
        ProductRepository productRepository
    ) {
        this.wishlistRepository = wishlistRepository;
        this.obMemberRepository = obMemberRepository;
        this.productRepository = productRepository;
    }

    // =====================================================
    // 찜 등록
    // =====================================================
    public WishlistDTO createWishlist(WishlistDTO dto) {

        if (dto.getMno() == null) {
            throw new IllegalArgumentException(
                "회원번호는 필수입니다."
            );
        }

        if (dto.getPno() == null) {
            throw new IllegalArgumentException(
                "상품번호는 필수입니다."
            );
        }

        // 회원 존재 확인
        ObMember member =
            obMemberRepository.findById(dto.getMno())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 회원입니다."
                    )
                );

        // 탈퇴/비정상 회원 찜 방지
        if (member.getStatusNo() == null ||
            member.getStatusNo() != 1) {

            throw new IllegalArgumentException(
                "정상 상태의 회원만 찜할 수 있습니다."
            );
        }

        // 상품 존재 확인
        Product product =
            productRepository.findById(dto.getPno())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 상품입니다."
                    )
                );

        // 판매 중지 상품 찜 방지
        if (product.getStatusNo() == null ||
            product.getStatusNo() != 1) {

            throw new IllegalArgumentException(
                "판매 중인 상품만 찜할 수 있습니다."
            );
        }

        // 같은 회원이 같은 상품을 중복 찜하는 것 방지
        if (
            wishlistRepository
                .existsByMemberNoAndProductNo(
                    dto.getMno(),
                    dto.getPno()
                )
        ) {
            throw new IllegalArgumentException(
                "이미 찜한 상품입니다."
            );
        }

        Wishlist wishlist = Wishlist.builder()
            .member(member)
            .product(product)
            .cdate(LocalDateTime.now())
            .build();

        return toDTO(
            wishlistRepository.save(wishlist)
        );
    }

    // =====================================================
    // 회원별 찜 목록
    // =====================================================
    @Transactional(readOnly = true)
    public List<WishlistDTO> findByMember(Long mno) {

        if (!obMemberRepository.existsById(mno)) {
            throw new IllegalArgumentException(
                "존재하지 않는 회원입니다."
            );
        }

        return wishlistRepository
            .findAllByMemberNoOrderByCdateDesc(mno)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // =====================================================
    // 특정 상품 찜 여부
    // =====================================================
    @Transactional(readOnly = true)
    public boolean isWishlisted(
        Long mno,
        Long pno
    ) {

        return wishlistRepository
            .existsByMemberNoAndProductNo(
                mno,
                pno
            );
    }

    // =====================================================
    // 찜 삭제
    // =====================================================
    public void deleteWishlist(
        Long mno,
        Long pno
    ) {

        Wishlist wishlist =
            wishlistRepository
                .findByMemberNoAndProductNo(
                    mno,
                    pno
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "등록된 찜 정보가 없습니다."
                    )
                );

        wishlistRepository.delete(wishlist);
    }

    // =====================================================
    // Entity → DTO
    // =====================================================
    private WishlistDTO toDTO(
        Wishlist wishlist
    ) {

        return WishlistDTO.builder()
            .no(wishlist.getNo())
            .mno(wishlist.getMember().getNo())
            .pno(wishlist.getProduct().getNo())
            .cdate(wishlist.getCdate())
            .build();
    }
}