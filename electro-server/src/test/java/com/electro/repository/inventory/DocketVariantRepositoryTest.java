package com.electro.repository.inventory;

import com.electro.entity.inventory.Docket;
import com.electro.entity.inventory.DocketVariant;
import com.electro.entity.inventory.DocketVariantKey;
import com.electro.entity.product.Product;
import com.electro.entity.product.Variant;
import com.electro.repository.product.ProductRepository;
import com.electro.repository.product.VariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DocketVariantRepositoryTest {

    @Autowired
    private DocketVariantRepository docketVariantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VariantRepository variantRepository;

    @Autowired
    private DocketRepository docketRepository;

    private Product product;
    private Variant variant1;
    private Variant variant2;
    private Docket docket1;
    private Docket docket2;
    private DocketVariant docketVariant1;
    private DocketVariant docketVariant2;
    private DocketVariant docketVariant3;

    @BeforeEach
    void setUp() {
        // Tạo product
        product = new Product();
        product.setName("Test Product");
        product = productRepository.save(product);

        // Tạo variants
        variant1 = new Variant();
        variant1.setSku("VARIANT-1");
        variant1.setProduct(product);
        variant1 = variantRepository.save(variant1);

        variant2 = new Variant();
        variant2.setSku("VARIANT-2");
        variant2.setProduct(product);
        variant2 = variantRepository.save(variant2);

        // Tạo dockets
        docket1 = new Docket();
        docket1.setCode("DOCKET-1");
        docket1 = docketRepository.save(docket1);

        docket2 = new Docket();
        docket2.setCode("DOCKET-2");
        docket2 = docketRepository.save(docket2);

        // Tạo docket variants
        docketVariant1 = new DocketVariant();
        docketVariant1.setDocketVariantKey(new DocketVariantKey(docket1.getId(), variant1.getId()));
        docketVariant1.setDocket(docket1);
        docketVariant1.setVariant(variant1);
        docketVariant1.setQuantity(10);
        docketVariant1 = docketVariantRepository.save(docketVariant1);

        docketVariant2 = new DocketVariant();
        docketVariant2.setDocketVariantKey(new DocketVariantKey(docket2.getId(), variant1.getId()));
        docketVariant2.setDocket(docket2);
        docketVariant2.setVariant(variant1);
        docketVariant2.setQuantity(20);
        docketVariant2 = docketVariantRepository.save(docketVariant2);

        docketVariant3 = new DocketVariant();
        docketVariant3.setDocketVariantKey(new DocketVariantKey(docket1.getId(), variant2.getId()));
        docketVariant3.setDocket(docket1);
        docketVariant3.setVariant(variant2);
        docketVariant3.setQuantity(30);
        docketVariant3 = docketVariantRepository.save(docketVariant3);
    }

    /**
     * Test ID: DV-REP-001
     * Test case: Tìm docket variant theo ID
     * Mục tiêu: Kiểm tra repository trả về đúng docket variant
     */
    @Test
    void findById_ShouldReturnDocketVariant() {
        // Act
        Optional<DocketVariant> result = docketVariantRepository.findById(docketVariant1.getDocketVariantKey());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(docketVariant1.getDocketVariantKey(), result.get().getDocketVariantKey());
        assertEquals(docketVariant1.getQuantity(), result.get().getQuantity());
    }

    /**
     * Test ID: DV-REP-002
     * Test case: Tìm docket variant theo ID không tồn tại
     * Mục tiêu: Kiểm tra repository trả về Optional empty
     */
    @Test
    void findById_WithNonExistentId_ShouldReturnEmpty() {
        // Act
        Optional<DocketVariant> result = docketVariantRepository.findById(
            new DocketVariantKey(999L, 999L));

        // Assert
        assertTrue(result.isEmpty());
    }

    /**
     * Test ID: DV-REP-003
     * Test case: Lưu docket variant mới
     * Mục tiêu: Kiểm tra repository lưu thành công docket variant mới
     */
    @Test
    void save_ShouldSaveNewDocketVariant() {
        // Arrange
        DocketVariant newDocketVariant = new DocketVariant();
        newDocketVariant.setDocketVariantKey(new DocketVariantKey(docket2.getId(), variant2.getId()));
        newDocketVariant.setDocket(docket2);
        newDocketVariant.setVariant(variant2);
        newDocketVariant.setQuantity(40);

        // Act
        DocketVariant saved = docketVariantRepository.save(newDocketVariant);

        // Assert
        assertNotNull(saved);
        assertEquals(newDocketVariant.getDocketVariantKey(), saved.getDocketVariantKey());
        assertEquals(newDocketVariant.getQuantity(), saved.getQuantity());
    }

    /**
     * Test ID: DV-REP-004
     * Test case: Cập nhật docket variant
     * Mục tiêu: Kiểm tra repository cập nhật thành công docket variant
     */
    @Test
    void save_ShouldUpdateExistingDocketVariant() {
        // Arrange
        docketVariant1.setQuantity(50);

        // Act
        DocketVariant updated = docketVariantRepository.save(docketVariant1);

        // Assert
        assertNotNull(updated);
        assertEquals(docketVariant1.getDocketVariantKey(), updated.getDocketVariantKey());
        assertEquals(50, updated.getQuantity());
    }

    /**
     * Test ID: DV-REP-005
     * Test case: Xóa docket variant
     * Mục tiêu: Kiểm tra repository xóa thành công docket variant
     */
    @Test
    void delete_ShouldDeleteDocketVariant() {
        // Act
        docketVariantRepository.delete(docketVariant1);

        // Assert
        assertTrue(docketVariantRepository.findById(docketVariant1.getDocketVariantKey()).isEmpty());
    }

    /**
     * Test ID: DV-REP-006
     * Test case: Tìm tất cả docket variants với phân trang
     * Mục tiêu: Kiểm tra repository trả về đúng danh sách docket variants có phân trang
     */
    @Test
    void findAll_WithPagination_ShouldReturnPagedDocketVariants() {
        // Act
        Page<DocketVariant> result = docketVariantRepository.findAll(PageRequest.of(0, 2));

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
    }

    /**
     * Test ID: DV-REP-007
     * Test case: Tìm docket variants với specification
     * Mục tiêu: Kiểm tra repository trả về đúng danh sách docket variants theo điều kiện
     */
    @Test
    void findAll_WithSpecification_ShouldReturnFilteredDocketVariants() {
        // Arrange
        Specification<DocketVariant> spec = (Root<DocketVariant> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> 
            cb.equal(root.get("quantity"), 10);

        // Act
        List<DocketVariant> result = docketVariantRepository.findAll(spec);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getQuantity());
    }

    /**
     * Test ID: DV-REP-008
     * Test case: Kiểm tra tồn tại docket variant
     * Mục tiêu: Kiểm tra repository trả về đúng kết quả kiểm tra tồn tại
     */
    @Test
    void existsById_ShouldReturnCorrectResult() {
        // Act & Assert
        assertTrue(docketVariantRepository.existsById(docketVariant1.getDocketVariantKey()));
        assertFalse(docketVariantRepository.existsById(new DocketVariantKey(999L, 999L)));
    }

    /**
     * Test ID: DV-REP-009
     * Test case: Đếm số lượng docket variants
     * Mục tiêu: Kiểm tra repository trả về đúng số lượng docket variants
     */
    @Test
    void count_ShouldReturnCorrectCount() {
        // Act
        long count = docketVariantRepository.count();

        // Assert
        assertEquals(3, count);
    }

    /**
     * Test ID: DV-REP-010
     * Test case: Xóa docket variant theo ID
     * Mục tiêu: Kiểm tra repository xóa thành công docket variant theo ID
     */
    @Test
    void deleteById_ShouldDeleteDocketVariant() {
        // Act
        docketVariantRepository.deleteById(docketVariant1.getDocketVariantKey());

        // Assert
        assertTrue(docketVariantRepository.findById(docketVariant1.getDocketVariantKey()).isEmpty());
    }
} 