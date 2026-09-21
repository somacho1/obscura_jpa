package dev.jpa.obscura_jpa.product;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jpa.obscura_jpa.brand.Brand;
import dev.jpa.obscura_jpa.brand.BrandRepository;
import dev.jpa.obscura_jpa.category.Category;
import dev.jpa.obscura_jpa.category.CategoryRepository;
import dev.jpa.obscura_jpa.productimage.ProductImage;
import dev.jpa.obscura_jpa.productimage.ProductImageRepository;
import dev.jpa.obscura_jpa.productoption.ProductOption;
import dev.jpa.obscura_jpa.productoption.ProductOptionRepository;
import dev.jpa.obscura_jpa.stock.StockRepository;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductOptionRepository productOptionRepository;
    private final StockRepository stockRepository;

    public ProductService(
        ProductRepository productRepository,
        BrandRepository brandRepository,
        CategoryRepository categoryRepository,
        ProductImageRepository productImageRepository,
        ProductOptionRepository productOptionRepository,
        StockRepository stockRepository
    ) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
        this.productOptionRepository = productOptionRepository;
        this.stockRepository = stockRepository;
    }

    // 상품 등록
    public ProductDTO createProduct(ProductDTO dto) {

        // 브랜드 존재 여부 확인
        Brand brand = brandRepository.findById(dto.getBno())
            .orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 브랜드입니다.")
            );

        // 비활성 브랜드에는 상품 등록 불가
        if (brand.getStatusNo() == 0) {
            throw new IllegalArgumentException(
                "비활성 브랜드에는 상품을 등록할 수 없습니다."
            );
        }

        // 카테고리 존재 여부 확인
        Category category = categoryRepository.findById(dto.getCno())
            .orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 카테고리입니다.")
            );

        // 비활성 카테고리에는 상품 등록 불가
        if (category.getStatusNo() == 0) {
            throw new IllegalArgumentException(
                "비활성 카테고리에는 상품을 등록할 수 없습니다."
            );
        }

        // 상품명 확인
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }

        // 가격 확인
        if (dto.getPrice() == null || dto.getPrice() < 0) {
            throw new IllegalArgumentException(
                "상품 가격은 0원 이상이어야 합니다."
            );
        }

        // 할인율 기본값
        int discountRate = dto.getDiscountRate() == null
            ? 0
            : dto.getDiscountRate();

        if (discountRate < 0 || discountRate > 100) {
            throw new IllegalArgumentException(
                "할인율은 0~100 사이여야 합니다."
            );
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

        return productRepository
            .findAllByOrderByNoDesc()
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 판매중 상품 조회
    @Transactional(readOnly = true)
    public List<ProductDTO> findActiveProducts() {

        return productRepository
            .findAllByStatusNoOrderByNoDesc(1)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 상품번호 조회
    @Transactional(readOnly = true)
    public ProductDTO findByNo(Long no) {

        Product product = productRepository
            .findById(no)
            .orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 상품입니다.")
            );

        return toDTO(product);
    }

    /**
     * 사용자 상품 상세페이지 조회
     *
     * PRODUCT 기본정보와
     * PRODUCTIMAGE, PRODUCTOPTION, STOCK 데이터를
     * 상세페이지에서 사용하기 좋은 형태로 조합한다.
     */
    @Transactional(readOnly = true)
    public ProductDetailDTO findDetailByNo(Long no) {

        // 1. 상품 조회
        Product product = productRepository
            .findById(no)
            .orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 상품입니다.")
            );

        /*
         * 2. 현재 노출중인 이미지 전체 조회
         *
         * SEQNO 순서로 가져온 뒤
         * MAIN / SUB / DETAIL 타입으로 나누어 사용한다.
         */
        List<ProductImage> images =
            productImageRepository
                .findAllByProductNoAndDisplayYnOrderBySeqNoAsc(
                    no,
                    "Y"
                );

        // MAIN 대표 이미지
        String mainImageUrl = images.stream()
            .filter(image ->
                "MAIN".equalsIgnoreCase(image.getImageType())
            )
            .map(ProductImage::getImageUrl)
            .findFirst()
            .orElse(null);

        // SUB 이미지
        List<String> subImages = images.stream()
            .filter(image ->
                "SUB".equalsIgnoreCase(image.getImageType())
            )
            .map(ProductImage::getImageUrl)
            .toList();

        // DETAIL 이미지
        List<String> detailImages = images.stream()
            .filter(image ->
                "DETAIL".equalsIgnoreCase(image.getImageType())
            )
            .map(ProductImage::getImageUrl)
            .toList();

        /*
         * 3. 현재 사용중인 상품옵션 조회
         *
         * USEYN = 'Y'인 옵션만 사용자에게 노출한다.
         */
        List<ProductOption> productOptions =
            productOptionRepository
                .findAllByProductNoAndUseYnOrderByNoAsc(
                    no,
                    "Y"
                );

        /*
         * 4. 옵션별 STOCK을 조회하여 품절여부 계산
         *
         * 재고수량 자체는 사용자 화면에 보내지 않고
         * soldOut 값만 전달한다.
         *
         * STOCK 데이터가 존재하지 않는 경우도
         * 안전하게 품절로 처리한다.
         */
        List<ProductDetailOptionDTO> options =
            productOptions.stream()
                .map(option -> {

                    boolean soldOut =
                        stockRepository
                            .findByProductOptionNo(option.getNo())
                            .map(stock ->
                                stock.getQty() <= 0
                            )
                            .orElse(true);

                    return ProductDetailOptionDTO.builder()
                        .optionNo(option.getNo())
                        .color(option.getColor())
                        .sizeValue(option.getSizeValue())
                        .soldOut(soldOut)
                        .build();
                })
                .toList();

        // 5. 할인 적용 판매가격 계산
        int discountRate =
            product.getDiscountRate() == null
                ? 0
                : product.getDiscountRate();

        long salePrice =
            product.getPrice()
            * (100L - discountRate)
            / 100L;

        /*
         * 6. 상세페이지 전용 DTO로 조립
         */
        return ProductDetailDTO.builder()
            .no(product.getNo())

            .bno(product.getBrand().getNo())
            .brandName(product.getBrand().getName())

            .cno(product.getCategory().getNo())
            .categoryName(product.getCategory().getName())

            .name(product.getName())
            .detail(product.getDetail())

            .price(product.getPrice())
            .discountRate(discountRate)
            .salePrice(salePrice)

            .mainImageUrl(mainImageUrl)
            .subImages(subImages)
            .detailImages(detailImages)

            .options(options)

            .statusNo(product.getStatusNo())
            .build();
    }

    // 브랜드별 상품 조회
    @Transactional(readOnly = true)
    public List<ProductDTO> findByBrand(Long bno) {

        return productRepository
            .findAllByBrandNoOrderByNoDesc(bno)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 카테고리별 상품 조회
    @Transactional(readOnly = true)
    public List<ProductDTO> findByCategory(Long cno) {

        return productRepository
            .findAllByCategoryNoOrderByNoDesc(cno)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 브랜드 + 카테고리 조회
    @Transactional(readOnly = true)
    public List<ProductDTO> findByBrandAndCategory(
        Long bno,
        Long cno
    ) {

        return productRepository
            .findAllByBrandNoAndCategoryNoOrderByNoDesc(
                bno,
                cno
            )
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 상품명 검색
    @Transactional(readOnly = true)
    public List<ProductDTO> searchByName(String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                "검색어를 입력해주세요."
            );
        }

        return productRepository
            .findByNameContainingIgnoreCaseOrderByNoDesc(name)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 상품 수정
    public ProductDTO updateProduct(
        Long no,
        ProductDTO dto
    ) {

        Product product = productRepository
            .findById(no)
            .orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 상품입니다.")
            );

        // 브랜드 변경
        if (dto.getBno() != null) {

            Brand brand = brandRepository
                .findById(dto.getBno())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 브랜드입니다."
                    )
                );

            if (brand.getStatusNo() == 0) {
                throw new IllegalArgumentException(
                    "비활성 브랜드로 변경할 수 없습니다."
                );
            }

            product.setBrand(brand);
        }

        // 카테고리 변경
        if (dto.getCno() != null) {

            Category category = categoryRepository
                .findById(dto.getCno())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 카테고리입니다."
                    )
                );

            if (category.getStatusNo() == 0) {
                throw new IllegalArgumentException(
                    "비활성 카테고리로 변경할 수 없습니다."
                );
            }

            product.setCategory(category);
        }

        if (dto.getName() != null) {

            if (dto.getName().isBlank()) {
                throw new IllegalArgumentException(
                    "상품명은 비워둘 수 없습니다."
                );
            }

            product.setName(dto.getName());
        }

        if (dto.getDetail() != null) {
            product.setDetail(dto.getDetail());
        }

        if (dto.getPrice() != null) {

            if (dto.getPrice() < 0) {
                throw new IllegalArgumentException(
                    "상품 가격은 0원 이상이어야 합니다."
                );
            }

            product.setPrice(dto.getPrice());
        }

        if (dto.getDiscountRate() != null) {

            if (
                dto.getDiscountRate() < 0 ||
                dto.getDiscountRate() > 100
            ) {
                throw new IllegalArgumentException(
                    "할인율은 0~100 사이여야 합니다."
                );
            }

            product.setDiscountRate(
                dto.getDiscountRate()
            );
        }

        if (dto.getStatusNo() != null) {

            if (
                dto.getStatusNo() != 0 &&
                dto.getStatusNo() != 1
            ) {
                throw new IllegalArgumentException(
                    "상품 상태는 0 또는 1이어야 합니다."
                );
            }

            product.setStatusNo(
                dto.getStatusNo()
            );
        }

        return toDTO(
            productRepository.save(product)
        );
    }

    // 상품 비활성화
    public void disableProduct(Long no) {

        Product product = productRepository
            .findById(no)
            .orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 상품입니다.")
            );

        if (product.getStatusNo() == 0) {
            throw new IllegalArgumentException(
                "이미 비활성화된 상품입니다."
            );
        }

        product.setStatusNo(0);

        productRepository.save(product);
    }

    /**
     * Entity → 목록/기본조회용 DTO
     *
     * PRODUCT 기본정보에
     * 브랜드명, 카테고리명, 실제 판매가격,
     * 대표이미지 URL을 조합해서 반환한다.
     */
    private ProductDTO toDTO(Product product) {

        // 할인율 null 방어
        int discountRate =
            product.getDiscountRate() == null
                ? 0
                : product.getDiscountRate();

        // 할인 적용 판매가격
        long salePrice =
            product.getPrice()
            * (100L - discountRate)
            / 100L;

        /*
         * MAIN + DISPLAYYN='Y' 중
         * SEQNO가 가장 빠른 대표이미지를 가져온다.
         */
        String mainImageUrl =
            productImageRepository
                .findFirstByProductNoAndImageTypeAndDisplayYnOrderBySeqNoAsc(
                    product.getNo(),
                    "MAIN",
                    "Y"
                )
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return ProductDTO.builder()
            .no(product.getNo())

            .bno(product.getBrand().getNo())
            .brandName(product.getBrand().getName())

            .cno(product.getCategory().getNo())
            .categoryName(product.getCategory().getName())

            .name(product.getName())
            .detail(product.getDetail())

            .price(product.getPrice())
            .discountRate(discountRate)
            .salePrice(salePrice)

            .mainImageUrl(mainImageUrl)

            .statusNo(product.getStatusNo())
            .cdate(product.getCdate())
            .build();
    }
}