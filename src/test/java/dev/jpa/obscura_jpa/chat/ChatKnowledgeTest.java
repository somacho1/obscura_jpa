package dev.jpa.obscura_jpa.chat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatKnowledgeTest {
    @Test void shippingQuestionFindsPublishedFee() {
        var found = ChatKnowledge.search("택배비가 얼마인가요?");
        assertTrue(found.stream().anyMatch(f -> f.content().contains("3,500원") && f.content().contains("60,000원")));
    }
    @Test void returnQuestionIncludesRestrictions() {
        var titles = ChatKnowledge.search("반품할 수 있나요?").stream().map(ChatKnowledge.Faq::title).toList();
        assertTrue(titles.contains("교환·반품 안내"));
        assertTrue(titles.contains("교환·반품 제한 안내"));
    }
    @Test void unrelatedQuestionHasNoGrounding() {
        assertTrue(ChatKnowledge.search("서울 날씨를 알려주세요").isEmpty());
    }
    @Test void unsupportedInventoryLookupHasNoGrounding() {
        assertTrue(ChatKnowledge.search("이 상품 재고 몇 개 남았나요?").isEmpty());
    }
}
