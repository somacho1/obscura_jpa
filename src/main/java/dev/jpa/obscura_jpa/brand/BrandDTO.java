package dev.jpa.obscura_jpa.brand;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 브랜드 DTO
 *
 * 브랜드 등록, 조회, 수정 시
 * Controller와 Service 사이에서 데이터를 전달한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandDTO {

    /** 브랜드번호 */
    private Long no;

    /** 브랜드명 */
    private String name;

    /** 브랜드 로고 이미지 URL */
    private String logoUrl;

    /** 브랜드 대표 이미지 URL */
    private String visualUrl;

    /** 브랜드 설명 */
    private String detail;

    /** 브랜드 상태: 0 비활성 / 1 활성 */
    private Integer statusNo;

    /** 브랜드 등록일시 */
    private LocalDateTime cdate;
}