package dev.jpa.obscura_jpa.recommendation;

import java.util.*;

/** DB/웹 의존성이 없는 추천 계산. AI가 제안하고 관리자가 확정한 태그를 사용합니다. */
public final class RecommendationRanker {
    private RecommendationRanker() {}
    public record Product(long no, long bno, long cno, String name, String mainImageUrl, boolean available, long popularity) {}
    public record Brand(long no, String name, String topBrandYn, int topSeqNo) {}
    public record Pick(long no, long bno, String name, String mainImageUrl, double score, List<String> matchedTags) {}
    public record Result(String mode, List<Pick> products, List<Brand> brands) {}
    private static final Map<String, Double> GROUP_WEIGHT = Map.of("type", 1.0, "pattern", 0.5, "style", 2.0, "knit", 0.5);

    public static Result rank(List<Product> products, List<Brand> brands, Map<Long, Set<String>> tags,
                              Set<Long> wished, Set<Long> purchased, int limit) {
        Map<Long, Double> brandProfile = new HashMap<>(), categoryProfile = new HashMap<>();
        Map<String, Double> tagProfile = new HashMap<>();
        Set<Long> history = new HashSet<>(wished); history.addAll(purchased);
        for (Product product : products) {
            if (!history.contains(product.no())) continue;
            // 같은 상품의 중복 주문/옵션 수량으로 취향 점수가 과하게 커지지 않도록 상품 단위로 반영합니다.
            double weight = (wished.contains(product.no()) ? 2 : 0) + (purchased.contains(product.no()) ? 3 : 0);
            brandProfile.merge(product.bno(), weight, Double::sum);
            categoryProfile.merge(product.cno(), weight, Double::sum);
            vector(tags.getOrDefault(product.no(), Set.of())).forEach((key, value) -> tagProfile.merge(key, value * weight, Double::sum));
        }
        double brandMax = brandProfile.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double categoryMax = categoryProfile.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        boolean hasHistory = !brandProfile.isEmpty();
        List<Product> candidates = products.stream().filter(Product::available).filter(p -> !purchased.contains(p.no())).toList();
        Map<Long, Double> similarity = new HashMap<>();
        for (Product product : candidates) similarity.put(product.no(), cosine(tagProfile, vector(tags.getOrDefault(product.no(), Set.of()))));
        boolean tagMatch = similarity.values().stream().anyMatch(value -> value > 0);
        Map<Long, Double> scores = new HashMap<>();
        for (Product product : candidates) {
            double brand = brandProfile.getOrDefault(product.bno(), 0.0) / brandMax;
            double category = categoryProfile.getOrDefault(product.cno(), 0.0) / categoryMax;
            // 태그 일치가 있으면 태그를 우선합니다. 페이지 조회 시 모델을 재실행하지 않습니다.
            scores.put(product.no(), tagMatch ? similarity.get(product.no()) * 0.8 + brand * 0.15 + category * 0.05
                : hasHistory ? brand * 0.7 + category * 0.3 : 0);
        }
        Comparator<Product> order = Comparator.<Product>comparingDouble(p -> scores.get(p.no())).reversed()
            .thenComparing(Comparator.comparingLong(Product::popularity).reversed())
            .thenComparing(Comparator.comparingLong(Product::no).reversed());
        List<Pick> picks = candidates.stream().sorted(order).limit(limit).map(p -> new Pick(p.no(), p.bno(), p.name(), p.mainImageUrl(),
            Math.round(scores.get(p.no()) * 1_000_000.0) / 1_000_000.0,
            tags.getOrDefault(p.no(), Set.of()).stream().filter(tagProfile::containsKey)
                .sorted(Comparator.<String>comparingDouble(key -> tagProfile.get(key)).reversed().thenComparing(Comparator.naturalOrder()))
                .limit(3).map(key -> key.substring(key.indexOf(':') + 1)).toList())).toList();
        // 왼쪽 브랜드 목록은 최종 7개 상품에 한정하지 않고 전체 추천 가능한 브랜드에서 독립적으로 고릅니다.
        Set<Long> availableBrands = new HashSet<>(); candidates.forEach(p -> availableBrands.add(p.bno()));
        List<Brand> rankedBrands = brands.stream().filter(b -> availableBrands.contains(b.no()))
            .sorted(Comparator.<Brand>comparingDouble(b -> brandProfile.getOrDefault(b.no(), 0.0)).reversed()
                .thenComparingInt(b -> "Y".equals(b.topBrandYn()) ? 0 : 1).thenComparingInt(Brand::topSeqNo).thenComparingLong(Brand::no))
            .limit(8).toList();
        return new Result(tagMatch ? "AI_TAGS" : hasHistory ? "HISTORY" : "DEFAULT", picks, rankedBrands);
    }

    private static Map<String, Double> vector(Set<String> tags) {
        Map<String, Integer> counts = new HashMap<>();
        for (String tag : tags) { String group = group(tag); if (GROUP_WEIGHT.containsKey(group)) counts.merge(group, 1, Integer::sum); }
        Map<String, Double> result = new HashMap<>();
        for (String tag : tags) { String group = group(tag); if (GROUP_WEIGHT.containsKey(group)) result.put(tag, GROUP_WEIGHT.get(group) / counts.get(group)); }
        return result;
    }
    private static String group(String tag) { int index = tag.indexOf(':'); return index > 0 ? tag.substring(0,index) : ""; }
    private static double cosine(Map<String, Double> a, Map<String, Double> b) {
        double normA = a.values().stream().mapToDouble(v -> v*v).sum(), normB = b.values().stream().mapToDouble(v -> v*v).sum();
        if (normA == 0 || normB == 0) return 0;
        double dot = b.entrySet().stream().mapToDouble(e -> e.getValue() * a.getOrDefault(e.getKey(), 0.0)).sum();
        return Math.min(1, dot / Math.sqrt(normA * normB));
    }
}
