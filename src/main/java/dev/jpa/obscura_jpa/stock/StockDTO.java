package dev.jpa.obscura_jpa.stock;

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
public class StockDTO {

    private Long no;
    private Long pono;
    private Long qty;
    private LocalDateTime udate;
}