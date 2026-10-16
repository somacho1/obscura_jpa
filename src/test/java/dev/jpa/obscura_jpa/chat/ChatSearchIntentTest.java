package dev.jpa.obscura_jpa.chat;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatSearchIntentTest {
    private Map<String, Object> base() {
        Map<String, Object> m = new HashMap<>();
        m.put("intent", "PRODUCT"); m.put("brand", ""); m.put("category", "");
        m.put("styles", List.of()); m.put("types", List.of()); m.put("keyword", "");
        m.put("minPrice", null); m.put("maxPrice", null); m.put("sort", "PRICE_LOW");
        return m;
    }
    @Test void preservesUpperBudgetAndExclusiveBoundary() {
        var m = base(); m.put("maxPrice", 100000L); m.put("maxExclusive", true);
        var result = ChatSearchIntent.from(m);
        assertEquals(100000L, result.maxPrice()); assertTrue(result.maxExclusive());
        assertTrue(result.summary().contains("100,000원 미만"));
    }
    @Test void rejectsInvertedPriceRange() {
        var m = base(); m.put("minPrice", 200000L); m.put("maxPrice", 100000L);
        assertThrows(IllegalArgumentException.class, () -> ChatSearchIntent.from(m));
    }
    @Test void rejectsArbitrarySqlSort() {
        var m = base(); m.put("sort", "p.price; delete from Product");
        assertThrows(IllegalArgumentException.class, () -> ChatSearchIntent.from(m));
    }
    @Test void retainsUnknownStyleInsteadOfDroppingRequestedFilter() {
        var m = base(); m.put("styles", List.of("없는스타일"));
        assertEquals(List.of("없는스타일"), ChatSearchIntent.from(m).styles());
    }
    @Test void rejectsFractionalOrNegativePrice() {
        var m = base(); m.put("minPrice", -1L);
        assertThrows(IllegalArgumentException.class, () -> ChatSearchIntent.from(m));
        m.put("minPrice", 1000.5);
        assertThrows(IllegalArgumentException.class, () -> ChatSearchIntent.from(m));
    }
}
