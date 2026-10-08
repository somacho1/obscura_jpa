package dev.jpa.obscura_jpa.auth;

import java.util.List;
import dev.jpa.obscura_jpa.member.ObMemberDTO;
import dev.jpa.obscura_jpa.member.ObMemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/members")
@RequireRole("SUPER_ADMIN")
@RequiredArgsConstructor
public class AdminMemberController {
    private final ObMemberService members;
    private final AdminMemberService admin;

    public record RoleRequest(String role) {}

    // 회원 DTO에는 비밀번호가 포함되지 않습니다.
    @GetMapping
    public List<ObMemberDTO> findAll() {
        return members.findAll();
    }

    @PutMapping("/{no}/role")
    public ObMemberDTO changeRole(@PathVariable("no") Long no, @RequestBody RoleRequest body,
                                 HttpServletRequest request) {
        return admin.changeRole(request, no, body.role());
    }
}
