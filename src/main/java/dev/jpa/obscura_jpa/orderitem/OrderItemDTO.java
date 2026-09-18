package dev.jpa.obscura_jpa.orderitem;

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
public class OrderItemDTO {

    private Long no;

    private Long ordno;

    private Long pono;

    private String productName;

    private String color;

    private String sizeValue;

    private Long price;

    private Long qty;

    private Integer statusNo;

    private Long cancelQty;

    private String cancelReason;

    private LocalDateTime cancelDate;

    private LocalDateTime cdate;
}