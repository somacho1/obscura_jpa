package dev.jpa.obscura_jpa.recommendation;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {
    private final JdbcTemplate jdbc;
    public RecommendationService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    // 결제 완료 이후 주문 중 취소되지 않고 실제 남아 있는 상품만 구매 이력/인기도에 사용합니다.
    private static final String PAID_ITEMS = """
        FROM ORDERITEM I JOIN ORDERS O ON O.NO=I.ORDNO
        JOIN PRODUCTOPTION PO ON PO.NO=I.PONO
        WHERE O.STATUSNO IN (2,3,4,5) AND NVL(O.CANCELSTATUSNO,0) NOT IN (2,3)
          AND I.STATUSNO<>0 AND I.QTY-NVL(I.CANCELQTY,0)>0
        """;

    @Transactional(readOnly = true)
    public RecommendationRanker.Result recommend(Long memberNo, int limit) {
        Set<Long> wished = new HashSet<>(), purchased = new HashSet<>();
        if (memberNo != null) {
            wished.addAll(jdbc.query("SELECT DISTINCT PNO FROM WISHLIST WHERE MNO=?", (rs,n) -> rs.getLong(1), memberNo));
            purchased.addAll(jdbc.query("SELECT DISTINCT PO.PNO " + PAID_ITEMS + " AND O.MNO=?", (rs,n) -> rs.getLong(1), memberNo));
        }
        // 태그/브랜드/카테고리 프로필을 계산하기 위해 판매중지된 과거 상품도 함께 읽습니다.
        // CLOB 설명과 사이즈표는 읽지 않습니다. 상품별 개별 API 호출도 하지 않습니다.
        String sql = """
            SELECT P.NO,P.BNO,P.CNO,P.NAME,
              (SELECT MAX(I.IMAGEURL) KEEP (DENSE_RANK FIRST ORDER BY I.SEQNO,I.NO)
               FROM PRODUCTIMAGE I WHERE I.PNO=P.NO AND I.IMAGETYPE='MAIN' AND I.DISPLAYYN='Y') AS MAINIMAGEURL,
              CASE WHEN P.STATUSNO=1 AND B.STATUSNO=1 AND C.STATUSNO=1
                AND EXISTS (SELECT 1 FROM PRODUCTOPTION X JOIN STOCK S ON S.PONO=X.NO
                            WHERE X.PNO=P.NO AND X.USEYN='Y' AND S.QTY>0)
                THEN 1 ELSE 0 END AS AVAILABLE,
              NVL(SALES.POPULARITY,0) AS POPULARITY
            FROM PRODUCT P JOIN BRAND B ON B.NO=P.BNO JOIN CATEGORY C ON C.NO=P.CNO
            LEFT JOIN (SELECT PO.PNO,SUM(I.QTY-NVL(I.CANCELQTY,0)) AS POPULARITY
            """ + PAID_ITEMS + " GROUP BY PO.PNO) SALES ON SALES.PNO=P.NO";
        List<RecommendationRanker.Product> products = jdbc.query(sql, (rs,n) -> new RecommendationRanker.Product(
            rs.getLong("NO"), rs.getLong("BNO"), rs.getLong("CNO"), rs.getString("NAME"),
            rs.getString("MAINIMAGEURL"), rs.getInt("AVAILABLE")==1, rs.getLong("POPULARITY")));
        List<RecommendationRanker.Brand> brands = jdbc.query("SELECT NO,NAME,TOPBRANDYN,TOPSEQNO FROM BRAND WHERE STATUSNO=1",
            (rs,n) -> new RecommendationRanker.Brand(rs.getLong("NO"),rs.getString("NAME"),rs.getString("TOPBRANDYN"),rs.getInt("TOPSEQNO")));
        Map<Long, Set<String>> tags = new HashMap<>();
        // SCORE의 크기는 분류별로 비교 가능하지 않으므로, 관리자 확인을 거친 태그의 존재를 사용합니다.
        // 직접 입력하여 SCORE=0인 태그도 같은 기준으로 추천에 반영합니다.
        jdbc.query("SELECT PNO,TAG FROM PRODUCTTAG", rs -> {
            String tag = rs.getString("TAG");
            if (tag != null) tags.computeIfAbsent(rs.getLong("PNO"), key -> new HashSet<>()).add(tag.trim().toLowerCase(Locale.ROOT));
        });
        return RecommendationRanker.rank(products, brands, tags, wished, purchased, limit);
    }
}
