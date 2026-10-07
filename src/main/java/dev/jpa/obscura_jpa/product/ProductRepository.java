package dev.jpa.obscura_jpa.product;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// JpaRepository: 기존 저장·조회 기능
// JpaSpecificationExecutor: 카테고리·할인 조건을 조합하고 페이지 단위로 조회하는 기능
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // 전체 상품 최신순
    List<Product> findAllByOrderByNoDesc();

    // 판매중 상품
    List<Product> findAllByStatusNoOrderByNoDesc(Integer statusNo);

    // 특정 브랜드 상품
    List<Product> findAllByBrandNoOrderByNoDesc(Long bno);

    // 특정 카테고리 상품
    List<Product> findAllByCategoryNoOrderByNoDesc(Long cno);

    // 브랜드 + 카테고리 상품
    List<Product> findAllByBrandNoAndCategoryNoOrderByNoDesc(Long bno, Long cno);

    // 상품명 검색
    List<Product> findByNameContainingIgnoreCaseOrderByNoDesc(String name);

    // 특정 브랜드의 전체 상품 수
    long countByBrandNo(Long bno);

    // 특정 브랜드의 할인 상품 수
    long countByBrandNoAndDiscountRateGreaterThan(Long bno, Integer discountRate);

    // 상품 등록 시 같은 CODE가 이미 있는지 확인합니다.
    boolean existsByCodeIgnoreCase(String code);

    // 상품 수정 시 현재 상품을 제외하고 같은 CODE가 있는지 확인합니다.
    boolean existsByCodeIgnoreCaseAndNoNot(String code, Long no);
    
 // 결제된 주문의 취소 제외 수량을 상품별로 합산합니다.
 // 판매량이 없는 상품도 포함하여 인기순 목록의 페이징을 유지합니다.
 @Query(
     value = """
         SELECT P.*
         FROM PRODUCT P
         JOIN BRAND B ON B.NO = P.BNO
         LEFT JOIN (
             SELECT PO.PNO,
                    SUM(GREATEST(OI.QTY - NVL(OI.CANCELQTY, 0), 0)) AS SOLDQTY
             FROM ORDERITEM OI
             JOIN PRODUCTOPTION PO ON PO.NO = OI.PONO
             JOIN ORDERS O ON O.NO = OI.ORDNO
             JOIN PAYMENT PM ON PM.ORDNO = O.NO
             WHERE O.STATUSNO IN (2, 3, 4, 5)
               AND PM.STATUSNO IN (1, 2)
             GROUP BY PO.PNO
         ) SALES ON SALES.PNO = P.NO
         WHERE P.STATUSNO = 1
           AND (:cno IS NULL OR P.CNO = :cno)
           AND (:saleOnly = 0 OR P.DISCOUNTRATE > 0)
           AND (
               :keyword IS NULL
               OR INSTR(LOWER(P.NAME), :keyword) > 0
               OR INSTR(LOWER(B.NAME), :keyword) > 0
               OR INSTR(LOWER(P.CODE), :keyword) > 0
           )
         ORDER BY NVL(SALES.SOLDQTY, 0) DESC, P.CDATE DESC, P.NO DESC
         """,
     countQuery = """
         SELECT COUNT(*)
         FROM PRODUCT P
         JOIN BRAND B ON B.NO = P.BNO
         WHERE P.STATUSNO = 1
           AND (:cno IS NULL OR P.CNO = :cno)
           AND (:saleOnly = 0 OR P.DISCOUNTRATE > 0)
           AND (
               :keyword IS NULL
               OR INSTR(LOWER(P.NAME), :keyword) > 0
               OR INSTR(LOWER(B.NAME), :keyword) > 0
               OR INSTR(LOWER(P.CODE), :keyword) > 0
           )
         """,
     nativeQuery = true
 )
 Page<Product> findPopularPage(
     @Param("cno") Long cno,
     @Param("saleOnly") int saleOnly,
     @Param("keyword") String keyword,
     Pageable pageable
 );
 
//판매 중인 MD 추천 상품만 표시 순서대로 조회합니다.
//순서가 같으면 최신 상품을 먼저 표시합니다.
Page<Product> findByStatusNoAndMdPickYnOrderByMdSeqNoAscCdateDescNoDesc(
  Integer statusNo,
  String mdPickYn,
  Pageable pageable
);

}