package dev.jpa.obscura_jpa.fashionai;

import dev.jpa.obscura_jpa.auth.RequireRole;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/fashion-ai/products/{pno}/tags")
@RequireRole
public class FashionTagController {
    private final FashionTagService service;
    public FashionTagController(FashionTagService service) { this.service = service; }
    @GetMapping
    public ResponseEntity<?> list(@PathVariable("pno") Long pno) {
        try { return ResponseEntity.ok(service.list(pno)); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("message",e.getMessage())); }
    }
    // 선택 태그 전체를 하나의 트랜잭션으로 교체합니다. 빈 배열은 이 화면의 태그 전체 삭제입니다.
    @PutMapping
    public ResponseEntity<?> save(@PathVariable("pno") Long pno, @RequestBody List<FashionTagService.TagInput> tags) {
        try { return ResponseEntity.ok(service.replace(pno,tags)); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("message",e.getMessage())); }
    }
    @GetMapping("/status")
    public ResponseEntity<?> status(@PathVariable("pno") Long pno) {
        try { return ResponseEntity.ok(Map.of("count", service.countAll(pno))); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }
    @PostMapping("/if-empty")
    public ResponseEntity<?> saveIfEmpty(@PathVariable("pno") Long pno, @RequestBody List<FashionTagService.TagInput> tags) {
        try { return ResponseEntity.ok(service.saveIfEmpty(pno, tags)); }
        catch (IllegalStateException e) { return ResponseEntity.status(409).body(Map.of("message", e.getMessage())); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

}
