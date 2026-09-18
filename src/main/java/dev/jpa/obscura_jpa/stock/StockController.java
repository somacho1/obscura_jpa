package dev.jpa.obscura_jpa.stock;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
@CrossOrigin(origins = "*")
public class StockController {

    private final StockService stockService;

    public StockController(
        StockService stockService
    ) {
        this.stockService = stockService;
    }

    // 재고 최초 등록
    @PostMapping
    public ResponseEntity<?> createStock(
        @RequestBody StockDTO dto
    ) {

        try {

            StockDTO result =
                stockService.createStock(dto);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 전체 재고 조회
    @GetMapping
    public ResponseEntity<List<StockDTO>> findAll() {

        return ResponseEntity.ok(
            stockService.findAll()
        );
    }

    // 재고번호로 단건 조회
    @GetMapping("/{no}")
    public ResponseEntity<?> findByNo(
        @PathVariable("no") Long no
    ) {

        try {

            return ResponseEntity.ok(
                stockService.findByNo(no)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 상품옵션번호로 재고 조회
    @GetMapping("/option/{pono}")
    public ResponseEntity<?> findByOption(
        @PathVariable("pono") Long pono
    ) {

        try {

            return ResponseEntity.ok(
                stockService.findByOption(pono)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }

    // 재고수량 수정
    @PutMapping("/{no}")
    public ResponseEntity<?> updateStock(
        @PathVariable("no") Long no,
        @RequestBody StockDTO dto
    ) {

        try {

            return ResponseEntity.ok(
                stockService.updateStock(no, dto)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());
        }
    }
}