package dev.jpa.obscura_jpa.product;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 이력이 없는 테스트 상품만 영구 삭제합니다.
 * 연관 데이터와 상품 삭제를 한 트랜잭션으로 처리해 중간 실패 시 DB를 롤백합니다.
 */
@Service
public class ProductPermanentDeleteService {
    private final EntityManager entityManager;
    private final ProductRepository productRepository;

    public ProductPermanentDeleteService(EntityManager entityManager, ProductRepository productRepository) {
        this.entityManager = entityManager;
        this.productRepository = productRepository;
    }

    @Transactional
    public void delete(Long productNo) {
        if (!productRepository.existsById(productNo)) {
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }

        // ORDERITEM은 PRODUCTOPTION을 참조합니다. 주문 이력이 있으면 상품과 옵션을 유지해야 합니다.
        Number orderCount = (Number) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM ORDERITEM OI JOIN PRODUCTOPTION PO ON OI.PONO = PO.NO WHERE PO.PNO = :pno"
        ).setParameter("pno", productNo).getSingleResult();
        if (orderCount.longValue() > 0) {
            throw new IllegalStateException("주문 이력이 있는 상품은 영구 삭제할 수 없습니다. 판매중지를 이용해주세요.");
        }

        // 자식 행부터 삭제합니다. CARTITEM과 STOCK은 옵션을, 나머지는 상품을 참조합니다.
        execute("DELETE FROM CARTITEM WHERE PONO IN (SELECT NO FROM PRODUCTOPTION WHERE PNO = :pno)", productNo);
        execute("DELETE FROM STOCK WHERE PONO IN (SELECT NO FROM PRODUCTOPTION WHERE PNO = :pno)", productNo);
        execute("DELETE FROM WISHLIST WHERE PNO = :pno", productNo);
        execute("DELETE FROM PRODUCTTAG WHERE PNO = :pno", productNo);
        execute("DELETE FROM PRODUCTIMAGE WHERE PNO = :pno", productNo);
        execute("DELETE FROM PRODUCTOPTION WHERE PNO = :pno", productNo);
        execute("DELETE FROM PRODUCT WHERE NO = :pno", productNo);
    }

    private void execute(String sql, Long productNo) {
        entityManager.createNativeQuery(sql).setParameter("pno", productNo).executeUpdate();
    }
}
