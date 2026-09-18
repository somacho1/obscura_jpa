package dev.jpa.obscura_jpa.wishlist;

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
public class WishlistDTO {

    private Long no;
    private Long mno;
    private Long pno;
    private LocalDateTime cdate;
}