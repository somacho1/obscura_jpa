package dev.jpa.obscura_jpa.productimage;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageDTO {

    private Long no;
    private Long pno;
    private String imageUrl;
    private String imageType;
    private String displayYn;
    private Integer seqNo;
    private LocalDateTime cdate;
}