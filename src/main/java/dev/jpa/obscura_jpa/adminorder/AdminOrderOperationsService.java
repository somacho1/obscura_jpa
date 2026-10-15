package dev.jpa.obscura_jpa.adminorder;

import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly=true)
public class AdminOrderOperationsService {
    private final JdbcTemplate jdbc;
    public AdminOrderOperationsService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    // 읽음과 무관하게 실제 주문·결제·환불 상태에서 대기 업무를 집계합니다.
    private static final String BANK_WAIT="O.STATUSNO=1 AND O.CANCELSTATUSNO=0 AND P.METHOD='BANK' AND P.STATUSNO=0";
    private static final String LEGACY="NOT EXISTS (SELECT 1 FROM REFUND R WHERE R.ORDNO=O.NO)";
    private static final String BANK_REFUND="(EXISTS (SELECT 1 FROM REFUND R WHERE R.ORDNO=O.NO AND R.METHOD='BANK' AND R.STATUSNO=0) OR (O.CANCELSTATUSNO=3 AND P.METHOD='BANK' AND P.STATUSNO=1 AND "+LEGACY+"))";
    private static final String CHECK="(EXISTS (SELECT 1 FROM REFUND R WHERE R.ORDNO=O.NO AND R.METHOD='TOSS' AND R.STATUSNO=0) OR (O.CANCELSTATUSNO=3 AND P.METHOD='TOSS' AND "+LEGACY+") OR (O.STATUSNO=1 AND P.METHOD='TOSS' AND P.STATUSNO=0 AND P.PAYMENTKEY IS NOT NULL))";
    private static final String SHIPPING="O.STATUSNO IN (2,3,4) AND O.CANCELSTATUSNO IN (0,1) AND P.STATUSNO IN (1,2) AND EXISTS (SELECT 1 FROM DELIVERY D JOIN DELIVERYITEM DI ON DI.DNO=D.NO WHERE D.ORDNO=O.NO AND D.STATUSNO=0 AND D.SHIPDATE IS NULL AND DI.QTY>0)";
    private static final String UNREAD="EXISTS (SELECT 1 FROM ADMINNOTICE N WHERE N.ORDNO=O.NO AND N.READYN='N')";
    private static final String FROM=" FROM ORDERS O LEFT JOIN PAYMENT P ON P.ORDNO=O.NO ";
    private static final Map<String,String> FILTERS=Map.of("ALL","1=1","BANK",BANK_WAIT,"SHIPPING",SHIPPING,"REFUND",BANK_REFUND,"CHECK",CHECK,"UNREAD",UNREAD);

