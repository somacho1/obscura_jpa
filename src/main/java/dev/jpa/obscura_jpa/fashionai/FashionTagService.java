package dev.jpa.obscura_jpa.fashionai;

import dev.jpa.obscura_jpa.product.Product;
import dev.jpa.obscura_jpa.producttag.ProductTag;
import jakarta.persistence.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기존 PRODUCTTAG에 그룹:태그 형식으로 저장. 다른 수동 태그는 보존합니다. */
@Service
public class FashionTagService {
    @PersistenceContext private EntityManager em;
    private static final Set<String> GROUPS = Set.of("type", "pattern", "style", "knit");
    public record TagInput(String group, String label, BigDecimal score) {}

    @Transactional
    public List<TagInput> replace(Long pno, List<TagInput> input) {
        if (input == null || input.size() > 20) throw new IllegalArgumentException("태그는 최대 20개까지 저장할 수 있습니다.");
        // 모두 검증한 후 수정하여 오류 시 기존 태그를 유지합니다.
        Map<String, BigDecimal> requested = new LinkedHashMap<>();
        for (TagInput tag : input) {
            if (tag == null || !GROUPS.contains(tag.group() == null ? "" : tag.group())
                || tag.label() == null || tag.label().isBlank()) throw new IllegalArgumentException("태그 분류와 이름을 확인해주세요.");
            String label = tag.label().trim().toLowerCase(Locale.ROOT);
            String key = tag.group() + ":" + label;
            if (label.contains(":" ) || key.getBytes(StandardCharsets.UTF_8).length > 50)
                throw new IllegalArgumentException("태그 이름이 너무 길거나 사용할 수 없는 문자가 있습니다.");
            if (tag.score() == null || tag.score().compareTo(BigDecimal.ZERO) < 0 || tag.score().compareTo(BigDecimal.ONE) > 0)
                throw new IllegalArgumentException("저장 점수는 0~1 범위여야 합니다.");
            if (requested.put(key, tag.score().setScale(4, RoundingMode.HALF_UP)) != null)
                throw new IllegalArgumentException("동일한 태그가 중복되었습니다.");
        }
        // 같은 상품에 대한 동시 저장을 직렬화하여 중복행을 방지합니다.
        Product product = em.find(Product.class, pno, LockModeType.PESSIMISTIC_WRITE);
        if (product == null) throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        List<ProductTag> existing = em.createQuery("select t from ProductTag t where t.product.no = :pno", ProductTag.class)
            .setParameter("pno", pno).getResultList();
        for (ProductTag tag : existing) {
            if (!isManaged(tag.getTag())) continue;
            BigDecimal score = requested.remove(tag.getTag());
            if (score == null) em.remove(tag); else tag.setScore(score);
        }
        for (var entry : requested.entrySet()) {
            ProductTag tag = new ProductTag();
            tag.setProduct(product); tag.setTag(entry.getKey()); tag.setScore(entry.getValue()); tag.setCdate(LocalDateTime.now());
            em.persist(tag);
        }
        em.flush();
        return list(pno);
    }

    @Transactional(readOnly = true)
    public List<TagInput> list(Long pno) {
        if (em.find(Product.class, pno) == null) throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        return em.createQuery("select t from ProductTag t where t.product.no = :pno order by t.no", ProductTag.class)
            .setParameter("pno", pno).getResultList().stream().filter(t -> isManaged(t.getTag()))
            .map(t -> { int index = t.getTag().indexOf(':'); return new TagInput(t.getTag().substring(0,index), t.getTag().substring(index+1), t.getScore()); }).toList();
    }
    @Transactional(readOnly = true)
    public long countAll(Long pno) {
        if (em.find(Product.class, pno) == null) throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        return em.createQuery("select count(t) from ProductTag t where t.product.no = :pno", Long.class)
            .setParameter("pno", pno).getSingleResult();
    }
    @Transactional
    public List<TagInput> saveIfEmpty(Long pno, List<TagInput> input) {
        if (em.find(Product.class, pno, LockModeType.PESSIMISTIC_WRITE) == null)
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        if (countAll(pno) > 0) throw new IllegalStateException("이미 저장된 태그가 있어 보호했습니다. 상품 수정 화면에서 확인해주세요.");
        if (input == null || input.isEmpty()) throw new IllegalArgumentException("저장할 태그를 선택해주세요.");
        return replace(pno, input);
    }
    private boolean isManaged(String tag) {
        int index = tag.indexOf(':');
        return index > 0 && GROUPS.contains(tag.substring(0,index));
    }
}
