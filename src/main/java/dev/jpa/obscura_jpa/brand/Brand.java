package dev.jpa.obscura_jpa.brand;

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
 * 브랜드 Entity
 *
 * Oracle BRAND 테이블과 매핑된다.
 * 브랜드 기본정보와 화면 노출용 이미지를 관리한다.
 */
@Entity
@Table(name = "BRAND")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {

    /** 브랜드번호 */
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "brand_seq_generator"
    )
    @SequenceGenerator(
        name = "brand_seq_generator",
        sequenceName = "BRAND_SEQ",
        allocationSize = 1
    )
    @Column(name = "NO")
    private Long no;

    /** 브랜드명 - 중복 불가 */
    @Column(name = "NAME", nullable = false, unique = true, length = 100)
    private String name;

    /** 브랜드 로고 이미지 URL */
    @Column(name = "LOGOURL", length = 500)
    private String logoUrl;

    /** 브랜드 대표 이미지 URL */
    @Column(name = "VISUALURL", length = 500)
    private String visualUrl;

    /** 브랜드 설명 */
    @Column(name = "DETAIL", length = 1000)
    private String detail;

    /** 브랜드 상태: 0 비활성 / 1 활성 */
    @Column(name = "STATUSNO", nullable = false)
    private Integer statusNo;

    /** 브랜드 등록일시 */
    @Column(name = "CDATE", nullable = false)
    private LocalDateTime cdate;
}