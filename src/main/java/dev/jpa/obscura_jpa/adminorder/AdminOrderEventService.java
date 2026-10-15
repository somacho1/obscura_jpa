package dev.jpa.obscura_jpa.adminorder;

import dev.jpa.obscura_jpa.auth.SessionAuthService;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AdminOrderEventService {
    private final JdbcTemplate jdbc;
    private final EntityManager entities;
    private final SessionAuthService auth;
    public AdminOrderEventService(JdbcTemplate jdbc, EntityManager entities, SessionAuthService auth) {
        this.jdbc=jdbc; this.entities=entities; this.auth=auth;
    }
    // 업무 처리와 같은 트랜잭션으로 기록하여 오류로 롤백되면 알림·이력도 함께 취소합니다.
    // 주문 생성 직후의 FK 참조를 위해 JPA 변경을 먼저 반영합니다. flush는 커밋이 아닙니다.
    @Transactional(propagation=Propagation.MANDATORY)
    public void record(Long orderNo, Long refundNo, String key, String type, String text, Long amount, boolean notify) {
        entities.flush();
        Long actor=null; String role="SYSTEM";
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            var member=auth.requireMember(attrs.getRequest()); actor=member.getNo(); role=member.getRole();
        }
        // 업무 서비스의 주문 잠금 아래에서 실행하며 고유키 제약도 중복 기록을 방지합니다.
        jdbc.update("""
            INSERT INTO ORDERLOG (NO,ORDNO,RNO,MNO,ACTORTYPE,EVENTKEY,TYPE,CONTENT,AMOUNT)
            SELECT ORDERLOG_SEQ.NEXTVAL,?,?,?,?,?,?,?,? FROM DUAL
            WHERE NOT EXISTS (SELECT 1 FROM ORDERLOG WHERE EVENTKEY=?)
            """,orderNo,refundNo,actor,role,key,type,text,amount,key);
        if (notify) jdbc.update("""
            INSERT INTO ADMINNOTICE (NO,ORDNO,RNO,EVENTKEY,TYPE,TITLE,AMOUNT)
            SELECT ADMINNOTICE_SEQ.NEXTVAL,?,?,?,?,?,? FROM DUAL
            WHERE NOT EXISTS (SELECT 1 FROM ADMINNOTICE WHERE EVENTKEY=?)
            """,orderNo,refundNo,key,type,text,amount,key);
    }
}