    public record Summary(long bankWaiting,long shippingWaiting,long refundWaiting,long checkNeeded,long unreadNotices) {}
    public record PageResult(List<Map<String,Object>> content,long totalElements,int totalPages,int number,int size) {}
    public record DetailResult(List<Map<String,Object>> notices,PageResult logs) {}
    public Summary summary() {
        return new Summary(count("SELECT COUNT(*)"+FROM+"WHERE "+BANK_WAIT),count("SELECT COUNT(*)"+FROM+"WHERE "+SHIPPING),
            count("SELECT COUNT(*)"+FROM+"WHERE "+BANK_REFUND),count("SELECT COUNT(*)"+FROM+"WHERE "+CHECK),
            count("SELECT COUNT(*) FROM ADMINNOTICE WHERE READYN='N'"));
    }
    private long count(String sql,Object... args) { return jdbc.queryForObject(sql,Long.class,args); }
    private List<Map<String,Object>> rows(String sql,Object... args) {
        var result=jdbc.queryForList(sql,args);
        result.forEach(row -> row.replaceAll((key,value) -> value instanceof Timestamp date ? date.toLocalDateTime().toString() : value));
        return result;
    }
    private void validatePage(int page,int size) {
        if(page<1 || page>1000000 || size<1 || size>100) throw new IllegalArgumentException("페이지는 1 이상, 조회 개수는 1~100입니다.");
    }
    private static final String NOTICE_SELECT="""
        SELECT N.NO AS "no",N.ORDNO AS "orderNo",N.RNO AS "refundNo",N.TYPE AS "type",
        N.TITLE AS "title",N.AMOUNT AS "amount",N.READYN AS "readYn",N.CDATE AS "cdate",
        N.READDATE AS "readDate",M.NAME AS "readerName",
        CASE
          WHEN N.RNO IS NOT NULL AND R.STATUSNO=1 THEN '환불 완료'
          WHEN N.RNO IS NOT NULL AND R.METHOD='BANK' THEN '송금 대기'
          WHEN N.RNO IS NOT NULL THEN '결과 확인 필요'
          WHEN N.TYPE='LEGACY_REFUND_REQUEST' AND P.STATUSNO=3 THEN '환불 완료'
          WHEN N.TYPE='LEGACY_REFUND_REQUEST' AND P.METHOD='BANK' THEN '송금 대기'
          WHEN N.TYPE='LEGACY_REFUND_REQUEST' THEN '결과 확인 필요'
          WHEN N.TYPE='BANK_APPLIED' AND P.STATUSNO=0 AND O.STATUSNO=1 THEN '입금 대기'
          WHEN N.TYPE='PAYMENT_CHECK' AND P.STATUSNO=0 AND O.STATUSNO=1 THEN '승인 결과 확인 필요'
          WHEN O.STATUSNO=0 THEN '주문 취소'
          ELSE '확인할 이력' END AS "processState"
        FROM ADMINNOTICE N JOIN ORDERS O ON O.NO=N.ORDNO
        LEFT JOIN PAYMENT P ON P.ORDNO=O.NO LEFT JOIN REFUND R ON R.NO=N.RNO
        LEFT JOIN OBMEMBER M ON M.NO=N.CHECKMNO
        """;
    public PageResult notices(boolean unread,int page,int size) {
        validatePage(page,size); String where=unread?" WHERE N.READYN='N' ":" ";
        long total=count("SELECT COUNT(*) FROM ADMINNOTICE N"+where);
        return new PageResult(rows(NOTICE_SELECT+where+" ORDER BY N.CDATE DESC,N.NO DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",(page-1)*size,size),total,(int)((total+size-1)/size),page-1,size);
    }
    public PageResult orders(String filter,boolean unread,int page,int size) {
        validatePage(page,size);String rule=FILTERS.get(filter);
        if(rule==null) throw new IllegalArgumentException("올바른 주문 필터가 필요합니다.");
        String where=" WHERE "+rule+(unread?" AND "+UNREAD:"");
        long total=count("SELECT COUNT(*)"+FROM+where);
        String select="""
            SELECT O.NO AS "no",O.MNO AS "memberNo",M.NAME AS "memberName",O.CDATE AS "cdate",
            O.TOTALPRICE AS "totalPrice",O.STATUSNO AS "statusNo",O.CANCELSTATUSNO AS "cancelStatusNo",
            P.METHOD AS "method",P.STATUSNO AS "paymentStatusNo",
            (SELECT MAX(I.PRODUCTNAME) KEEP (DENSE_RANK FIRST ORDER BY I.NO) FROM ORDERITEM I WHERE I.ORDNO=O.NO) AS "productName",
            (SELECT COUNT(*) FROM ADMINNOTICE N WHERE N.ORDNO=O.NO AND N.READYN='N') AS "unreadCount",
            (SELECT COUNT(*) FROM REFUND R WHERE R.ORDNO=O.NO AND R.STATUSNO=0) AS "pendingRefundCount"
            """;
        return new PageResult(rows(select+FROM+" LEFT JOIN OBMEMBER M ON M.NO=O.MNO "+where+
            " ORDER BY O.CDATE DESC,O.NO DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",(page-1)*size,size),total,(int)((total+size-1)/size),page-1,size);
    }
    public DetailResult detail(Long orderNo,int page,int size,Long noticeNo) {
        validatePage(page,size);
        if(count("SELECT COUNT(*) FROM ORDERS WHERE NO=?",orderNo)==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"주문이 없습니다.");
        long total=count("SELECT COUNT(*) FROM ORDERLOG WHERE ORDNO=?",orderNo);
        var logs=rows("""
            SELECT L.NO AS "no",L.RNO AS "refundNo",L.TYPE AS "type",L.CONTENT AS "content",L.AMOUNT AS "amount",
            L.ACTORTYPE AS "actorType",M.NAME AS "actorName",L.CDATE AS "cdate"
            FROM ORDERLOG L LEFT JOIN OBMEMBER M ON M.NO=L.MNO WHERE L.ORDNO=?
            ORDER BY L.CDATE DESC,L.NO DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """,orderNo,(page-1)*size,size);
        // 알림은 최신 100건씩 이력 화면에서 페이지로 조회할 수 있고, 상세에서는 최근 100건을 표시합니다.
        var notices=rows(NOTICE_SELECT+" WHERE N.ORDNO=? ORDER BY N.CDATE DESC,N.NO DESC FETCH FIRST 100 ROWS ONLY",orderNo);
        // 이전 알림 페이지에서 클릭한 요청도 상세에 포함해 읽음 처리가 누락되지 않도록 합니다.
        if(noticeNo!=null && noticeNo>0 && notices.stream().noneMatch(n -> ((Number)n.get("no")).longValue()==noticeNo)) {
            var selected=rows(NOTICE_SELECT+" WHERE N.ORDNO=? AND N.NO=?",orderNo,noticeNo);
            if(!selected.isEmpty()) { if(notices.size()==100) notices.remove(99); notices.add(0,selected.get(0)); }
        }
        return new DetailResult(notices,new PageResult(logs,total,(int)((total+size-1)/size),page-1,size));
    }
    @Transactional
    public int read(Long orderNo,List<Long> ids,Long adminNo) {
        if(ids==null || ids.size()>100 || ids.stream().anyMatch(no -> no==null || no<=0)) throw new IllegalArgumentException("확인할 알림번호를 확인해주세요.");
        int changed=0;
        // 실제 표시된 알림만 확인합니다. 늦게 도착한 새 요청을 함께 읽음 처리하지 않습니다.
        for(Long no:new LinkedHashSet<>(ids)) changed+=jdbc.update(
            "UPDATE ADMINNOTICE SET READYN='Y',CHECKMNO=?,READDATE=SYSTIMESTAMP WHERE NO=? AND ORDNO=? AND READYN='N'",adminNo,no,orderNo);
        return changed;
    }
}
