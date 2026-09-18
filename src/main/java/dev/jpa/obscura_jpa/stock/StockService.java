package dev.jpa.obscura_jpa.stock;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jpa.obscura_jpa.productoption.ProductOption;
import dev.jpa.obscura_jpa.productoption.ProductOptionRepository;

@Service
@Transactional
public class StockService {

    private final StockRepository stockRepository;
    private final ProductOptionRepository productOptionRepository;

    public StockService(
        StockRepository stockRepository,
        ProductOptionRepository productOptionRepository
    ) {
        this.stockRepository = stockRepository;
        this.productOptionRepository = productOptionRepository;
    }

    // 재고 최초 등록
    public StockDTO createStock(StockDTO dto) {

        if (dto.getPono() == null) {
            throw new IllegalArgumentException(
                "상품옵션번호는 필수입니다."
            );
        }

        ProductOption productOption =
            productOptionRepository.findById(dto.getPono())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "존재하지 않는 상품 옵션입니다."
                    )
                );

        // 사용 중지된 옵션에는 새로운 재고 등록 불가
        if (!"Y".equals(productOption.getUseYn())) {
            throw new IllegalArgumentException(
                "사용 중지된 상품 옵션에는 재고를 등록할 수 없습니다."
            );
        }

        // 옵션 하나에는 STOCK 하나만 존재
        if (stockRepository.existsByProductOptionNo(dto.getPono())) {
            throw new IllegalArgumentException(
                "이미 재고가 등록된 상품 옵션입니다."
            );
        }

        Long qty = dto.getQty() == null
            ? 0L
            : dto.getQty();

        if (qty < 0) {
            throw new IllegalArgumentException(
                "재고수량은 0 이상이어야 합니다."
            );
        }

        Stock stock = Stock.builder()
            .productOption(productOption)
            .qty(qty)
            .udate(LocalDateTime.now())
            .build();

        return toDTO(
            stockRepository.save(stock)
        );
    }

    // 전체 재고 조회
    @Transactional(readOnly = true)
    public List<StockDTO> findAll() {

        return stockRepository.findAll()
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // 재고번호(NO)로 단건 조회
    @Transactional(readOnly = true)
    public StockDTO findByNo(Long no) {

        Stock stock = stockRepository.findById(no)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "존재하지 않는 재고정보입니다."
                )
            );

        return toDTO(stock);
    }

    // 상품옵션번호(PONO)로 재고 조회
    @Transactional(readOnly = true)
    public StockDTO findByOption(Long pono) {

        Stock stock =
            stockRepository.findByProductOptionNo(pono)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "해당 상품 옵션의 재고정보가 없습니다."
                    )
                );

        return toDTO(stock);
    }

    // 재고수량 수정
    public StockDTO updateStock(
        Long no,
        StockDTO dto
    ) {

        Stock stock = stockRepository.findById(no)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "존재하지 않는 재고정보입니다."
                )
            );

        if (dto.getQty() == null) {
            throw new IllegalArgumentException(
                "재고수량은 필수입니다."
            );
        }

        if (dto.getQty() < 0) {
            throw new IllegalArgumentException(
                "재고수량은 0 이상이어야 합니다."
            );
        }

        stock.setQty(dto.getQty());

        // 재고가 변경될 때마다 수정시간 갱신
        stock.setUdate(LocalDateTime.now());

        return toDTO(
            stockRepository.save(stock)
        );
    }

    // Entity → DTO
    private StockDTO toDTO(Stock stock) {

        return StockDTO.builder()
            .no(stock.getNo())
            .pono(stock.getProductOption().getNo())
            .qty(stock.getQty())
            .udate(stock.getUdate())
            .build();
    }
}