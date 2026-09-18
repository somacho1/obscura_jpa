package dev.jpa.obscura_jpa.producttag;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jpa.obscura_jpa.product.Product;
import dev.jpa.obscura_jpa.product.ProductRepository;

@Service
@Transactional
public class ProductTagService {

    private final ProductTagRepository productTagRepository;
    private final ProductRepository productRepository;

    public ProductTagService(
        ProductTagRepository productTagRepository,
        ProductRepository productRepository
    ) {
        this.productTagRepository =
            productTagRepository;

        this.productRepository =
            productRepository;
    }

    /**
     * 상품 AI 태그 등록.
     *
     * 현재는 Postman으로 테스트하고,
     * 나중에는 Hugging Face 분석 결과가
     * 이 로직으로 들어오도록 확장한다.
     */
    public ProductTagDTO create(ProductTagDTO dto) {

        if (dto.getPno() == null) {
            throw new IllegalArgumentException(
                "상품번호는 필수입니다."
            );
        }

        if (dto.getTag() == null ||
            dto.getTag().isBlank()) {

            throw new IllegalArgumentException(
                "태그는 필수입니다."
            );
        }

        if (dto.getScore() == null) {
            throw new IllegalArgumentException(
                "AI 분석점수는 필수입니다."
            );
        }

        /*
         * SCORE는 0 ~ 1만 허용.
         *
         * DB CHECK 제약조건도 있지만
         * Service에서도 먼저 검증한다.
         */
        if (dto.getScore().compareTo(BigDecimal.ZERO) < 0 ||
            dto.getScore().compareTo(BigDecimal.ONE) > 0) {

            throw new IllegalArgumentException(
                "AI 분석점수는 0~1 사이여야 합니다."
            );
        }

        Product product =
            productRepository.findById(dto.getPno())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 상품입니다."
                    )
                );

        /*
         * 태그값을 소문자로 통일.
         *
         * STREETWEAR / Streetwear / streetwear가
         * 각각 다른 태그로 저장되는 것을 방지한다.
         */
        String tag =
            dto.getTag()
                .trim()
                .toLowerCase();

        /*
         * PRODUCTTAG에는
         * UNIQUE(PNO, TAG)가 걸려있다.
         *
         * DB 오류가 발생하기 전에
         * Service에서도 중복을 확인한다.
         */
        if (productTagRepository
            .existsByProductNoAndTag(
                product.getNo(),
                tag
            )) {

            throw new IllegalArgumentException(
                "이미 등록된 상품 태그입니다."
            );
        }

        ProductTag productTag =
            new ProductTag();

        productTag.setProduct(product);
        productTag.setTag(tag);
        productTag.setScore(dto.getScore());
        productTag.setCdate(LocalDateTime.now());

        return toDTO(
            productTagRepository.save(productTag)
        );
    }

    /**
     * 상품별 AI 태그 조회.
     *
     * 점수가 높은 순서로 반환한다.
     */
    @Transactional(readOnly = true)
    public List<ProductTagDTO> findByProduct(
        Long pno
    ) {

        if (!productRepository.existsById(pno)) {
            throw new IllegalArgumentException(
                "존재하지 않는 상품입니다."
            );
        }

        return productTagRepository
            .findAllByProductNoOrderByScoreDesc(pno)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * 태그별 상품태그 조회.
     *
     * 나중에 AI 추천 후보상품 검색에서
     * 활용할 수 있다.
     */
    @Transactional(readOnly = true)
    public List<ProductTagDTO> findByTag(
        String tag
    ) {

        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException(
                "태그는 필수입니다."
            );
        }

        String normalizedTag =
            tag.trim().toLowerCase();

        return productTagRepository
            .findAllByTagOrderByScoreDesc(
                normalizedTag
            )
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * 태그 점수 수정.
     *
     * AI 모델을 다시 실행했을 때
     * 기존 태그의 SCORE를 갱신할 수 있다.
     */
    public ProductTagDTO updateScore(
        Long no,
        ProductTagDTO dto
    ) {

        ProductTag productTag =
            productTagRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 상품태그입니다."
                    )
                );

        if (dto.getScore() == null) {
            throw new IllegalArgumentException(
                "AI 분석점수는 필수입니다."
            );
        }

        if (dto.getScore().compareTo(BigDecimal.ZERO) < 0 ||
            dto.getScore().compareTo(BigDecimal.ONE) > 0) {

            throw new IllegalArgumentException(
                "AI 분석점수는 0~1 사이여야 합니다."
            );
        }

        productTag.setScore(dto.getScore());

        return toDTO(productTag);
    }

    /**
     * 상품태그 삭제.
     *
     * AI 재분석 결과 해당 스타일이 더 이상
     * 유효하지 않을 때 제거할 수 있다.
     */
    public void delete(Long no) {

        ProductTag productTag =
            productTagRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 상품태그입니다."
                    )
                );

        productTagRepository.delete(productTag);
    }

    /**
     * Entity → DTO.
     */
    private ProductTagDTO toDTO(
        ProductTag productTag
    ) {

        ProductTagDTO dto =
            new ProductTagDTO();

        dto.setNo(productTag.getNo());

        dto.setPno(
            productTag
                .getProduct()
                .getNo()
        );

        dto.setTag(productTag.getTag());
        dto.setScore(productTag.getScore());
        dto.setCdate(productTag.getCdate());

        return dto;
    }
}