package dev.jpa.obscura_jpa.category;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상품 카테고리 DTO
 *
 * 카테고리 등록, 조회, 수정 시
 * Controller와 Service 사이에서 데이터를 전달한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDTO {

    /** 카테고리번호 */
    private Long no;

    /** 카테고리명 */
    private String name;

    /** 카테고리 상태: 0 비활성 / 1 활성 */
    private Integer statusNo;

    /** 화면 노출 순서 */
    private Integer seqNo;

    /** 카테고리 등록일시 */
    private LocalDateTime cdate;
}