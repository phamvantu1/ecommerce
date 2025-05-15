package com.electro.service.inventory;

import com.electro.constant.ResourceName;
import com.electro.constant.SearchFields;
import com.electro.dto.ListResponse;
import com.electro.dto.inventory.DocketRequest;
import com.electro.dto.inventory.DocketResponse;
import com.electro.entity.inventory.Docket;
import com.electro.entity.inventory.DocketReason;
import com.electro.entity.inventory.DocketVariant;
import com.electro.entity.inventory.Warehouse;
import com.electro.entity.product.Product;
import com.electro.entity.product.Variant;
import com.electro.repository.inventory.DocketReasonRepository;
import com.electro.repository.inventory.DocketRepository;
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
class DocketServiceImplTest {

    @Autowired
    private DocketService docketService;

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

    private Docket docket;
    private Warehouse warehouse;
    private Product product;
    private Variant variant;
    private DocketReason reason;

    @BeforeEach
    void setUp() {
        // Tạo warehouse
        warehouse = new Warehouse();
        warehouse.setName("Test Warehouse");
        warehouse.setCode("TEST-WH-001");
        warehouse.setStatus(1);
        warehouse = warehouseRepository.save(warehouse);

        // Tạo reason
        reason = new DocketReason();
        reason.setName("Test Reason");
        reason.setStatus(1);
        reason = docketReasonRepository.save(reason);

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

        // Tạo docket
        docket = new Docket();
        docket.setCode("TEST-DOCKET-001");
        docket.setWarehouse(warehouse);
        docket.setReason(reason);
        docket.setType(1); // Docket nhập
        docket.setStatus(1);
        docket = docketRepository.save(docket);

        // Tạo docket variant
        DocketVariant docketVariant = new DocketVariant();
        docketVariant.setDocket(docket);
        docketVariant.setVariant(variant);
        docketVariant.setQuantity(10);
        docket.getDocketVariants().add(docketVariant);
        docket = docketRepository.save(docket);
    }

