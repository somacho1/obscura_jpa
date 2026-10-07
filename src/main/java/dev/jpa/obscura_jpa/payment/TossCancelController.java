package dev.jpa.obscura_jpa.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/toss")
@CrossOrigin(origins = "*")
public class TossCancelController {

    private final TossCancelService tossCancelService;

    public TossCancelController(TossCancelService tossCancelService) {
        this.tossCancelService = tossCancelService;
    }

    // POST /api/payments/toss/cancel
    @PostMapping("/cancel")
    public ResponseEntity<?> cancel(@RequestBody TossCancelRequest request) {
        try {
            return ResponseEntity.ok(tossCancelService.cancel(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}