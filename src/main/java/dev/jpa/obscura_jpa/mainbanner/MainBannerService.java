package dev.jpa.obscura_jpa.mainbanner;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import dev.jpa.obscura_jpa.tool.Upload;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MainBannerService {

    private final MainBannerRepository mainBannerRepository;

    /** 관리자 전체 목록 */
    @Transactional(readOnly = true)
    public List<MainBannerDTO> findAll() {
        return mainBannerRepository.findAllByOrderBySeqNoAscNoAsc()
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /** 메인 Hero에 표시할 배너 목록 */
    @Transactional(readOnly = true)
    public List<MainBannerDTO> findActiveBanners() {
        return mainBannerRepository
            .findAllByStatusNoAndImageUrlIsNotNullOrderBySeqNoAscNoAsc(1)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /** 배너 상세 조회 */
    @Transactional(readOnly = true)
    public MainBannerDTO findByNo(Long no) {
        return toDTO(getBanner(no));
    }

    /** 신규 배너 등록: 이미지 업로드 전이므로 숨김으로 시작 */
    public MainBannerDTO createBanner(MainBannerDTO dto) {
        String name = validateName(dto.getName());
        String altText = validateAltText(dto.getAltText());
        int seqNo = validateSeqNo(dto.getSeqNo());

        MainBanner banner = MainBanner.builder()
            .name(name)
            .altText(altText)
            .statusNo(0)
            .seqNo(seqNo)
            .build();

        return toDTO(mainBannerRepository.save(banner));
    }

    /** 배너명·대체 텍스트·노출 여부·순서를 수정합니다. */
    public MainBannerDTO updateBanner(Long no, MainBannerDTO dto) {
        MainBanner banner = getBanner(no);

        String name = validateName(dto.getName());
        String altText = validateAltText(dto.getAltText());
        int seqNo = validateSeqNo(dto.getSeqNo());

        // 상태를 보내지 않으면 기존 상태를 유지합니다.
        int statusNo = dto.getStatusNo() == null
            ? banner.getStatusNo()
            : dto.getStatusNo();

        if (statusNo != 0 && statusNo != 1) {
            throw new IllegalArgumentException("노출 상태는 0 또는 1이어야 합니다.");
        }

        if (statusNo == 1 && (
            banner.getImageUrl() == null || banner.getImageUrl().isBlank()
        )) {
            throw new IllegalArgumentException("이미지를 등록한 후 노출할 수 있습니다.");
        }

        banner.setName(name);
        banner.setAltText(altText);
        banner.setStatusNo(statusNo);
        banner.setSeqNo(seqNo);

        return toDTO(mainBannerRepository.save(banner));
    }

    /** 이미지 등록·교체: 원본 비율을 유지하고 크롭하지 않습니다. */
    public MainBannerDTO uploadImage(Long no, MultipartFile file) {
        MainBanner banner = getBanner(no);
        String imageUrl = Upload.saveBannerImage(file);

        banner.setImageUrl(imageUrl);
        return toDTO(mainBannerRepository.save(banner));
    }

    /** 이미지 연결을 제거하고 배너를 자동으로 숨깁니다. */
    public MainBannerDTO removeImage(Long no) {
        MainBanner banner = getBanner(no);

        banner.setStatusNo(0);
        banner.setImageUrl(null);

        return toDTO(mainBannerRepository.save(banner));
    }

    /** 배너 삭제: DB의 배너 정보를 삭제합니다. */
    public void deleteBanner(Long no) {
        MainBanner banner = getBanner(no);
        mainBannerRepository.delete(banner);
    }

    /** 존재하지 않는 배너는 404 응답 */
    private MainBanner getBanner(Long no) {
        return mainBannerRepository.findById(no)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "배너 정보를 찾을 수 없습니다."
            ));
    }

    /** 배너명 필수 검증 */
    private String validateName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("배너명을 입력해주세요.");
        }

        String name = value.trim();
        validateByteLength(name, 100, "배너명");
        return name;
    }

    /** 대체 텍스트는 선택 사항 */
    private String validateAltText(String value) {
        if (value == null || value.isBlank()) return null;

        String altText = value.trim();
        validateByteLength(altText, 300, "대체 텍스트");
        return altText;
    }

    /** NUMBER(9)에 맞춰 순서를 검증합니다. */
    private int validateSeqNo(Integer value) {
        int seqNo = value == null ? 0 : value;

        if (seqNo < 0 || seqNo > 999999999) {
            throw new IllegalArgumentException("노출 순서는 0~999999999 사이로 입력해주세요.");
        }

        return seqNo;
    }

    /** Oracle VARCHAR2 BYTE 기준에서 한글 입력 길이 초과를 방지합니다. */
    private void validateByteLength(String value, int maxBytes, String label) {
        if (value.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            throw new IllegalArgumentException(
                label + "은 UTF-8 기준 " + maxBytes + "바이트 이내로 입력해주세요."
            );
        }
    }

    private MainBannerDTO toDTO(MainBanner banner) {
        return MainBannerDTO.builder()
            .no(banner.getNo())
            .name(banner.getName())
            .imageUrl(banner.getImageUrl())
            .altText(banner.getAltText())
            .statusNo(banner.getStatusNo())
            .seqNo(banner.getSeqNo())
            .cdate(banner.getCdate())
            .build();
    }
}