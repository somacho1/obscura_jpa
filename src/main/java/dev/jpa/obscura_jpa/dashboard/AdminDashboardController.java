package dev.jpa.obscura_jpa.dashboard;

import dev.jpa.obscura_jpa.auth.RequireRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 기존 세션·DB 권한 검사에 연결되는 관리자 전용 읽기 API입니다.
@RestController
@RequireRole
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService service;
    public AdminDashboardController(AdminDashboardService service) { this.service = service; }
    @GetMapping
    public AdminDashboardService.DashboardResponse getDashboard() { return service.getDashboard(); }
}
