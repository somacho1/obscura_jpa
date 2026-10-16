package dev.jpa.obscura_jpa.brand;
import java.util.Optional;
import dev.jpa.obscura_jpa.product.ProductRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BrandDeletionTest {
    @Test void brandWithProductsCannotBeDeleted() {
        var brands = mock(BrandRepository.class); var products = mock(ProductRepository.class);
        var brand = new Brand(); when(brands.findById(1L)).thenReturn(Optional.of(brand));
        when(products.countByBrandNo(1L)).thenReturn(1L);
        assertThrows(IllegalStateException.class, () -> new BrandService(brands, products).deleteEmptyBrand(1L));
        verify(brands, never()).delete(any(Brand.class)); verify(brands, never()).flush();
    }
    @Test void emptyBrandIsDeletedAndFlushed() {
        var brands = mock(BrandRepository.class); var products = mock(ProductRepository.class);
        var brand = new Brand(); when(brands.findById(1L)).thenReturn(Optional.of(brand));
        when(products.countByBrandNo(1L)).thenReturn(0L);
        new BrandService(brands, products).deleteEmptyBrand(1L);
        verify(brands).delete(brand); verify(brands).flush();
    }
    @Test void missingBrandIsRejected() {
        var brands = mock(BrandRepository.class); var products = mock(ProductRepository.class);
        when(brands.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> new BrandService(brands, products).deleteEmptyBrand(1L));
        verifyNoInteractions(products); verify(brands, never()).delete(any(Brand.class));
    }
}
