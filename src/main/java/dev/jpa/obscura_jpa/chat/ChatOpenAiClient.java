package dev.jpa.obscura_jpa.chat;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatOpenAiClient {
    private final RestClient client;
    private final String key;
    private final String model;
    public ChatOpenAiClient(@Value("${openai.api-key:${OPENAI_API_KEY:}}") String key,
                       @Value("${openai.model:gpt-4o-mini}") String model) {
        this.key = key; this.model = model;
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(40));
        this.client = RestClient.builder().baseUrl("https://api.openai.com/v1")
            .requestFactory(factory).build();
    }

    @SuppressWarnings("unchecked")
    public String generate(String instructions, List<Map<String, String>> input, Map<String, Object> format) {
        if (key == null || key.isBlank()) throw failure("서버에 OPENAI_API_KEY가 설정되지 않았습니다.");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model); payload.put("instructions", instructions); payload.put("input", input);
        payload.put("store", false); payload.put("max_output_tokens", 1800);
        if (format != null) payload.put("text", Map.of("format", format));
        try {
            // Responses API: 모델에 종속적인 temperature 옵션을 넣지 않습니다.
            Map<String, Object> response = client.post().uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve().body(Map.class);
            if (response == null || !"completed".equals(response.get("status"))) throw failure("답변을 완성하지 못했습니다. 잠시 후 다시 시도해 주세요.");
            // REST 응답의 output은 추론 항목 등을 포함할 수 있어 message/output_text만 읽습니다.
            StringBuilder answer = new StringBuilder();
            if (response.get("output") instanceof List<?> output) {
                for (Object item : output) {
                    if (!(item instanceof Map<?, ?> msg) || !"message".equals(msg.get("type"))) continue;
                    if (!(msg.get("content") instanceof List<?> content)) continue;
                    for (Object part : content) {
                        if (part instanceof Map<?, ?> block && "output_text".equals(block.get("type")) && block.get("text") instanceof String text) answer.append(text);
                    }
                }
            }
            if (answer.isEmpty()) throw failure("답변을 받지 못했습니다. 다시 시도해 주세요.");
            return answer.toString();
        } catch (RestClientResponseException e) {
            // 인증키나 OpenAI의 원본 오류 본문을 화면·로그에 노출하지 않습니다.
            if (e.getStatusCode().value() == 401) throw failure("AI 연결 인증을 확인해 주세요. 서버 API 키 설정 확인이 필요합니다.");
            if (e.getStatusCode().value() == 429) throw failure("AI 사용 한도 또는 요청 제한에 도달했습니다. 잠시 후 다시 시도해 주세요.");
            throw failure("AI 연결에 실패했습니다. 서버의 모델 설정과 API 사용 가능 여부를 확인해 주세요.");
        } catch (RestClientException e) {
            throw failure("AI 응답 시간이 초과되었거나 연결이 원활하지 않습니다. 다시 시도해 주세요.");
        }
    }
    private static ResponseStatusException failure(String message) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
