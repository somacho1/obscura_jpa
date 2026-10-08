package dev.jpa.obscura_jpa.productimage;

import dev.jpa.obscura_jpa.auth.RequireRole;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/product-images")

public class ProductImageController {
    private static final int MAX_DETAIL_IMAGES = 10;
    private final ProductImageService productImageService;

    public ProductImageController(ProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    // 이미지 URL을 직접 전달해 상품 이미지를 등록하는 기존 API
    @RequireRole
    @PostMapping
    public ResponseEntity<ProductImageDTO> createImage(@RequestBody ProductImageDTO dto) {
        return ResponseEntity.ok(productImageService.createImage(dto));
    }

    // 대표 이미지 업로드: 원본 저장 후 4:5 이미지로 가공해 등록
    @RequireRole
    @PostMapping("/upload/main")
    public ResponseEntity<ProductImageDTO> uploadMainImage(
        @RequestParam("pno") Long pno,
        @RequestParam("file") MultipartFile file
    ) {
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

    // 상세 이미지 여러 장 업로드: 원본 비율을 유지하고 순서대로 등록
    @RequireRole
    @PostMapping("/upload/detail")
    public ResponseEntity<List<ProductImageDTO>> uploadDetailImages(
        @RequestParam("pno") Long pno,
        @RequestParam("files") List<MultipartFile> files
    ) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("업로드할 상세 이미지가 없습니다.");
        }
        if (files.size() > MAX_DETAIL_IMAGES) {
            throw new IllegalArgumentException("상세 이미지는 최대 10장까지 업로드할 수 있습니다.");
        }

        List<ProductImageDTO> result = new ArrayList<>();
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

    // 수정 화면에서 기존 이미지의 순서 또는 노출 여부를 변경
    @RequireRole
    @PutMapping("/{no}")
    public ResponseEntity<ProductImageDTO> updateImage(
        @PathVariable("no") Long no,
        @RequestBody ProductImageDTO dto
    ) {
        return ResponseEntity.ok(productImageService.updateImage(no, dto));
    }

    // 수정 화면에서 제거한 이미지의 DB 행을 삭제
    @RequireRole
    @DeleteMapping("/{no}")
    public ResponseEntity<Void> deleteImage(@PathVariable("no") Long no) {
        productImageService.deleteImage(no);
        return ResponseEntity.noContent().build();
    }

    // 이미지 번호로 단건 조회
    @GetMapping("/{no}")
    public ResponseEntity<ProductImageDTO> getImage(@PathVariable("no") Long no) {
        return ResponseEntity.ok(productImageService.findByNo(no));
    }

    // 상품의 전체 이미지 조회: 수정 화면에서 이미지 번호가 필요할 때 사용
    @GetMapping("/product/{pno}")
    public ResponseEntity<List<ProductImageDTO>> getImagesByProduct(@PathVariable("pno") Long pno) {
        return ResponseEntity.ok(productImageService.findByProduct(pno));
    }

    // 상품의 노출 이미지 조회
    @GetMapping("/product/{pno}/visible")
    public ResponseEntity<List<ProductImageDTO>> getVisibleImagesByProduct(@PathVariable("pno") Long pno) {
        return ResponseEntity.ok(productImageService.findVisibleByProduct(pno));
    }

    // 현재 대표 이미지 조회
    @GetMapping("/product/{pno}/main")
    public ResponseEntity<ProductImageDTO> getMainImage(@PathVariable("pno") Long pno) {
        return ResponseEntity.ok(productImageService.findMainImage(pno));
    }
}
