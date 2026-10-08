package dev.jpa.obscura_jpa.auth;

import java.util.Objects;
import dev.jpa.obscura_jpa.member.*;
import jakarta.persistence.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminMemberService {
    private final SessionAuthService auth;
    private final EntityManager entities;
    private final ObMemberService members;
    private final JdbcTemplate jdbc;

    @Transactional
    public ObMemberDTO changeRole(HttpServletRequest request, Long no, String nextRole) {
        ObMember actor = auth.requireRole(request, "SUPER_ADMIN");
        if (!"USER".equals(nextRole) && !"ADMIN".equals(nextRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER 또는 ADMIN만 지정할 수 있습니다.");
        }
        if (Objects.equals(actor.getNo(), no)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인의 권한은 변경할 수 없습니다.");
        }

        // 동시에 변경 요청이 들어와도 이전 권한과 기록이 일치하도록 잠급니다.
        ObMember target = entities.find(ObMember.class, no, LockModeType.PESSIMISTIC_WRITE);
        if (target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }
        if ("SUPER_ADMIN".equals(target.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "최상위 관리자 권한은 변경할 수 없습니다.");
        }
        if (!Integer.valueOf(1).equals(target.getStatusNo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "정상 회원의 권한만 변경할 수 있습니다.");
        }
        String previousRole = target.getRole();
        if (!Objects.equals(previousRole, nextRole)) {
            target.setRole(nextRole);
            // 권한 변경과 이력 저장은 같은 트랜잭션으로 처리합니다.
            jdbc.update("""
                INSERT INTO OBMEMBERROLELOG (NO, ACTORNO, TARGETNO, OLDROLE, NEWROLE, CDATE)
                VALUES (OBMEMBERROLELOG_SEQ.NEXTVAL, ?, ?, ?, ?, SYSTIMESTAMP)
                """, actor.getNo(), no, previousRole, nextRole);
            entities.flush();
        }
        return members.findByNo(no).orElseThrow();
    }
}
