package dev.jpa.obscura_jpa.mainbanner;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

/** 메인 배너 조회 및 관리 API */
@RestController
@RequestMapping("/api/main-banners")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://localhost:5174"
})
@RequiredArgsConstructor
public class MainBannerController {

    private final MainBannerService mainBannerService;

    /** 관리자 전체 목록 */
    @GetMapping
    public List<MainBannerDTO> findAll() {
        return mainBannerService.findAll();
    }

    /** Hero에서 사용할 노출 배너 목록 */
    @GetMapping("/active")
    public List<MainBannerDTO> findActiveBanners() {
        return mainBannerService.findActiveBanners();
    }

    /** 배너 상세 조회 */
    @GetMapping("/{no}")
    public MainBannerDTO findByNo(@PathVariable("no") Long no) {
        return mainBannerService.findByNo(no);
    }

    /** 배너 등록: 숨김 상태로 생성 */
    @PostMapping
    public ResponseEntity<MainBannerDTO> createBanner(
        @RequestBody MainBannerDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mainBannerService.createBanner(dto));
    }

    /** 배너 설정 수정 */
    @PutMapping("/{no}")
    public MainBannerDTO updateBanner(
        @PathVariable("no") Long no,
        @RequestBody MainBannerDTO dto
    ) {
        return mainBannerService.updateBanner(no, dto);
    }

    /** 이미지 등록·교체: multipart 필드명은 file */
    @PostMapping(
        value = "/{no}/image",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public MainBannerDTO uploadImage(
        @PathVariable("no") Long no,
        @RequestParam("file") MultipartFile file
    ) {
        return mainBannerService.uploadImage(no, file);
    }

    /** 이미지 연결 제거 및 자동 숨김 */
    @DeleteMapping("/{no}/image")
    public MainBannerDTO removeImage(@PathVariable("no") Long no) {
        return mainBannerService.removeImage(no);
    }

    /** 배너 정보 삭제 */
    @DeleteMapping("/{no}")
    public ResponseEntity<Void> deleteBanner(@PathVariable("no") Long no) {
        mainBannerService.deleteBanner(no);
        return ResponseEntity.noContent().build();
    }

    /** 입력값 검증 실패는 400과 안내 문구 반환 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(
        IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}