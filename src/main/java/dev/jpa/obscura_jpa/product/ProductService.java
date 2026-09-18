package dev.jpa.obscura_jpa.product;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jpa.obscura_jpa.brand.Brand;
import dev.jpa.obscura_jpa.brand.BrandRepository;
import dev.jpa.obscura_jpa.category.Category;
import dev.jpa.obscura_jpa.category.CategoryRepository;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
        ProductRepository productRepository,
        BrandRepository brandRepository,
        CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    // 상품 등록
    public ProductDTO createProduct(ProductDTO dto) {

        // 브랜드 존재 여부 확인
        Brand brand = brandRepository.findById(dto.getBno())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 브랜드입니다."));

        // 비활성 브랜드에는 상품 등록 불가
        if (brand.getStatusNo() == 0) {
            throw new IllegalArgumentException("비활성 브랜드에는 상품을 등록할 수 없습니다.");
        }

        // 카테고리 존재 여부 확인
        Category category = categoryRepository.findById(dto.getCno())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        // 비활성 카테고리에는 상품 등록 불가
        if (category.getStatusNo() == 0) {
            throw new IllegalArgumentException("비활성 카테고리에는 상품을 등록할 수 없습니다.");
        }

        // 상품명 확인
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }

        // 가격 확인
        if (dto.getPrice() == null || dto.getPrice() < 0) {
            throw new IllegalArgumentException("상품 가격은 0원 이상이어야 합니다.");
        }

        // 할인율 기본값
        int discountRate = dto.getDiscountRate() == null
            ? 0
            : dto.getDiscountRate();

        if (discountRate < 0 || discountRate > 100) {
            throw new IllegalArgumentException("할인율은 0~100 사이여야 합니다.");
        }

        Product product = Product.builder()
            .brand(brand)
            .category(category)
            .name(dto.getName())
            .detail(dto.getDetail())
            .price(dto.getPrice())
            .discountRate(discountRate)
            .statusNo(1)
            .cdate(LocalDateTime.now())
            .build();

        return toDTO(productRepository.save(product));
    }

    // 전체 상품 조회
    @Transactional(readOnly = true)
    public List<ProductDTO> findAll() {
        return productRepository.findAllByOrderByNoDesc()
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 판매중 상품 조회
    @Transactional(readOnly = true)
    public List<ProductDTO> findActiveProducts() {
        return productRepository.findAllByStatusNoOrderByNoDesc(1)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 상품번호 조회
    @Transactional(readOnly = true)
    public ProductDTO findByNo(Long no) {
        Product product = productRepository.findById(no)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        return toDTO(product);
    }

    // 브랜드별 상품 조회
    @Transactional(readOnly = true)
    public List<ProductDTO> findByBrand(Long bno) {
        return productRepository.findAllByBrandNoOrderByNoDesc(bno)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 카테고리별 상품 조회
    @Transactional(readOnly = true)
    public List<ProductDTO> findByCategory(Long cno) {
        return productRepository.findAllByCategoryNoOrderByNoDesc(cno)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 브랜드 + 카테고리 조회
    @Transactional(readOnly = true)
    public List<ProductDTO> findByBrandAndCategory(Long bno, Long cno) {
        return productRepository
            .findAllByBrandNoAndCategoryNoOrderByNoDesc(bno, cno)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 상품명 검색
    @Transactional(readOnly = true)
    public List<ProductDTO> searchByName(String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        return productRepository
            .findByNameContainingIgnoreCaseOrderByNoDesc(name)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 상품 수정
    public ProductDTO updateProduct(Long no, ProductDTO dto) {

        Product product = productRepository.findById(no)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // 브랜드 변경
        if (dto.getBno() != null) {

            Brand brand = brandRepository.findById(dto.getBno())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 브랜드입니다."));

            if (brand.getStatusNo() == 0) {
                throw new IllegalArgumentException("비활성 브랜드로 변경할 수 없습니다.");
            }

            product.setBrand(brand);
        }

        // 카테고리 변경
        if (dto.getCno() != null) {

            Category category = categoryRepository.findById(dto.getCno())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

            if (category.getStatusNo() == 0) {
                throw new IllegalArgumentException("비활성 카테고리로 변경할 수 없습니다.");
            }

            product.setCategory(category);
        }

        if (dto.getName() != null) {

            if (dto.getName().isBlank()) {
                throw new IllegalArgumentException("상품명은 비워둘 수 없습니다.");
            }

            product.setName(dto.getName());
        }

        if (dto.getDetail() != null) {
            product.setDetail(dto.getDetail());
        }

        if (dto.getPrice() != null) {

            if (dto.getPrice() < 0) {
                throw new IllegalArgumentException("상품 가격은 0원 이상이어야 합니다.");
            }

            product.setPrice(dto.getPrice());
        }

        if (dto.getDiscountRate() != null) {

            if (dto.getDiscountRate() < 0 || dto.getDiscountRate() > 100) {
                throw new IllegalArgumentException("할인율은 0~100 사이여야 합니다.");
            }

            product.setDiscountRate(dto.getDiscountRate());
        }

        if (dto.getStatusNo() != null) {

            if (dto.getStatusNo() != 0 && dto.getStatusNo() != 1) {
                throw new IllegalArgumentException("상품 상태는 0 또는 1이어야 합니다.");
            }

            product.setStatusNo(dto.getStatusNo());
        }

        return toDTO(productRepository.save(product));
    }

    // 상품 비활성화
    public void disableProduct(Long no) {

        Product product = productRepository.findById(no)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        if (product.getStatusNo() == 0) {
            throw new IllegalArgumentException("이미 비활성화된 상품입니다.");
        }

        product.setStatusNo(0);

        productRepository.save(product);
    }

    // Entity → DTO
    private ProductDTO toDTO(Product product) {

        return ProductDTO.builder()
            .no(product.getNo())
            .bno(product.getBrand().getNo())
            .cno(product.getCategory().getNo())
            .name(product.getName())
            .detail(product.getDetail())
            .price(product.getPrice())
            .discountRate(product.getDiscountRate())
            .statusNo(product.getStatusNo())
            .cdate(product.getCdate())
            .build();
    }
}