package dev.jpa.obscura_jpa.category;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상품 카테고리 Entity
 *
 * Oracle CATEGORY 테이블과 매핑된다.
 * 상품 분류 및 화면 노출 순서를 관리한다.
 */
@Entity
@Table(name = "CATEGORY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    /** 카테고리번호 */
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "category_seq_generator"
    )
    @SequenceGenerator(
        name = "category_seq_generator",
        sequenceName = "CATEGORY_SEQ",
        allocationSize = 1
    )
    @Column(name = "NO")
    private Long no;

    /** 카테고리명 */
    @Column(name = "NAME", nullable = false, unique = true, length = 50)
    private String name;

    /** 카테고리 상태: 0 비활성 / 1 활성 */
    @Column(name = "STATUSNO", nullable = false)
    private Integer statusNo;

    /** 화면 노출 순서 */
    @Column(name = "SEQNO", nullable = false)
    private Integer seqNo;

    /** 카테고리 등록일시 */
    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}