package dev.jpa.obscura_jpa.productoption;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jpa.obscura_jpa.product.Product;
import dev.jpa.obscura_jpa.product.ProductRepository;

@Service
@Transactional
public class ProductOptionService {

    private final ProductOptionRepository productOptionRepository;
    private final ProductRepository productRepository;

    public ProductOptionService(
        ProductOptionRepository productOptionRepository,
        ProductRepository productRepository
    ) {
        this.productOptionRepository = productOptionRepository;
        this.productRepository = productRepository;
    }

    // 옵션 등록
    public ProductOptionDTO createOption(ProductOptionDTO dto) {

        if (dto.getPno() == null) {
            throw new IllegalArgumentException("상품번호는 필수입니다.");
        }

        Product product = productRepository.findById(dto.getPno())
            .orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 상품입니다.")
            );

        // 비활성 상품에는 새로운 옵션 등록 불가
        if (product.getStatusNo() == 0) {
            throw new IllegalArgumentException(
                "비활성 상품에는 옵션을 등록할 수 없습니다."
            );
        }

        String color = normalize(dto.getColor());
        String sizeValue = normalize(dto.getSizeValue());

        // 같은 상품의 색상 + 사이즈 조합 중복 확인
        if (productOptionRepository
            .existsByProductNoAndColorAndSizeValue(
                dto.getPno(),
                color,
                sizeValue
            )) {

            throw new IllegalArgumentException(
                "이미 등록된 상품 옵션입니다."
            );
        }

        String useYn = dto.getUseYn() == null
            ? "Y"
            : dto.getUseYn().toUpperCase();

        validateUseYn(useYn);

        ProductOption option = ProductOption.builder()
            .product(product)
            .color(color)
            .sizeValue(sizeValue)
            .useYn(useYn)
            .cdate(LocalDateTime.now())
            .build();

        return toDTO(
            productOptionRepository.save(option)
        );
    }

    // 옵션 단건 조회
    @Transactional(readOnly = true)
    public ProductOptionDTO findByNo(Long no) {

        ProductOption option =
            productOptionRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 상품 옵션입니다."
                    )
                );

        return toDTO(option);
    }

    // 특정 상품의 전체 옵션
    @Transactional(readOnly = true)
    public List<ProductOptionDTO> findByProduct(Long pno) {

        return productOptionRepository
            .findAllByProductNoOrderByNoAsc(pno)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 특정 상품의 사용 가능한 옵션
    @Transactional(readOnly = true)
    public List<ProductOptionDTO> findAvailableByProduct(Long pno) {

        return productOptionRepository
            .findAllByProductNoAndUseYnOrderByNoAsc(
                pno,
                "Y"
            )
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 옵션 수정
    public ProductOptionDTO updateOption(
        Long no,
        ProductOptionDTO dto
    ) {

        ProductOption option =
            productOptionRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 상품 옵션입니다."
                    )
                );

        // 전달되지 않은 값은 기존값 유지
        String color = dto.getColor() != null
            ? normalize(dto.getColor())
            : option.getColor();

        String sizeValue = dto.getSizeValue() != null
            ? normalize(dto.getSizeValue())
            : option.getSizeValue();

        /*
         * 수정하려는 색상 + 사이즈 조합이
         * 다른 옵션에 이미 존재하는지 검사
         *
         * 현재 옵션 번호(no)는 제외
         */
        if (productOptionRepository
            .existsByProductNoAndColorAndSizeValueAndNoNot(
                option.getProduct().getNo(),
                color,
                sizeValue,
                no
            )) {

            throw new IllegalArgumentException(
                "이미 등록된 상품 옵션입니다."
            );
        }

        option.setColor(color);
        option.setSizeValue(sizeValue);

        if (dto.getUseYn() != null) {

            String useYn =
                dto.getUseYn().toUpperCase();

            validateUseYn(useYn);

            option.setUseYn(useYn);
        }

        return toDTO(
            productOptionRepository.save(option)
        );
    }

    // 옵션 비활성화
    public void disableOption(Long no) {

        ProductOption option =
            productOptionRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 상품 옵션입니다."
                    )
                );

        // 실제 DELETE하지 않고 사용 여부만 N으로 변경
        option.setUseYn("N");

        productOptionRepository.save(option);
    }

    // USEYN 검증
    private void validateUseYn(String useYn) {

        if (!"Y".equals(useYn)
            && !"N".equals(useYn)) {

            throw new IllegalArgumentException(
                "옵션 사용여부는 Y 또는 N이어야 합니다."
            );
        }
    }

    /*
     * 빈 문자열은 NULL 처리
     *
     * COLOR와 SIZEVALUE는 DB에서 NULL 허용이므로
     * "" 같은 빈 문자열을 그대로 저장하지 않는다.
     */
    private String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toUpperCase();
    }

    // Entity → DTO
    private ProductOptionDTO toDTO(
        ProductOption option
    ) {

        return ProductOptionDTO.builder()
            .no(option.getNo())
            .pno(option.getProduct().getNo())
            .color(option.getColor())
            .sizeValue(option.getSizeValue())
            .useYn(option.getUseYn())
            .cdate(option.getCdate())
            .build();
    }
}