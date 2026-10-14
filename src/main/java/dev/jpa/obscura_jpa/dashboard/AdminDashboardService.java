package dev.jpa.obscura_jpa.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import dev.jpa.obscura_jpa.order.OrderDTO;
import dev.jpa.obscura_jpa.order.OrderService;
import dev.jpa.obscura_jpa.product.ProductDTO;
import dev.jpa.obscura_jpa.product.ProductService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
    private final EntityManager entities;
    private final OrderService orders;
    private final ProductService products;
    public AdminDashboardService(EntityManager entities, OrderService orders, ProductService products) {
        this.entities = entities; this.orders = orders; this.products = products;
    }
    public record DashboardResponse(long brandCount, long productCount, long saleProductCount,
        long todayOrderCount, long totalOrderCount, List<OrderDTO> recentOrders,
        List<ProductDTO> recentProducts, LocalDateTime updatedAt) {}

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        // 오늘 주문은 한국 시간 자정~다음 자정의 생성 건수입니다(취소·결제대기 포함).
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        long todayCount = ((Number) entities.createNativeQuery(
            "SELECT COUNT(*) FROM ORDERS WHERE CDATE >= :startDate AND CDATE < :endDate")
            .setParameter("startDate", today.atStartOfDay())
            .setParameter("endDate", today.plusDays(1).atStartOfDay()).getSingleResult()).longValue();
        // 전체 등록 건수는 판매중지 포함, 할인 상품은 현재 판매중인 할인 상품만 집계합니다.
        long brands = count("SELECT COUNT(*) FROM BRAND");
        long productCount = count("SELECT COUNT(*) FROM PRODUCT");
        long saleCount = count("SELECT COUNT(*) FROM PRODUCT WHERE STATUSNO = 1 AND DISCOUNTRATE > 0");
        var orderPage = orders.findAdminOrderPage(1, 5);
        // 최신 다섯 건의 번호만 읽은 뒤 기존 DTO 변환을 재사용합니다. 전체 상품 조회는 하지 않습니다.
        List<?> productNos = entities.createNativeQuery("SELECT NO FROM PRODUCT ORDER BY CDATE DESC, NO DESC")
            .setMaxResults(5).getResultList();
        List<ProductDTO> recentProducts = productNos.stream()
            .map(no -> products.findByNo(((Number) no).longValue())).toList();
        return new DashboardResponse(brands, productCount, saleCount, todayCount,
            orderPage.getTotalElements(), orderPage.getContent(), recentProducts,
            LocalDateTime.now(ZoneId.of("Asia/Seoul")));
    }
    private long count(String sql) {
        return ((Number) entities.createNativeQuery(sql).getSingleResult()).longValue();
    }
}