    /**
     * Test ID: DOCKET-SV-001
     * Test case: Tìm tất cả dockets với phân trang
     * Mục tiêu: Kiểm tra service trả về đúng danh sách dockets có phân trang
     */
    @Test
    void findAll_ShouldReturnPagedDockets() {
        // Act
        ListResponse<DocketResponse> result = docketService.findAll(0, 10, "id,desc", null, null, false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(docket.getCode(), result.getContent().get(0).getCode());
    }

    /**
     * Test ID: DOCKET-SV-002
     * Test case: Tìm tất cả dockets với filter
     * Mục tiêu: Kiểm tra service trả về đúng danh sách dockets theo filter
     */
    @Test
    void findAll_WithFilter_ShouldReturnFilteredDockets() {
        // Act
        ListResponse<DocketResponse> result = docketService.findAll(0, 10, "id,desc", 
            "type==1", null, false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().get(0).getType());
    }

    /**
     * Test ID: DOCKET-SV-003
     * Test case: Tìm tất cả dockets với search
     * Mục tiêu: Kiểm tra service trả về đúng danh sách dockets theo search
     */
    @Test
    void findAll_WithSearch_ShouldReturnSearchedDockets() {
        // Act
        ListResponse<DocketResponse> result = docketService.findAll(0, 10, "id,desc", 
            null, "TEST-DOCKET", false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals("TEST-DOCKET-001", result.getContent().get(0).getCode());
    }

    /**
     * Test ID: DOCKET-SV-004
     * Test case: Tìm tất cả dockets với all=true
     * Mục tiêu: Kiểm tra service trả về tất cả dockets không phân trang
     */
    @Test
    void findAll_WithAllTrue_ShouldReturnAllDockets() {
        // Act
        ListResponse<DocketResponse> result = docketService.findAll(0, 10, "id,desc", 
            null, null, true);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
    }

    /**
     * Test ID: DOCKET-SV-005
     * Test case: Tìm docket theo ID tồn tại
     * Mục tiêu: Kiểm tra service trả về đúng docket
     */
    @Test
    void findById_WithExistingId_ShouldReturnDocket() {
        // Act
        DocketResponse result = docketService.findById(docket.getId());

        // Assert
        assertNotNull(result);
        assertEquals(docket.getCode(), result.getCode());
        assertEquals(docket.getType(), result.getType());
        assertEquals(docket.getStatus(), result.getStatus());
    }

    /**
     * Test ID: DOCKET-SV-006
     * Test case: Tìm docket theo ID không tồn tại
     * Mục tiêu: Kiểm tra service ném ra ResourceNotFoundException
     */
    @Test
    void findById_WithNonExistentId_ShouldThrowException() {
        // Act & Assert
        assertThrows(RuntimeException.class, () -> docketService.findById(999L));
    }

    /**
     * Test ID: DOCKET-SV-007
     * Test case: Lưu docket mới
     * Mục tiêu: Kiểm tra service lưu thành công docket mới
     */
    @Test
    void save_WithNewDocket_ShouldSaveSuccessfully() {
        // Arrange
        DocketRequest request = new DocketRequest();
        request.setCode("TEST-DOCKET-002");
        request.setWarehouseId(warehouse.getId());
        request.setReasonId(reason.getId());
        request.setType(1);
        request.setStatus(1);

        // Act
        DocketResponse result = docketService.save(request);

        // Assert
        assertNotNull(result);
        assertEquals(request.getCode(), result.getCode());
        assertEquals(request.getType(), result.getType());
        assertEquals(request.getStatus(), result.getStatus());
    }

    /**
     * Test ID: DOCKET-SV-008
     * Test case: Cập nhật docket
     * Mục tiêu: Kiểm tra service cập nhật thành công docket
     */
    @Test
    void save_WithExistingId_ShouldUpdateSuccessfully() {
        // Arrange
        DocketRequest request = new DocketRequest();
        request.setCode("TEST-DOCKET-UPDATED");
        request.setWarehouseId(warehouse.getId());
        request.setReasonId(reason.getId());
        request.setType(1);
        request.setStatus(3); // Hoàn thành

        // Act
        DocketResponse result = docketService.save(docket.getId(), request);

        // Assert
        assertNotNull(result);
        assertEquals(request.getCode(), result.getCode());
        assertEquals(request.getType(), result.getType());
        assertEquals(request.getStatus(), result.getStatus());
    }

    /**
     * Test ID: DOCKET-SV-009
     * Test case: Cập nhật docket với ID không tồn tại
     * Mục tiêu: Kiểm tra service ném ra ResourceNotFoundException
     */
    @Test
    void save_WithNonExistentId_ShouldThrowException() {
        // Arrange
        DocketRequest request = new DocketRequest();
        request.setCode("TEST-DOCKET-UPDATED");
        request.setWarehouseId(warehouse.getId());
        request.setReasonId(reason.getId());
        request.setType(1);
        request.setStatus(3);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> docketService.save(999L, request));
    }

    /**
     * Test ID: DOCKET-SV-010
     * Test case: Cập nhật docket với type khác 1
     * Mục tiêu: Kiểm tra service cập nhật thành công docket với type khác 1
     */
    @Test
    void save_WithDifferentType_ShouldUpdateSuccessfully() {
        // Arrange
        DocketRequest request = new DocketRequest();
        request.setCode("TEST-DOCKET-UPDATED");
        request.setWarehouseId(warehouse.getId());
        request.setReasonId(reason.getId());
        request.setType(2); // Type khác 1
        request.setStatus(3);

        // Act
        DocketResponse result = docketService.save(docket.getId(), request);

        // Assert
        assertNotNull(result);
        assertEquals(request.getCode(), result.getCode());
        assertEquals(request.getType(), result.getType());
        assertEquals(request.getStatus(), result.getStatus());
    }

    /**
     * Test ID: DOCKET-SV-011
     * Test case: Cập nhật docket với status khác 3
     * Mục tiêu: Kiểm tra service cập nhật thành công docket với status khác 3
     */
    @Test
    void save_WithDifferentStatus_ShouldUpdateSuccessfully() {
        // Arrange
        DocketRequest request = new DocketRequest();
        request.setCode("TEST-DOCKET-UPDATED");
        request.setWarehouseId(warehouse.getId());
        request.setReasonId(reason.getId());
        request.setType(1);
        request.setStatus(2); // Status khác 3

        // Act
        DocketResponse result = docketService.save(docket.getId(), request);

        // Assert
        assertNotNull(result);
        assertEquals(request.getCode(), result.getCode());
        assertEquals(request.getType(), result.getType());
        assertEquals(request.getStatus(), result.getStatus());
    }

    /**
     * Test ID: DOCKET-SV-012
     * Test case: Xóa docket
     * Mục tiêu: Kiểm tra service xóa thành công docket
     */
    @Test
    void delete_ShouldDeleteDocket() {
        // Act
        docketService.delete(docket.getId());

        // Assert
        assertFalse(docketRepository.existsById(docket.getId()));
    }

    /**
     * Test ID: DOCKET-SV-013
     * Test case: Xóa nhiều dockets
     * Mục tiêu: Kiểm tra service xóa thành công nhiều dockets
     */
    @Test
    void delete_WithMultipleIds_ShouldDeleteAllDockets() {
        // Arrange
        Docket docket2 = new Docket();
        docket2.setCode("TEST-DOCKET-002");
        docket2.setWarehouse(warehouse);
        docket2.setReason(reason);
        docket2.setType(1);
        docket2.setStatus(1);
        docket2 = docketRepository.save(docket2);

        List<Long> ids = Arrays.asList(docket.getId(), docket2.getId());

        // Act
        docketService.delete(ids);

        // Assert
        ids.forEach(id -> assertFalse(docketRepository.existsById(id)));
    }

    /**
     * Test ID: DOCKET-SV-014
     * Test case: Xóa docket với ID không tồn tại
     * Mục tiêu: Kiểm tra service không ném ra exception khi xóa ID không tồn tại
     */
    @Test
    void delete_WithNonExistentId_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> docketService.delete(999L));
    }

    /**
     * Test ID: DOCKET-SV-015
     * Test case: Xóa nhiều dockets với danh sách rỗng
     * Mục tiêu: Kiểm tra service không ném ra exception khi xóa danh sách rỗng
     */
    @Test
    void delete_WithEmptyList_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> docketService.delete(Collections.emptyList()));
    }
} 