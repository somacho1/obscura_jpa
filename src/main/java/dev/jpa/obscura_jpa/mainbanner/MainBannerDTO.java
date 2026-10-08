package dev.jpa.obscura_jpa.mainbanner;

import java.time.LocalDateTime;
import lombok.*;

/** 메인 배너 등록·수정 요청 및 조회 응답 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MainBannerDTO {

    private Long no;
    private String name;
    private String imageUrl;
    private String altText;
    private Integer statusNo;
    private Integer seqNo;
    private LocalDateTime cdate;
}