package dev.jpa.obscura_jpa.chat;

import java.util.*;

// AI는 검색 조건만 생성합니다. SQL·URL·권한은 생성하거나 실행하지 않습니다.
public record ChatSearchIntent(String intent, String brand, String category, List<String> styles,
    List<String> types, String keyword, Long minPrice, Long maxPrice,
    boolean minExclusive, boolean maxExclusive, boolean saleOnly, String sort) {
    public static ChatSearchIntent from(Map<String, Object> data) {
        String intent = text(data, "intent");
        String sort = text(data, "sort");
        String category = text(data, "category").toUpperCase(Locale.ROOT);
        if (!Set.of("PRODUCT", "POLICY", "STYLE_ADVICE", "OTHER", "ORDER_STATUS").contains(intent)
            || !Set.of("PRICE_LOW", "PRICE_HIGH", "DISCOUNT_HIGH", "LATEST").contains(sort)
            || !Set.of("", "MEN", "WOMEN", "SHOES", "ACC").contains(category))
            throw new IllegalArgumentException("검색 조건을 해석하지 못했습니다.");
        Long min = price(data.get("minPrice")); Long max = price(data.get("maxPrice"));
        if (min != null && max != null && min > max) throw new IllegalArgumentException("최소 가격이 최대 가격보다 높습니다.");
        return new ChatSearchIntent(intent, text(data, "brand"), category, values(data.get("styles")),
            values(data.get("types")), text(data, "keyword"), min, max,
            Boolean.TRUE.equals(data.get("minExclusive")), Boolean.TRUE.equals(data.get("maxExclusive")),
            Boolean.TRUE.equals(data.get("saleOnly")), sort);
    }
    private static String text(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (!(value instanceof String s) || s.length() > 100) throw new IllegalArgumentException("검색 조건을 확인해 주세요.");
        return s.trim();
    }
    private static Long price(Object value) {
        if (value == null) return null;
        if (!(value instanceof Number n)) throw new IllegalArgumentException("가격 조건이 올바르지 않습니다.");
        long result = n.longValue();
        if (result < 0 || result > 1_000_000_000_000L || n.doubleValue() != (double) result)
            throw new IllegalArgumentException("가격 조건이 올바르지 않습니다.");
        return result;
    }
    private static List<String> values(Object value) {
        if (!(value instanceof List<?> list) || list.size() > 10) throw new IllegalArgumentException("태그 조건을 확인해 주세요.");
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String s) || s.isBlank() || s.length() > 50) throw new IllegalArgumentException("태그 조건이 올바르지 않습니다.");
            result.add(s.trim().toLowerCase(Locale.ROOT));
        }
        return result.stream().distinct().toList();
    }
    public String summary() {
        List<String> parts = new ArrayList<>();
        if (!brand.isBlank()) parts.add(brand);
        if (!category.isBlank()) parts.add(category);
        parts.addAll(styles); parts.addAll(types);
        if (!keyword.isBlank()) parts.add(keyword);
        if (minPrice != null) parts.add(String.format("%,d원 %s", minPrice, minExclusive ? "초과" : "이상"));
        if (maxPrice != null) parts.add(String.format("%,d원 %s", maxPrice, maxExclusive ? "미만" : "이하"));
        if (saleOnly) parts.add("할인 상품");
        parts.add(switch (sort) { case "PRICE_LOW" -> "낮은 가격순"; case "PRICE_HIGH" -> "높은 가격순"; case "DISCOUNT_HIGH" -> "높은 할인율순"; default -> "최신순"; });
        return String.join(" · ", parts);
    }
}
