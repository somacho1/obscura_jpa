package dev.jpa.obscura_jpa.brand;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dev.jpa.obscura_jpa.product.ProductRepository;
import lombok.RequiredArgsConstructor;

/**
 * 브랜드 Service
 *
 * 브랜드 등록, 조회, 수정,
 * 활성/비활성 처리 로직을 담당한다.
 */
@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    /**
     * 브랜드 등록
     */
    public BrandDTO createBrand(BrandDTO dto) {

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("브랜드명을 입력해주세요.");
        }

        if (brandRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("이미 등록된 브랜드명입니다.");
        }

        Brand brand = Brand.builder()
            .name(dto.getName())
            .logoUrl(dto.getLogoUrl())
            .visualUrl(dto.getVisualUrl())
            .detail(dto.getDetail())
            .statusNo(1)
            .cdate(LocalDateTime.now())
            .build();

        Brand savedBrand = brandRepository.save(brand);

        return toDTO(savedBrand);
    }

    /**
     * 전체 브랜드 조회
     * 관리자 화면에서 사용
     */
    public List<BrandDTO> findAll() {

        return brandRepository.findAllByOrderByNoDesc()
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * 활성 브랜드 조회
     * 사용자 화면에서 사용
     */
    public List<BrandDTO> findActiveBrands() {

        return brandRepository.findAllByStatusNoOrderByNoDesc(1)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * 브랜드번호로 단건 조회
     */
    public Optional<BrandDTO> findByNo(Long no) {

        return brandRepository.findById(no)
            .map(this::toDTO);
    }

    /**
     * 브랜드명으로 단건 조회
     */
    public Optional<BrandDTO> findByName(String name) {

        return brandRepository.findByName(name)
            .map(this::toDTO);
    }

    /**
     * 브랜드 수정
     */
    public BrandDTO updateBrand(Long no, BrandDTO dto) {

        Brand brand = brandRepository.findById(no)
            .orElseThrow(() -> new IllegalArgumentException("브랜드 정보를 찾을 수 없습니다."));

        if (dto.getName() != null && !dto.getName().isBlank() && !dto.getName().equals(brand.getName())) {
            if (brandRepository.existsByName(dto.getName())) {
                throw new IllegalArgumentException("이미 등록된 브랜드명입니다.");
            }
            brand.setName(dto.getName());
        }

        if (dto.getLogoUrl() != null) {
            brand.setLogoUrl(dto.getLogoUrl());
        }

        if (dto.getVisualUrl() != null) {
            brand.setVisualUrl(dto.getVisualUrl());
        }

        if (dto.getDetail() != null) {
            brand.setDetail(dto.getDetail());
        }

        if (dto.getStatusNo() != null) {
            if (dto.getStatusNo() != 0 && dto.getStatusNo() != 1) {
                throw new IllegalArgumentException("브랜드 상태는 0 또는 1만 가능합니다.");
            }
            brand.setStatusNo(dto.getStatusNo());
        }

        Brand updatedBrand = brandRepository.save(brand);

        return toDTO(updatedBrand);
    }

    /**
     * 브랜드 비활성 처리
     */
    public void disableBrand(Long no) {

        Brand brand = brandRepository.findById(no)
            .orElseThrow(() -> new IllegalArgumentException("브랜드 정보를 찾을 수 없습니다."));

        if (brand.getStatusNo() == 0) {
            throw new IllegalArgumentException("이미 비활성 상태인 브랜드입니다.");
        }

        brand.setStatusNo(0);
        brandRepository.save(brand);
    }

    /**
     * Entity -> DTO 변환
     *
     * 관리자 화면에서 사용할 상품 수와
     * 할인 상품 수도 PRODUCT 테이블에서 계산한다.
     */
    private BrandDTO toDTO(Brand brand) {

        long productCount = productRepository.countByBrandNo(brand.getNo());
        long saleProductCount = productRepository.countByBrandNoAndDiscountRateGreaterThan(brand.getNo(), 0);

        return BrandDTO.builder()
            .no(brand.getNo())
            .name(brand.getName())
            .logoUrl(brand.getLogoUrl())
            .visualUrl(brand.getVisualUrl())
            .detail(brand.getDetail())
            .statusNo(brand.getStatusNo())
            .cdate(brand.getCdate())
            .productCount(productCount)
            .saleProductCount(saleProductCount)
            .build();
    }
}