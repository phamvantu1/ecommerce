package com.electro.service.inventory;

import com.electro.constant.ResourceName;
import com.electro.constant.SearchFields;
import com.electro.dto.ListResponse;
import com.electro.dto.inventory.CountVariantRequest;
import com.electro.dto.inventory.CountVariantResponse;
import com.electro.entity.inventory.Count;
import com.electro.entity.inventory.CountVariant;
import com.electro.entity.inventory.CountVariantKey;
import com.electro.entity.inventory.Warehouse;
import com.electro.entity.product.Product;
import com.electro.entity.product.Variant;
import com.electro.repository.inventory.CountRepository;
import com.electro.repository.inventory.CountVariantRepository;
import com.electro.repository.inventory.WarehouseRepository;
import com.electro.repository.product.ProductRepository;
import com.electro.repository.product.VariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CountVariantServiceImplTest {

    @Autowired
    private CountVariantService countVariantService;

    @Autowired
    private CountVariantRepository countVariantRepository;

    @Autowired
    private CountRepository countRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VariantRepository variantRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    private Count count;
    private Product product;
    private Variant variant;
    private CountVariant countVariant;
    private CountVariantKey countVariantKey;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        // Tạo warehouse
        warehouse = new Warehouse();
        warehouse.setName("Test Warehouse");
        warehouse.setCode("TEST-WH-001");
        warehouse.setStatus(1);
        warehouse = warehouseRepository.save(warehouse);

        // Tạo product
        product = new Product();
        product.setName("Test Product");
        product.setCode("TEST-PROD-001");
        product.setSlug("test-product-001");
        product.setStatus(1);
        product = productRepository.save(product);

        // Tạo variant
        variant = new Variant();
        variant.setSku("TEST-SKU-001");
        variant.setProduct(product);
        variant.setCost(100.0);
        variant.setPrice(150.0);
        variant.setStatus(1);
        variant = variantRepository.save(variant);

        // Tạo count
        count = new Count();
        count.setCode("TEST-COUNT-001");
        count.setWarehouse(warehouse);
        count.setStatus(1);
        count = countRepository.save(count);

        // Tạo count variant
        countVariantKey = new CountVariantKey(count.getId(), variant.getId());
        countVariant = new CountVariant();
        countVariant.setCountVariantKey(countVariantKey);
        countVariant.setCount(count);
        countVariant.setVariant(variant);
        countVariant.setInventory(10);
        countVariant.setActualInventory(10);
        countVariant = countVariantRepository.save(countVariant);
    }

    /**
     * Test ID: CV-SV-001
     * Test case: Tìm tất cả count variants với phân trang
     * Mục tiêu: Kiểm tra service trả về đúng danh sách count variants có phân trang
     */
    @Test
    void findAll_ShouldReturnPagedCountVariants() {
        // Act
        ListResponse<CountVariantResponse> result = countVariantService.findAll(0, 10, "id,desc", null, null, false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(variant.getId(), result.getContent().get(0).getVariant().getId());
        assertEquals(countVariant.getInventory(), result.getContent().get(0).getInventory());
        assertEquals(countVariant.getActualInventory(), result.getContent().get(0).getActualInventory());
    }

    /**
     * Test ID: CV-SV-002
     * Test case: Tìm tất cả count variants với filter
     * Mục tiêu: Kiểm tra service trả về đúng danh sách count variants theo filter
     */
    @Test
    void findAll_WithFilter_ShouldReturnFilteredCountVariants() {
        // Act
        ListResponse<CountVariantResponse> result = countVariantService.findAll(0, 10, "id,desc", 
            "inventory==10", null, false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(10, result.getContent().get(0).getInventory());
    }

    /**
     * Test ID: CV-SV-003
     * Test case: Tìm tất cả count variants với search
     * Mục tiêu: Kiểm tra service trả về đúng danh sách count variants theo search
     */
    @Test
    void findAll_WithSearch_ShouldReturnSearchedCountVariants() {
        // Act
        ListResponse<CountVariantResponse> result = countVariantService.findAll(0, 10, "id,desc", 
            null, "TEST-SKU-001", false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals("TEST-SKU-001", result.getContent().get(0).getVariant().getSku());
    }

    /**
     * Test ID: CV-SV-004
     * Test case: Tìm tất cả count variants với all=true
     * Mục tiêu: Kiểm tra service trả về tất cả count variants không phân trang
     */
    @Test
    void findAll_WithAllTrue_ShouldReturnAllCountVariants() {
        // Act
        ListResponse<CountVariantResponse> result = countVariantService.findAll(0, 10, "id,desc", 
            null, null, true);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
    }

    /**
     * Test ID: CV-SV-005
     * Test case: Tìm count variant theo ID tồn tại
     * Mục tiêu: Kiểm tra service trả về đúng count variant
     */
    @Test
    void findById_WithExistingId_ShouldReturnCountVariant() {
        // Act
        CountVariantResponse result = countVariantService.findById(countVariantKey);

        // Assert
        assertNotNull(result);
        assertEquals(variant.getId(), result.getVariant().getId());
        assertEquals(countVariant.getInventory(), result.getInventory());
        assertEquals(countVariant.getActualInventory(), result.getActualInventory());
    }

    /**
     * Test ID: CV-SV-006
     * Test case: Tìm count variant theo ID không tồn tại
     * Mục tiêu: Kiểm tra service ném ra ResourceNotFoundException
     */
    @Test
    void findById_WithNonExistentId_ShouldThrowException() {
        // Arrange
        CountVariantKey nonExistentKey = new CountVariantKey(999L, 999L);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> countVariantService.findById(nonExistentKey));
    }

    /**
     * Test ID: CV-SV-007
     * Test case: Lưu count variant mới
     * Mục tiêu: Kiểm tra service lưu thành công count variant mới
     */
    @Test
    void save_WithNewCountVariant_ShouldSaveSuccessfully() {
        // Arrange
        CountVariantRequest request = new CountVariantRequest();
        request.setVariantId(variant.getId());
        request.setInventory(20);
        request.setActualInventory(20);

        // Act
        CountVariantResponse result = countVariantService.save(request);

        // Assert
        assertNotNull(result);
        assertEquals(request.getVariantId(), result.getVariant().getId());
        assertEquals(request.getInventory(), result.getInventory());
        assertEquals(request.getActualInventory(), result.getActualInventory());

        // Verify saved in database
        CountVariant saved = countVariantRepository.findById(
            new CountVariantKey(count.getId(), result.getVariant().getId())).orElse(null);
        assertNotNull(saved);
        assertEquals(request.getInventory(), saved.getInventory());
        assertEquals(request.getActualInventory(), saved.getActualInventory());
    }

    /**
     * Test ID: CV-SV-008
     * Test case: Cập nhật count variant
     * Mục tiêu: Kiểm tra service cập nhật thành công count variant
     */
    @Test
    void save_WithExistingId_ShouldUpdateSuccessfully() {
        // Arrange
        CountVariantRequest request = new CountVariantRequest();
        request.setVariantId(variant.getId());
        request.setInventory(30);
        request.setActualInventory(30);

        // Act
        CountVariantResponse result = countVariantService.save(countVariantKey, request);

        // Assert
        assertNotNull(result);
        assertEquals(request.getVariantId(), result.getVariant().getId());
        assertEquals(request.getInventory(), result.getInventory());
        assertEquals(request.getActualInventory(), result.getActualInventory());

        // Verify updated in database
        CountVariant updated = countVariantRepository.findById(countVariantKey).orElse(null);
        assertNotNull(updated);
        assertEquals(request.getInventory(), updated.getInventory());
        assertEquals(request.getActualInventory(), updated.getActualInventory());
    }

    /**
     * Test ID: CV-SV-009
     * Test case: Xóa count variant
     * Mục tiêu: Kiểm tra service xóa thành công count variant
     */
    @Test
    void delete_ShouldDeleteCountVariant() {
        // Act
        countVariantService.delete(countVariantKey);

        // Assert
        assertFalse(countVariantRepository.existsById(countVariantKey));
    }

    /**
     * Test ID: CV-SV-010
     * Test case: Xóa nhiều count variants
     * Mục tiêu: Kiểm tra service xóa thành công nhiều count variants
     */
    @Test
    void delete_WithMultipleIds_ShouldDeleteAllCountVariants() {
        // Arrange
        CountVariantKey key2 = new CountVariantKey(count.getId(), variant.getId());
        List<CountVariantKey> ids = Arrays.asList(countVariantKey, key2);

        // Act
        countVariantService.delete(ids);

        // Assert
        ids.forEach(id -> assertFalse(countVariantRepository.existsById(id)));
    }

    /**
     * Test ID: CV-SV-011
     * Test case: Xóa count variant với ID không tồn tại
     * Mục tiêu: Kiểm tra service không ném ra exception khi xóa ID không tồn tại
     */
    @Test
    void delete_WithNonExistentId_ShouldNotThrowException() {
        // Arrange
        CountVariantKey nonExistentKey = new CountVariantKey(999L, 999L);

        // Act & Assert
        assertDoesNotThrow(() -> countVariantService.delete(nonExistentKey));
    }

    /**
     * Test ID: CV-SV-012
     * Test case: Xóa nhiều count variants với danh sách rỗng
     * Mục tiêu: Kiểm tra service không ném ra exception khi xóa danh sách rỗng
     */
    @Test
    void delete_WithEmptyList_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> countVariantService.delete(Collections.emptyList()));
    }
} 