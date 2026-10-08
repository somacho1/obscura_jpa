package dev.jpa.obscura_jpa.mainbanner;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MainBannerRepository extends JpaRepository<MainBanner, Long> {

    /** 관리자 목록: 노출 순서 → 배너번호 */
    List<MainBanner> findAllByOrderBySeqNoAscNoAsc();

    /** Hero 목록: 노출 중이며 이미지가 있는 배너만 조회 */
    List<MainBanner> findAllByStatusNoAndImageUrlIsNotNullOrderBySeqNoAscNoAsc(
        Integer statusNo
    );
}