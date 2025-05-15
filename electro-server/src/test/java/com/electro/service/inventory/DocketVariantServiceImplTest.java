package com.electro.service.inventory;

import com.electro.constant.ResourceName;
import com.electro.constant.SearchFields;
import com.electro.dto.ListResponse;
import com.electro.dto.inventory.DocketVariantRequest;
import com.electro.dto.inventory.DocketVariantResponse;
import com.electro.entity.inventory.Docket;
import com.electro.entity.inventory.DocketReason;
import com.electro.entity.inventory.DocketVariant;
import com.electro.entity.inventory.DocketVariantKey;
import com.electro.entity.inventory.Warehouse;
import com.electro.entity.product.Product;
import com.electro.entity.product.Variant;
import com.electro.repository.inventory.DocketReasonRepository;
import com.electro.repository.inventory.DocketRepository;
import com.electro.repository.inventory.DocketVariantRepository;
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
class DocketVariantServiceImplTest {

    @Autowired
    private DocketVariantService docketVariantService;

    @Autowired
    private DocketVariantRepository docketVariantRepository;

    @Autowired
    private DocketRepository docketRepository;

    @Autowired
    private DocketReasonRepository docketReasonRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VariantRepository variantRepository;

    private DocketVariant docketVariant;
    private Docket docket;
    private Variant variant;
    private DocketVariantKey docketVariantKey;

    @BeforeEach
    void setUp() {
        // Tạo warehouse
        Warehouse warehouse = new Warehouse();
        warehouse.setName("Test Warehouse");
        warehouse.setCode("TEST-WH-001");
        warehouse.setStatus(1);
        warehouse = warehouseRepository.save(warehouse);

        // Tạo reason
        DocketReason reason = new DocketReason();
        reason.setName("Test Reason");
        reason.setStatus(1);
        reason = docketReasonRepository.save(reason);

        // Tạo product
        Product product = new Product();
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

        // Tạo docket
        docket = new Docket();
        docket.setCode("TEST-DOCKET-001");
        docket.setWarehouse(warehouse);
        docket.setReason(reason);
        docket.setType(1);
        docket.setStatus(1);
        docket = docketRepository.save(docket);

        // Tạo docket variant
        docketVariantKey = new DocketVariantKey(docket.getId(), variant.getId());
        docketVariant = new DocketVariant();
        docketVariant.setDocketVariantKey(docketVariantKey);
        docketVariant.setDocket(docket);
        docketVariant.setVariant(variant);
        docketVariant.setQuantity(10);
        docketVariant = docketVariantRepository.save(docketVariant);
    }

