package dev.jpa.obscura_jpa.chat;

import java.util.List;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService service;
    public ChatController(ChatService service) { this.service = service; }

    // 클라이언트가 system 역할이나 과도하게 긴 대화를 전송하지 못하도록 제한합니다.
    public record Message(@NotNull @Pattern(regexp = "user|assistant") String role,
                          @NotBlank @Size(max = 2000) String content) {}
    public record Request(@NotBlank @Size(max = 500) String question,
                          @NotNull @Size(max = 10) List<@NotNull @Valid Message> history) {}
    public record Answer(String answer, String mode, List<String> sources,
                         List<ChatProductService.Item> products, String conditions) {}

    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<?> handle(ResponseStatusException error) {
        return org.springframework.http.ResponseEntity.status(error.getStatusCode())
            .body(java.util.Map.of("message", error.getReason() == null ? "상담 요청을 처리하지 못했습니다." : error.getReason()));
    }

    @PostMapping
    public Answer chat(@Valid @RequestBody Request request, HttpSession session) {
        // 로컬 포트폴리오용 세션 단위 제한입니다. API 키나 외부 오류 본문은 반환하지 않습니다.
        synchronized (session) {
            long now = System.currentTimeMillis();
            Long start = (Long) session.getAttribute("chatWindow");
            Integer count = (Integer) session.getAttribute("chatCount");
            if (start == null || now - start >= 60000) {
                session.setAttribute("chatWindow", now); count = 0;
            }
            if (count != null && count >= 12) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 질문해 주세요.");
            }
            session.setAttribute("chatCount", count == null ? 1 : count + 1);
        }
        return service.answer(request);
    }
}
