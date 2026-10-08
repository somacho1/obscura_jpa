package dev.jpa.obscura_jpa.auth;

import dev.jpa.obscura_jpa.member.*;
import jakarta.servlet.http.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// 서버·Oracle을 실행하지 않고 세션과 DB 권한 판정만 검증합니다.
class SessionAuthServiceTest {
    private ObMemberRepository repository;
    private HttpServletRequest request;
    private HttpSession session;
    private SessionAuthService auth;
    private ObMember member;

    @BeforeEach
    void setUp() {
        repository = mock(ObMemberRepository.class);
        request = mock(HttpServletRequest.class);
        session = mock(HttpSession.class);
        auth = new SessionAuthService(repository);
        member = ObMember.builder().no(4L).id("test").role("USER").statusNo(1).build();
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("OBSCURA_AUTH_MEMBER_NO")).thenReturn(4L);
        when(repository.findById(4L)).thenReturn(Optional.of(member));
    }

    private void denied(HttpStatus status, Runnable operation) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, operation::run);
        assertEquals(status, exception.getStatusCode());
    }

    @Test
    void anonymousCannotAccess() {
        when(request.getSession(false)).thenReturn(null);
        denied(HttpStatus.UNAUTHORIZED, () -> auth.requireRole(request, "ADMIN"));
    }

    @Test
    void userCannotAccessAdmin() {
        denied(HttpStatus.FORBIDDEN, () -> auth.requireRole(request, "ADMIN"));
    }

    @Test
    void adminCannotManageRoles() {
        member.setRole("ADMIN");
        assertSame(member, auth.requireRole(request, "ADMIN"));
        denied(HttpStatus.FORBIDDEN, () -> auth.requireRole(request, "SUPER_ADMIN"));
    }

    @Test
    void superAdminCanUseBothAdminLevels() {
        member.setRole("SUPER_ADMIN");
        assertSame(member, auth.requireRole(request, "ADMIN"));
        assertSame(member, auth.requireRole(request, "SUPER_ADMIN"));
    }

    @Test
    void revocationIsAppliedWithoutNewLogin() {
        member.setRole("ADMIN");
        auth.requireRole(request, "ADMIN");
        member.setRole("USER");
        denied(HttpStatus.FORBIDDEN, () -> auth.requireRole(request, "ADMIN"));
        verify(repository, times(2)).findById(4L);
    }

    @Test
    void suspendedAccountLosesSession() {
        member.setStatusNo(2);
        denied(HttpStatus.UNAUTHORIZED, () -> auth.requireMember(request));
        verify(session).invalidate();
    }

    @Test
    void memberCannotModifyAnotherProfile() {
        auth.requireOwner(request, 4L);
        denied(HttpStatus.FORBIDDEN, () -> auth.requireOwner(request, 5L));
    }

    @Test
    void loginRotatesSessionAndLogoutInvalidatesIt() {
        HttpSession replacement = mock(HttpSession.class);
        when(request.getSession(true)).thenReturn(replacement);
        auth.signIn(request, 4L);
        verify(session).invalidate();
        verify(replacement).setAttribute("OBSCURA_AUTH_MEMBER_NO", 4L);
        verify(replacement).setMaxInactiveInterval(1800);
        when(request.getSession(false)).thenReturn(replacement);
        auth.signOut(request);
        verify(replacement).invalidate();
    }
}
