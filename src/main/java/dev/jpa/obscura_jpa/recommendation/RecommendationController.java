package dev.jpa.obscura_jpa.recommendation;

import dev.jpa.obscura_jpa.auth.SessionAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService service;
    private final SessionAuthService auth;
    public RecommendationController(RecommendationService service, SessionAuthService auth) { this.service = service; this.auth = auth; }
    public record Response(Long ownerNo, String mode, java.util.List<RecommendationRanker.Pick> products, java.util.List<RecommendationRanker.Brand> brands) {}

    @GetMapping("/for-you")
    public ResponseEntity<Response> forYou(HttpServletRequest request, @RequestParam(name="size", defaultValue="7") int size) {
        // 회원번호를 쿼리 파라미터로 받지 않고 서버 세션으로만 본인 이력을 결정합니다.
        HttpSession session = request.getSession(false);
        Long ownerNo = session != null && session.getAttribute("OBSCURA_AUTH_MEMBER_NO") != null
            ? auth.requireMember(request).getNo() : null;
        RecommendationRanker.Result result = service.recommend(ownerNo, Math.max(1, Math.min(30, size)));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).varyBy("Cookie")
            .body(new Response(ownerNo, result.mode(), result.products(), result.brands()));
    }
}
