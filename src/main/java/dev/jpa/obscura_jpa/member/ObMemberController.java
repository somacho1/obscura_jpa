package dev.jpa.obscura_jpa.member;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * 회원 Controller
 *
 * 회원가입, 조회, 수정, 탈퇴,
 * 아이디/이메일 중복검사 API를 제공한다.
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ObMemberController {

    private final ObMemberService obMemberService;

    /**
     * 회원가입
     *
     * POST /api/members
     */
    @PostMapping
    public ResponseEntity<?> createMember(
        @RequestBody ObMemberDTO dto
    ) {

        try {

            ObMemberDTO savedMember =
                obMemberService.createMember(dto);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedMember);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
        }
    }
    
    /**
     * 로그인
     *
     * POST /api/members/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
        @RequestBody LoginRequestDTO dto
    ) {

        try {

            LoginResponseDTO member =
                obMemberService.login(dto);

            return ResponseEntity.ok(
                member
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
        }
    }

    /**
     * 전체 회원 조회
     *
     * GET /api/members
     */
    @GetMapping
    public ResponseEntity<List<ObMemberDTO>> findAll() {

        return ResponseEntity.ok(
            obMemberService.findAll()
        );
    }

    /**
     * 회원번호로 회원 조회
     *
     * GET /api/members/1
     */
    @GetMapping("/{no}")
    public ResponseEntity<?> findByNo(
        @PathVariable("no") Long no
    ) {

        Optional<ObMemberDTO> member =
            obMemberService.findByNo(no);

        if (member.isEmpty()) {

            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("회원 정보를 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(member.get());
    }

    /**
     * 로그인 아이디로 회원 조회
     *
     * GET /api/members/search?id=user01
     */
    @GetMapping("/search")
    public ResponseEntity<?> findById(
        @RequestParam("id") String id
    ) {

        Optional<ObMemberDTO> member =
            obMemberService.findById(id);

        if (member.isEmpty()) {

            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("회원 정보를 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(member.get());
    }

    /**
     * 회원정보 수정
     *
     * PUT /api/members/1
     */
    @PutMapping("/{no}")
    public ResponseEntity<?> updateMember(
        @PathVariable("no") Long no,
        @RequestBody ObMemberDTO dto
    ) {

        try {

            ObMemberDTO updatedMember =
                obMemberService.updateMember(no, dto);

            return ResponseEntity.ok(updatedMember);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
        }
    }

    /**
     * 회원탈퇴
     *
     * DELETE /api/members/1
     *
     * 실제 DB 행은 삭제하지 않고
     * STATUSNO를 0으로 변경한다.
     */
    @DeleteMapping("/{no}")
    public ResponseEntity<?> withdrawMember(
        @PathVariable("no") Long no
    ) {

        try {

            obMemberService.withdrawMember(no);

            return ResponseEntity
                .noContent()
                .build();

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
        }
    }

    /**
     * 아이디 중복 검사
     *
     * GET /api/members/check-id?id=user01
     */
    @GetMapping("/check-id")
    public ResponseEntity<Boolean> checkId(
        @RequestParam("id") String id
    ) {

        return ResponseEntity.ok(
            obMemberService.existsById(id)
        );
    }

    /**
     * 이메일 중복 검사
     *
     * GET /api/members/check-email?email=user@test.com
     */
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(
        @RequestParam("email") String email
    ) {

        return ResponseEntity.ok(
            obMemberService.existsByEmail(email)
        );
    }
}