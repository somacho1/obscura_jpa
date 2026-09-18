package dev.jpa.obscura_jpa.productoption;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOptionRepository
    extends JpaRepository<ProductOption, Long> {

    // 특정 상품의 전체 옵션
    List<ProductOption> findAllByProductNoOrderByNoAsc(Long pno);

    // 특정 상품의 현재 사용 가능한 옵션
    List<ProductOption> findAllByProductNoAndUseYnOrderByNoAsc(
        Long pno,
        String useYn
    );

    // 같은 상품에 같은 색상 + 사이즈 조합이 존재하는지 확인
    boolean existsByProductNoAndColorAndSizeValue(
        Long pno,
        String color,
        String sizeValue
    );

    // 수정할 때 자기 자신(NO)은 제외하고 중복 확인
    boolean existsByProductNoAndColorAndSizeValueAndNoNot(
        Long pno,
        String color,
        String sizeValue,
        Long no
    );
}