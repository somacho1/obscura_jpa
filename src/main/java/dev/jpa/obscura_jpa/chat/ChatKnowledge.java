package dev.jpa.obscura_jpa.chat;

import java.util.List;
import java.util.Locale;

// 기존 React deliveryPolicy.ts에 표시된 정책을 상담용으로 옮긴 자료입니다.
// 운영정책 변경 시 이 자료도 함께 수정하세요. 이번 단계에서는 DB 검색을 사용하지 않습니다.
public final class ChatKnowledge {
    private ChatKnowledge() {}
    public record Faq(String title, List<String> keywords, String content) {}
    public static final List<Faq> FAQ = List.of(
        new Faq("배송 안내", List.of("배송", "택배", "출고", "언제", "며칠", "무료", "도착"),
            "CJ대한통운 배송. 배송비 3,500원, 총 금액 60,000원 이상 무료배송. 도서·산간 추가 비용 가능. 오후 1시 이전 주문 당일 출고 안내. 결제일로부터 평균 3일 이내 발송, 배송 2~5일 정도 소요. 주문량과 지역에 따라 지연될 수 있고 공휴일·휴일 배송 불가. 실제 주문의 출고나 도착일은 이 자료로 확인할 수 없음."),
        new Faq("교환·반품 안내", List.of("교환", "반품", "하자", "불량", "오배송", "변심", "사이즈"),
            "MYPAGE/주문내역에서 교환·반품 신청. 단순 변심은 수령 후 7일 이내, 미착용 및 상품·포장 상태 유지 필요. 사이트에는 단순 변심·사이즈 교환/반품비 3,000원으로 안내되어 있으며 소비자 귀책은 왕복 배송비 부담 안내도 있음. 따라서 실제 차감 총액을 임의 계산하지 말고 별도 문의 안내. 하자·오배송은 판매자 귀책으로 배송비 별도 차감 없이 처리 안내. 사용 후에는 처리 어려움. 반품 주소: 서울시 성동구 성수이로24길 36, 이공오. 현금 동봉 금지."),
        new Faq("교환·반품 제한 안내", List.of("교환", "반품", "포장", "착용", "사용", "맞춤", "훼손"),
            "사이트 안내 기준 수령 후 7일 경과, 착용·사용 및 상품가치 훼손, 재판매 곤란한 맞춤 제작, 도서, 사전 접수 없는 반송 등은 교환·반품 제한 안내 대상. 개별 사유의 최종 처리 가능 여부나 예외는 상담원이 확인해야 함."),
        new Faq("취소·환불 안내", List.of("취소", "환불", "카드", "검품"),
            "신용카드 취소·환불은 반품 완료 및 검품 이후 통상 영업일 3~6일 소요 안내. 카드사 처리 기간에 따라 다름. 실제 환불 진행 상태는 조회하지 않으며 취소·환불을 대신 실행하지 않음.")
    );
    public static List<Faq> search(String question) {
        String text = question.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        return FAQ.stream().filter(faq -> faq.keywords().stream().anyMatch(text::contains)).toList();
    }
}
