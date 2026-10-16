package dev.jpa.obscura_jpa.chat;

import java.util.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.jpa.obscura_jpa.product.Product;
import dev.jpa.obscura_jpa.productimage.ProductImage;
import dev.jpa.obscura_jpa.productimage.ProductImageRepository;

@Service
@Transactional(readOnly = true)
public class ChatProductService {
    @PersistenceContext private EntityManager em;
    private final ProductImageRepository images;
    public ChatProductService(ProductImageRepository images) { this.images = images; }
    public record Item(Long no, String brand, String name, long price, long salePrice,
                       int discountRate, String imageUrl) {}

    // 모델이 실제 등록된 브랜드·태그를 보고 조건을 해석하도록 공개 분류 목록만 전달합니다.
    public String vocabulary() {
        List<String> brands = em.createQuery("select b.name from Brand b where b.statusNo = 1 order by b.name", String.class)
            .setMaxResults(200).getResultList();
        List<String> tags = em.createQuery("""
            select distinct t.tag from ProductTag t join t.product p
            where p.statusNo = 1 and (lower(t.tag) like 'style:%' or lower(t.tag) like 'type:%') order by t.tag
            """, String.class).setMaxResults(200).getResultList();
        return "등록 브랜드: " + brands + "\n등록 태그: " + tags;
    }

    public List<Item> search(ChatSearchIntent filter) {
        // 값은 모두 바인딩합니다. 동적 문자열은 서버에 정의된 조건/정렬 구문만 사용합니다.
        String sale = "floor(p.price * (100 - coalesce(p.discountRate, 0)) / 100.0)";
        StringBuilder jpql = new StringBuilder("select p from Product p join fetch p.brand join fetch p.category where p.statusNo = 1 and p.brand.statusNo = 1 and p.category.statusNo = 1");
        Map<String, Object> params = new LinkedHashMap<>();
        if (!filter.brand().isBlank()) { jpql.append(" and lower(p.brand.name) = :brand"); params.put("brand", filter.brand().toLowerCase(Locale.ROOT)); }
        if (!filter.category().isBlank()) { jpql.append(" and upper(p.category.name) = :category"); params.put("category", filter.category()); }
        if (!filter.keyword().isBlank()) {
            jpql.append(" and (lower(p.name) like :keyword escape '!' or lower(p.code) like :keyword escape '!')");
            params.put("keyword", "%" + escaped(filter.keyword().toLowerCase(Locale.ROOT)) + "%");
        }
        tags(jpql, params, "styles", "style:", filter.styles());
        tags(jpql, params, "types", "type:", filter.types());
        if (filter.minPrice() != null) { jpql.append(" and ").append(sale).append(filter.minExclusive() ? " > :min" : " >= :min"); params.put("min", filter.minPrice()); }
        if (filter.maxPrice() != null) { jpql.append(" and ").append(sale).append(filter.maxExclusive() ? " < :max" : " <= :max"); params.put("max", filter.maxPrice()); }
        if (filter.saleOnly()) jpql.append(" and coalesce(p.discountRate, 0) > 0");
        jpql.append(" order by ").append(switch (filter.sort()) {
            case "PRICE_LOW" -> sale + " asc";
            case "PRICE_HIGH" -> sale + " desc";
            case "DISCOUNT_HIGH" -> "coalesce(p.discountRate, 0) desc, " + sale + " asc";
            default -> "p.cdate desc";
        }).append(", p.no desc");
        var query = em.createQuery(jpql.toString(), Product.class);
        params.forEach(query::setParameter);
        return query.setMaxResults(3).getResultList().stream().map(p -> {
            int discount = p.getDiscountRate() == null ? 0 : p.getDiscountRate();
            String image = images.findFirstByProductNoAndImageTypeAndDisplayYnOrderBySeqNoAsc(p.getNo(), "MAIN", "Y")
                .map(ProductImage::getImageUrl).orElse(null);
            return new Item(p.getNo(), p.getBrand().getName(), p.getName(), p.getPrice(),
                p.getPrice() * (100L - discount) / 100L, discount, image);
        }).toList();
    }
    private static void tags(StringBuilder query, Map<String, Object> params, String param, String prefix, List<String> tags) {
        if (tags.isEmpty()) return;
        query.append(" and exists (select t.no from ProductTag t where t.product = p and lower(trim(t.tag)) in :").append(param).append(")");
        params.put(param, tags.stream().map(tag -> tag.startsWith(prefix) ? tag : prefix + tag).toList());
    }
    private static String escaped(String text) { return text.replace("!", "!!").replace("%", "!%").replace("_", "!_"); }
}
