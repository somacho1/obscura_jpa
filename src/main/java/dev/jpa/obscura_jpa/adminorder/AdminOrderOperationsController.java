package dev.jpa.obscura_jpa.adminorder;

import java.util.*;
import dev.jpa.obscura_jpa.auth.RequireRole;
import dev.jpa.obscura_jpa.auth.SessionAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequireRole
@RequestMapping("/api/admin/order-operations")
public class AdminOrderOperationsController {
    private final AdminOrderOperationsService service;
    private final SessionAuthService auth;
    public AdminOrderOperationsController(AdminOrderOperationsService service,SessionAuthService auth) {this.service=service;this.auth=auth;}
    @GetMapping("/summary") public AdminOrderOperationsService.Summary summary() {return service.summary();}
    @GetMapping("/notices") public AdminOrderOperationsService.PageResult notices(
        @RequestParam(name="unread",defaultValue="false") boolean unread,@RequestParam(name="page",defaultValue="1") int page,
        @RequestParam(name="size",defaultValue="20") int size) {return service.notices(unread,page,size);}
    @GetMapping("/orders") public AdminOrderOperationsService.PageResult orders(
        @RequestParam(name="filter",defaultValue="ALL") String filter,@RequestParam(name="unread",defaultValue="false") boolean unread,
        @RequestParam(name="page",defaultValue="1") int page,@RequestParam(name="size",defaultValue="20") int size) {return service.orders(filter,unread,page,size);}
    @GetMapping("/orders/{no}") public AdminOrderOperationsService.DetailResult detail(@PathVariable("no") Long no,
        @RequestParam(name="page",defaultValue="1") int page,@RequestParam(name="size",defaultValue="20") int size,@RequestParam(name="notice",required=false) Long noticeNo) {return service.detail(no,page,size,noticeNo);}
    public record ReadRequest(List<Long> ids) {}
    @PostMapping("/orders/{no}/read") public Map<String,Integer> read(@PathVariable("no") Long no,@RequestBody ReadRequest body,HttpServletRequest request) {
        var admin=auth.requireRole(request,"ADMIN");
        return Map.of("changed",service.read(no,body.ids(),admin.getNo()));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public Map<String,String> badRequest(IllegalArgumentException error) {return Map.of("message",error.getMessage());}
}
