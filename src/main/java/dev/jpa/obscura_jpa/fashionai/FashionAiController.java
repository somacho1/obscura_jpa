package dev.jpa.obscura_jpa.fashionai;

import dev.jpa.obscura_jpa.auth.RequireRole;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** React → 관리자 권한 검사 → 로컬 Python 모델 서버. 이미지 URL을 외부에서 받아 조회하지 않습니다. */
@RestController
@RequestMapping("/api/admin/fashion-ai")
@RequireRole
public class FashionAiController {
    private final URI endpoint;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
        .version(HttpClient.Version.HTTP_1_1).build();

    public FashionAiController(@Value("${obscura.ai.base-url:http://127.0.0.1:8000}") String baseUrl) {
        endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/api/ai/analyze");
    }

    @GetMapping("/taxonomy")
    public ResponseEntity<?> taxonomy() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint.toString().replace("/analyze", "/taxonomy")))
                .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) return error(502, "Python 태그 분류 목록을 불러오지 못했습니다.");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); return error(503, "요청이 중단되었습니다.");
        } catch (IOException e) { return error(503, "Python AI 서버를 먼저 실행해주세요."); }
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyze(@RequestParam(value="file", required=false) MultipartFile file,
                                    @RequestParam(value="files", required=false) List<MultipartFile> files,
                                    @RequestParam(value="category", defaultValue="AUTO") String category) {
        if (!Set.of("AUTO", "TOP", "BOTTOM", "DRESS", "OUTER", "SHOES", "BAG", "ACCESSORY").contains(category))
            return error(400, "분석 분류를 확인해주세요.");
        List<MultipartFile> images = new ArrayList<>();
        if (file != null) images.add(file);
        if (files != null) images.addAll(files);
        if (images.isEmpty() || images.size() > 3) return error(400, "같은 상품의 사진을 1~3장 선택해주세요.");
        for (MultipartFile image : images) {
            if (image.isEmpty()) return error(400, "비어 있는 이미지입니다.");
            if (image.getSize() > 10 * 1024 * 1024) return error(413, "이미지는 한 장당 최대 10MB까지 가능합니다.");
        }
        String boundary = "Obscura" + UUID.randomUUID().toString().replace("-", "");
        List<byte[]> parts = new ArrayList<>();
        parts.add(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"category\"\r\n\r\n" + category + "\r\n").getBytes(StandardCharsets.UTF_8));
        try {
            for (MultipartFile image : images) {
                // 실제 이미지 형식은 Python에서 파일 바이트를 읽어 검사합니다.
                parts.add(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"files\"; filename=\"image\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                parts.add(image.getBytes()); parts.add("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            parts.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(300))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArrays(parts)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200 || Set.of(400, 413, 415, 422).contains(response.statusCode()))
                return ResponseEntity.status(response.statusCode()).contentType(MediaType.APPLICATION_JSON).body(response.body());
            return error(502, "AI 분석 오류입니다. Python 서버 로그를 확인해주세요.");
        } catch (HttpTimeoutException e) { return error(504, "AI 분석 시간이 초과되었습니다. 잠시 후 다시 시도해주세요."); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return error(503, "AI 요청이 중단되었습니다."); }
        catch (IOException e) { return error(503, "AI 서버에 연결할 수 없습니다. Python 서버를 확인해주세요."); }
    }
    private ResponseEntity<?> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