    /**
     * Test ID: DV-SV-001
     * Test case: Tìm tất cả docket variants với phân trang
     * Mục tiêu: Kiểm tra service trả về đúng danh sách docket variants có phân trang
     */
    @Test
    void findAll_ShouldReturnPagedDocketVariants() {
        // Act
        ListResponse<DocketVariantResponse> result = docketVariantService.findAll(0, 10, "id,desc", null, null, false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(docketVariant.getQuantity(), result.getContent().get(0).getQuantity());
    }

    /**
     * Test ID: DV-SV-002
     * Test case: Tìm tất cả docket variants với filter
     * Mục tiêu: Kiểm tra service trả về đúng danh sách docket variants theo filter
     */
    @Test
    void findAll_WithFilter_ShouldReturnFilteredDocketVariants() {
        // Act
        ListResponse<DocketVariantResponse> result = docketVariantService.findAll(0, 10, "id,desc", 
            "quantity==10", null, false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(10, result.getContent().get(0).getQuantity());
    }

    /**
     * Test ID: DV-SV-003
     * Test case: Tìm tất cả docket variants với search
     * Mục tiêu: Kiểm tra service trả về đúng danh sách docket variants theo search
     */
    @Test
    void findAll_WithSearch_ShouldReturnSearchedDocketVariants() {
        // Act
        ListResponse<DocketVariantResponse> result = docketVariantService.findAll(0, 10, "id,desc", 
            null, "TEST-SKU-001", false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals("TEST-SKU-001", result.getContent().get(0).getVariant().getSku());
    }

    /**
     * Test ID: DV-SV-004
     * Test case: Tìm tất cả docket variants với all=true
     * Mục tiêu: Kiểm tra service trả về tất cả docket variants không phân trang
     */
    @Test
    void findAll_WithAllTrue_ShouldReturnAllDocketVariants() {
        // Act
        ListResponse<DocketVariantResponse> result = docketVariantService.findAll(0, 10, "id,desc", 
            null, null, true);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
    }

    /**
     * Test ID: DV-SV-005
     * Test case: Tìm docket variant theo ID tồn tại
     * Mục tiêu: Kiểm tra service trả về đúng docket variant
     */
    @Test
    void findById_WithExistingId_ShouldReturnDocketVariant() {
        // Act
        DocketVariantResponse result = docketVariantService.findById(docketVariantKey);

        // Assert
        assertNotNull(result);
        assertEquals(docketVariant.getQuantity(), result.getQuantity());
        assertEquals(docketVariant.getVariant().getId(), result.getVariant().getId());
    }

    /**
     * Test ID: DV-SV-006
     * Test case: Tìm docket variant theo ID không tồn tại
     * Mục tiêu: Kiểm tra service ném ra ResourceNotFoundException
     */
    @Test
    void findById_WithNonExistentId_ShouldThrowException() {
        // Arrange
        DocketVariantKey nonExistentKey = new DocketVariantKey(999L, 999L);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> docketVariantService.findById(nonExistentKey));
    }

    /**
     * Test ID: DV-SV-007
     * Test case: Lưu docket variant mới
     * Mục tiêu: Kiểm tra service lưu thành công docket variant mới
     */
    @Test
    void save_WithNewDocketVariant_ShouldSaveSuccessfully() {
        // Arrange
        DocketVariantRequest request = new DocketVariantRequest();
        request.setVariantId(variant.getId());
        request.setQuantity(20);

        // Act
        DocketVariantResponse result = docketVariantService.save(request);

        // Assert
        assertNotNull(result);
        assertEquals(request.getQuantity(), result.getQuantity());
        assertEquals(request.getVariantId(), result.getVariant().getId());
    }

    /**
     * Test ID: DV-SV-008
     * Test case: Cập nhật docket variant
     * Mục tiêu: Kiểm tra service cập nhật thành công docket variant
     */
    @Test
    void save_WithExistingId_ShouldUpdateSuccessfully() {
        // Arrange
        DocketVariantRequest request = new DocketVariantRequest();
        request.setVariantId(variant.getId());
        request.setQuantity(30);

        // Act
        DocketVariantResponse result = docketVariantService.save(docketVariantKey, request);

        // Assert
        assertNotNull(result);
        assertEquals(request.getQuantity(), result.getQuantity());
        assertEquals(request.getVariantId(), result.getVariant().getId());
    }

    /**
     * Test ID: DV-SV-009
     * Test case: Cập nhật docket variant với ID không tồn tại
     * Mục tiêu: Kiểm tra service ném ra ResourceNotFoundException
     */
    @Test
    void save_WithNonExistentId_ShouldThrowException() {
        // Arrange
        DocketVariantKey nonExistentKey = new DocketVariantKey(999L, 999L);
        DocketVariantRequest request = new DocketVariantRequest();
        request.setVariantId(variant.getId());
        request.setQuantity(30);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> docketVariantService.save(nonExistentKey, request));
    }

    /**
     * Test ID: DV-SV-010
     * Test case: Xóa docket variant
     * Mục tiêu: Kiểm tra service xóa thành công docket variant
     */
    @Test
    void delete_ShouldDeleteDocketVariant() {
        // Act
        docketVariantService.delete(docketVariantKey);

        // Assert
        assertFalse(docketVariantRepository.existsById(docketVariantKey));
    }

    /**
     * Test ID: DV-SV-011
     * Test case: Xóa nhiều docket variants
     * Mục tiêu: Kiểm tra service xóa thành công nhiều docket variants
     */
    @Test
    void delete_WithMultipleIds_ShouldDeleteAllDocketVariants() {
        // Arrange
        DocketVariantKey key2 = new DocketVariantKey(docket.getId(), variant.getId());
        List<DocketVariantKey> ids = Arrays.asList(docketVariantKey, key2);

        // Act
        docketVariantService.delete(ids);

        // Assert
        ids.forEach(id -> assertFalse(docketVariantRepository.existsById(id)));
    }

    /**
     * Test ID: DV-SV-012
     * Test case: Xóa docket variant với ID không tồn tại
     * Mục tiêu: Kiểm tra service không ném ra exception khi xóa ID không tồn tại
     */
    @Test
    void delete_WithNonExistentId_ShouldNotThrowException() {
        // Arrange
        DocketVariantKey nonExistentKey = new DocketVariantKey(999L, 999L);

        // Act & Assert
        assertDoesNotThrow(() -> docketVariantService.delete(nonExistentKey));
    }

    /**
     * Test ID: DV-SV-013
     * Test case: Xóa nhiều docket variants với danh sách rỗng
     * Mục tiêu: Kiểm tra service không ném ra exception khi xóa danh sách rỗng
     */
    @Test
    void delete_WithEmptyList_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> docketVariantService.delete(Collections.emptyList()));
    }
} 