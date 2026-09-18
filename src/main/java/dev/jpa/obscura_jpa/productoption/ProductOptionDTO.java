package dev.jpa.obscura_jpa.productoption;

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
public class ProductOptionDTO {

    private Long no;
    private Long pno;
    private String color;
    private String sizeValue;
    private String useYn;
    private LocalDateTime cdate;
}