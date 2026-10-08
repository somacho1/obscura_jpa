package dev.jpa.obscura_jpa.auth;

import java.util.Objects;
import dev.jpa.obscura_jpa.member.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SessionAuthService {
    private static final String MEMBER_KEY = "OBSCURA_AUTH_MEMBER_NO";
    private final ObMemberRepository members;

    // 세션에는 권한이 아닌 회원번호만 저장합니다. 기존 세션은 폐기합니다.
    public void signIn(HttpServletRequest request, Long no) {
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = request.getSession(true);
        session.setAttribute(MEMBER_KEY, no);
        session.setMaxInactiveInterval(1800);
    }

    // 브라우저가 보내는 회원번호·ROLE을 인증 근거로 사용하지 않습니다.
    // 매 요청 DB를 확인하므로 권한 회수와 회원 정지가 바로 반영됩니다.
    public ObMember requireMember(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object no = session == null ? null : session.getAttribute(MEMBER_KEY);
        if (!(no instanceof Long memberNo)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        ObMember member = members.findById(memberNo).orElse(null);
        if (member == null || !Integer.valueOf(1).equals(member.getStatusNo())) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인할 수 없는 회원입니다.");
        }
        return member;
    }

    public ObMember requireRole(HttpServletRequest request, String role) {
        ObMember member = requireMember(request);
        boolean permitted = "SUPER_ADMIN".equals(member.getRole())
            || ("ADMIN".equals(role) && "ADMIN".equals(member.getRole()));
        if (!permitted) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }
        return member;
    }

    // 개인 정보 수정·탈퇴는 로그인한 본인의 회원번호만 허용합니다.
    public void requireOwner(HttpServletRequest request, Long no) {
        if (!Objects.equals(requireMember(request).getNo(), no)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 계정만 접근할 수 있습니다.");
        }
    }

    public LoginResponseDTO current(HttpServletRequest request) {
        ObMember member = requireMember(request);
        return LoginResponseDTO.builder().no(member.getNo()).id(member.getId())
            .name(member.getName()).email(member.getEmail()).role(member.getRole()).build();
    }

    public void signOut(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }
}
