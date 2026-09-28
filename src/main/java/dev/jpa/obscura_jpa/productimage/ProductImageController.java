package dev.jpa.obscura_jpa.productimage;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import dev.jpa.obscura_jpa.tool.ImageUtil;
import dev.jpa.obscura_jpa.tool.Upload;

/**
 * 상품 이미지 Controller
 *
 * MAIN
 * - 원본 이미지 저장
 * - 4:5 중앙 크롭
 * - 1200 x 1500 이미지 생성
 * - 가공된 이미지 URL을 PRODUCTIMAGE에 저장
 *
 * DETAIL
 * - 원본 이미지 저장
 * - 원본 비율 유지
 * - PRODUCTIMAGE에 저장
 */
@RestController
@RequestMapping("/api/product-images")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductImageController {

    private static final int MAX_DETAIL_IMAGES = 10;

    private final ProductImageService productImageService;

    public ProductImageController(ProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    /**
     * 기존 상품 이미지 등록
     */
    @PostMapping
    public ResponseEntity<ProductImageDTO> createImage(@RequestBody ProductImageDTO dto) {
        return ResponseEntity.ok(productImageService.createImage(dto));
    }

    /**
     * MAIN 이미지 업로드
     *
     * 1. 이미지 검증
     * 2. 원본 저장
     * 3. 4:5 크롭
     * 4. 1200 x 1500 생성
     * 5. PRODUCTIMAGE 등록
     */
    @PostMapping("/upload/main")
    public ResponseEntity<ProductImageDTO> uploadMainImage(@RequestParam("pno") Long pno, @RequestParam("file") MultipartFile file) {
        String originalPath = Upload.saveOriginal(file);
        String mainImagePath = ImageUtil.createMainImage(originalPath);

        ProductImageDTO dto = ProductImageDTO.builder()
            .pno(pno)
            .imageUrl(mainImagePath)
            .imageType("MAIN")
            .displayYn("Y")
            .seqNo(1)
            .build();

        return ResponseEntity.ok(productImageService.createImage(dto));
    }

    /**
     * DETAIL 이미지 여러 장 업로드
     *
     * DETAIL 이미지는 크롭하지 않고 원본 비율을 유지한다.
     */
    @PostMapping("/upload/detail")
    public ResponseEntity<List<ProductImageDTO>> uploadDetailImages(@RequestParam("pno") Long pno, @RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("업로드할 상세 이미지가 없습니다.");
        }

        if (files.size() > MAX_DETAIL_IMAGES) {
            throw new IllegalArgumentException("상세 이미지는 최대 10장까지 업로드할 수 있습니다.");
        }

        List<ProductImageDTO> result = new java.util.ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            String imagePath = Upload.saveOriginal(files.get(i));

            ProductImageDTO dto = ProductImageDTO.builder()
                .pno(pno)
                .imageUrl(imagePath)
                .imageType("DETAIL")
                .displayYn("Y")
                .seqNo(i + 1)
                .build();

            result.add(productImageService.createImage(dto));
        }

        return ResponseEntity.ok(result);
    }

 // 이미지 번호로 조회
    @GetMapping("/{no}")
    public ResponseEntity<ProductImageDTO> getImage(@PathVariable("no") Long no) {
        return ResponseEntity.ok(productImageService.findByNo(no));
    }

    // 상품의 전체 이미지 조회
    @GetMapping("/product/{pno}")
    public ResponseEntity<List<ProductImageDTO>> getImagesByProduct(@PathVariable("pno") Long pno) {
        return ResponseEntity.ok(productImageService.findByProduct(pno));
    }

    // 상품의 노출 이미지 조회
    @GetMapping("/product/{pno}/visible")
    public ResponseEntity<List<ProductImageDTO>> getVisibleImagesByProduct(@PathVariable("pno") Long pno) {
        return ResponseEntity.ok(productImageService.findVisibleByProduct(pno));
    }

    // 상품 MAIN 이미지 조회
    @GetMapping("/product/{pno}/main")
    public ResponseEntity<ProductImageDTO> getMainImage(@PathVariable("pno") Long pno) {
        return ResponseEntity.ok(productImageService.findMainImage(pno));
    }
   
}