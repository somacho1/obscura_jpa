package dev.jpa.obscura_jpa.payment;

// 금액·결제키는 클라이언트에서 받지 않고 DB에서 확인합니다.
public record TossCancelRequest(
    Long orderNo,
    Long mno,
    String reason
) {
}