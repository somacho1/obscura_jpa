package dev.jpa.obscura_jpa.productimage;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jpa.obscura_jpa.product.Product;
import dev.jpa.obscura_jpa.product.ProductRepository;

@Service
@Transactional
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;

    public ProductImageService(
        ProductImageRepository productImageRepository,
        ProductRepository productRepository
    ) {
        this.productImageRepository = productImageRepository;
        this.productRepository = productRepository;
    }

    // 이미지 등록
    public ProductImageDTO createImage(ProductImageDTO dto) {

        Product product = productRepository.findById(dto.getPno())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        if (product.getStatusNo() == 0) {
            throw new IllegalArgumentException("비활성 상품에는 이미지를 등록할 수 없습니다.");
        }

        if (dto.getImageUrl() == null || dto.getImageUrl().isBlank()) {
            throw new IllegalArgumentException("이미지 URL은 필수입니다.");
        }

        String imageType = dto.getImageType();

        if (!"MAIN".equals(imageType) && !"DETAIL".equals(imageType)) {
            throw new IllegalArgumentException("이미지 구분은 MAIN 또는 DETAIL이어야 합니다.");
        }

        String displayYn = dto.getDisplayYn() == null
            ? "Y"
            : dto.getDisplayYn();

        if (!"Y".equals(displayYn) && !"N".equals(displayYn)) {
            throw new IllegalArgumentException("노출 여부는 Y 또는 N이어야 합니다.");
        }

        if (dto.getSeqNo() == null || dto.getSeqNo() < 1) {
            throw new IllegalArgumentException("이미지 순서는 1 이상이어야 합니다.");
        }

        ProductImage productImage = ProductImage.builder()
            .product(product)
            .imageUrl(dto.getImageUrl())
            .imageType(imageType)
            .displayYn(displayYn)
            .seqNo(dto.getSeqNo())
            .cdate(LocalDateTime.now())
            .build();

        return toDTO(productImageRepository.save(productImage));
    }

    // 이미지 단건 조회
    @Transactional(readOnly = true)
    public ProductImageDTO findByNo(Long no) {

        ProductImage image = productImageRepository.findById(no)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품 이미지입니다."));

        return toDTO(image);
    }

    // 특정 상품의 전체 이미지 조회
    @Transactional(readOnly = true)
    public List<ProductImageDTO> findByProduct(Long pno) {

        return productImageRepository
            .findAllByProductNoOrderBySeqNoAsc(pno)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 특정 상품의 노출 이미지 조회
    @Transactional(readOnly = true)
    public List<ProductImageDTO> findVisibleByProduct(Long pno) {

        return productImageRepository
            .findAllByProductNoAndDisplayYnOrderBySeqNoAsc(pno, "Y")
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 특정 상품의 대표 이미지 조회
    @Transactional(readOnly = true)
    public ProductImageDTO findMainImage(Long pno) {

        ProductImage image = productImageRepository
            .findFirstByProductNoAndImageTypeAndDisplayYnOrderBySeqNoAsc(
                pno,
                "MAIN",
                "Y"
            )
            .orElseThrow(() -> new IllegalArgumentException("대표 이미지가 없습니다."));

        return toDTO(image);
    }

    // 이미지 수정
    public ProductImageDTO updateImage(Long no, ProductImageDTO dto) {

        ProductImage image = productImageRepository.findById(no)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품 이미지입니다."));

        if (dto.getImageUrl() != null) {

            if (dto.getImageUrl().isBlank()) {
                throw new IllegalArgumentException("이미지 URL은 비워둘 수 없습니다.");
            }

            image.setImageUrl(dto.getImageUrl());
        }

        if (dto.getImageType() != null) {

            if (!"MAIN".equals(dto.getImageType())
                && !"DETAIL".equals(dto.getImageType())) {

                throw new IllegalArgumentException(
                    "이미지 구분은 MAIN 또는 DETAIL이어야 합니다."
                );
            }

            image.setImageType(dto.getImageType());
        }

        if (dto.getDisplayYn() != null) {

            if (!"Y".equals(dto.getDisplayYn())
                && !"N".equals(dto.getDisplayYn())) {

                throw new IllegalArgumentException(
                    "노출 여부는 Y 또는 N이어야 합니다."
                );
            }

            image.setDisplayYn(dto.getDisplayYn());
        }

        if (dto.getSeqNo() != null) {

            if (dto.getSeqNo() < 1) {
                throw new IllegalArgumentException(
                    "이미지 순서는 1 이상이어야 합니다."
                );
            }

            image.setSeqNo(dto.getSeqNo());
        }

        return toDTO(productImageRepository.save(image));
    }

    // 이미지 삭제
    public void deleteImage(Long no) {

        ProductImage image = productImageRepository.findById(no)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품 이미지입니다."));

        productImageRepository.delete(image);
    }

    // Entity → DTO
    private ProductImageDTO toDTO(ProductImage image) {

        return ProductImageDTO.builder()
            .no(image.getNo())
            .pno(image.getProduct().getNo())
            .imageUrl(image.getImageUrl())
            .imageType(image.getImageType())
            .displayYn(image.getDisplayYn())
            .seqNo(image.getSeqNo())
            .cdate(image.getCdate())
            .build();
    }
}