package dev.jpa.obscura_jpa.payment;

public record BankRefundRequest(
    Long mno,
    String reason,
    String bank,
    String account,
    String holder
) {
}