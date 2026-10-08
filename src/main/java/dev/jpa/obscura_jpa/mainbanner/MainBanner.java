package dev.jpa.obscura_jpa.mainbanner;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

/** Oracle MAINBANNER 테이블과 매핑되는 메인 배너 Entity */
@Entity
@Table(name = "MAINBANNER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MainBanner {

    /** 배너번호: Oracle 시퀀스로 생성 */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mainbanner_seq_generator")
    @SequenceGenerator(
        name = "mainbanner_seq_generator",
        sequenceName = "MAINBANNER_SEQ",
        allocationSize = 1
    )
    @Column(name = "NO", precision = 10, scale = 0)
    private Long no;

    /** 관리자에서 배너를 구분하는 이름 */
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    /** PC·태블릿·모바일 공통 이미지 경로 */
    @Column(name = "IMAGEURL", length = 500)
    private String imageUrl;

    /** 이미지 대체 텍스트 */
    @Column(name = "ALTTEXT", length = 300)
    private String altText;

    /** 0 숨김, 1 노출 */
    @Builder.Default
    @Column(name = "STATUSNO", nullable = false, precision = 1, scale = 0)
    private Integer statusNo = 0;

    /** 작은 값부터 표시 */
    @Builder.Default
    @Column(name = "SEQNO", nullable = false, precision = 9, scale = 0)
    private Integer seqNo = 0;

    /** 등록일시 */
    @Column(name = "CDATE", nullable = false, updatable = false)
    private LocalDateTime cdate;

    @PrePersist
    private void prePersist() {
        if (cdate == null) cdate = LocalDateTime.now();
        if (statusNo == null) statusNo = 0;
        if (seqNo == null) seqNo = 0;
    }
}