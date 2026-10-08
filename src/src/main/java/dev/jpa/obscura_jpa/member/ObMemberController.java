package dev.jpa.obscura_jpa.member;

import java.util.List;
import dev.jpa.obscura_jpa.auth.RequireRole;
import dev.jpa.obscura_jpa.auth.SessionAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class ObMemberController {
    private final ObMemberService obMemberService;
    private final SessionAuthService auth;

    // Service는 요청 ROLE과 무관하게 신규 계정을 USER로 저장합니다.
    @PostMapping
    public ResponseEntity<?> createMember(@RequestBody ObMemberDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(obMemberService.createMember(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO dto, HttpServletRequest request) {
        try {
            LoginResponseDTO member = obMemberService.login(dto);
            auth.signIn(request, member.getNo());
            return ResponseEntity.ok(member);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 새로고침 시 localStorage 대신 서버 세션으로 로그인 상태를 복원합니다.
    @GetMapping("/me")
    public LoginResponseDTO me(HttpServletRequest request) {
        return auth.current(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        auth.signOut(request);
        return ResponseEntity.noContent().build();
    }

    @RequireRole("SUPER_ADMIN")
    @GetMapping
    public List<ObMemberDTO> findAll() {
        return obMemberService.findAll();
    }

    @GetMapping("/{no}")
    public ObMemberDTO findByNo(@PathVariable("no") Long no, HttpServletRequest request) {
        auth.requireOwner(request, no);
        return obMemberService.findByNo(no).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
    }

    @RequireRole("SUPER_ADMIN")
    @GetMapping("/search")
    public ObMemberDTO findById(@RequestParam("id") String id) {
        return obMemberService.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
    }

    @PutMapping("/{no}")
    public ResponseEntity<?> updateMember(@PathVariable("no") Long no, @RequestBody ObMemberDTO dto,
                                         HttpServletRequest request) {
        auth.requireOwner(request, no);
        try {
            return ResponseEntity.ok(obMemberService.updateMember(no, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{no}")
    public ResponseEntity<?> withdrawMember(@PathVariable("no") Long no, HttpServletRequest request) {
        auth.requireOwner(request, no);
        if ("SUPER_ADMIN".equals(auth.requireMember(request).getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최상위 관리자는 여기서 탈퇴할 수 없습니다.");
        }
        try {
            obMemberService.withdrawMember(no);
            auth.signOut(request);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/check-id")
    public boolean checkId(@RequestParam("id") String id) {
        return obMemberService.existsById(id);
    }

    @GetMapping("/check-email")
    public boolean checkEmail(@RequestParam("email") String email) {
        return obMemberService.existsByEmail(email);
    }
}
