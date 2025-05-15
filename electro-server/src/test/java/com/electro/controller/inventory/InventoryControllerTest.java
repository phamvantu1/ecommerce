package com.electro.controller.inventory;

import com.electro.constant.AppConstants;
import com.electro.constant.FieldName;
import com.electro.constant.ResourceName;
import com.electro.dto.ListResponse;
import com.electro.dto.inventory.ProductInventoryResponse;
import com.electro.dto.inventory.VariantInventoryResponse;
import com.electro.entity.inventory.Docket;
import com.electro.entity.inventory.DocketReason;
import com.electro.entity.inventory.DocketVariant;
import com.electro.entity.inventory.DocketVariantKey;
import com.electro.entity.inventory.Warehouse;
import com.electro.entity.product.Product;
import com.electro.entity.product.Variant;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.product.ProductInventoryMapper;
import com.electro.mapper.product.VariantInventoryMapper;
import com.electro.projection.inventory.ProductInventory;
import com.electro.projection.inventory.VariantInventory;
import com.electro.repository.inventory.DocketReasonRepository;
import com.electro.repository.inventory.DocketRepository;
import com.electro.repository.inventory.DocketVariantRepository;
import com.electro.repository.inventory.WarehouseRepository;
import com.electro.repository.product.ProductRepository;
import com.electro.repository.product.VariantRepository;
import com.electro.utils.InventoryUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InventoryControllerTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DocketVariantRepository docketVariantRepository;

    @Autowired
    private DocketRepository docketRepository;

    @Autowired
    private DocketReasonRepository docketReasonRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductInventoryMapper productInventoryMapper;

    @Autowired
    private VariantRepository variantRepository;

    @Autowired
    private VariantInventoryMapper variantInventoryMapper;

    @Autowired
    private InventoryController inventoryController;

    @BeforeEach
    @Transactional
    void setUp() {
        // Tạo Product 1
        Product product1 = new Product();
        product1.setName("Test Product 1");
        product1.setCode("TEST-PROD-001");
        product1.setSlug("test-product-1");
        product1.setStatus(1);
        product1 = productRepository.save(product1);

        // Tạo Product 2
        Product product2 = new Product();
        product2.setName("Test Product 2");
        product2.setCode("TEST-PROD-002");
        product2.setSlug("test-product-2");
        product2.setStatus(1);
        product2 = productRepository.save(product2);

        // Tạo Variant 1
        Variant variant1 = new Variant();
        variant1.setSku("TEST-SKU-001");
        variant1.setProduct(product1);
        variant1.setCost(100.0);
        variant1.setPrice(150.0);
        variant1.setStatus(1);
        variant1 = variantRepository.save(variant1);

        // Tạo Variant 2
        Variant variant2 = new Variant();
        variant2.setSku("TEST-SKU-002");
        variant2.setProduct(product2);
        variant2.setCost(200.0);
        variant2.setPrice(250.0);
        variant2.setStatus(1);
        variant2 = variantRepository.save(variant2);

        // Tạo Warehouse
        Warehouse warehouse = new Warehouse();
        warehouse.setName("Test Warehouse");
        warehouse.setCode("TEST-WH-001");
        warehouse.setStatus(1);
        warehouse = warehouseRepository.save(warehouse);

        // Tạo DocketReason
        DocketReason reason = new DocketReason();
        reason.setName("Test Reason");
        reason.setStatus(1);
        reason = docketReasonRepository.save(reason);

        // Tạo Docket
        Docket docket = new Docket();
        docket.setCode("TEST-DOC-001");
        docket.setType(1); // Import
        docket.setStatus(3); // Completed
        docket.setReason(reason);
        docket.setWarehouse(warehouse);
        docket = docketRepository.save(docket);

        // Tạo DocketVariant 1
        DocketVariant docketVariant1 = new DocketVariant();
        docketVariant1.setDocketVariantKey(new DocketVariantKey(docket.getId(), variant1.getId()));
        docketVariant1.setDocket(docket);
        docketVariant1.setVariant(variant1);
        docketVariant1.setQuantity(10);
        docketVariantRepository.save(docketVariant1);

        // Tạo DocketVariant 2
        DocketVariant docketVariant2 = new DocketVariant();
        docketVariant2.setDocketVariantKey(new DocketVariantKey(docket.getId(), variant2.getId()));
        docketVariant2.setDocket(docket);
        docketVariant2.setVariant(variant2);
        docketVariant2.setQuantity(20);
        docketVariantRepository.save(docketVariant2);
    }

    /**
     * Test ID: 1
     * Test case: Lấy danh sách tồn kho sản phẩm với trang và kích thước mặc định
     * 
     * Mục tiêu: 
     * - Kiểm tra controller trả về danh sách tồn kho sản phẩm với status OK
     * - Kiểm tra phân trang hoạt động đúng với các tham số mặc định
     * 
     * Dữ liệu test:
     * - Database có sẵn 102 sản phẩm
     * - Thêm 2 sản phẩm mới trong test
     * - Sử dụng page = 1, size = 10 (mặc định)
     * 
     * Kết quả mong muốn:
     * - Status code: 200 OK
     * - Tổng số bản ghi: 104 (102 + 2)
     * - Số bản ghi trong trang hiện tại: 10
     * - Trang hiện tại: 1
     * - Kích thước trang: 10
     * - Tổng số trang: 11 (104/10 làm tròn lên)
     * - Là trang cuối: false
     */
    @Test
    @Transactional
    void getProductInventories_WithDefaultPagination_ShouldReturnProductInventories() {
        // Act
        ResponseEntity<ListResponse<ProductInventoryResponse>> response = 
            inventoryController.getProductInventories(1, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(104, response.getBody().getTotalElements(), "Tổng số bản ghi phải là 104 (102 + 2)");
        assertEquals(10, response.getBody().getContent().size(), "Số bản ghi trong trang hiện tại phải là 10");
        assertEquals(1, response.getBody().getPage(), "Trang hiện tại phải là 1");
        assertEquals(10, response.getBody().getSize(), "Kích thước trang phải là 10");
        assertEquals(11, response.getBody().getTotalPages(), "Tổng số trang phải là 11 (104/10 làm tròn lên)");
        assertFalse(response.getBody().isLast(), "Đây không phải là trang cuối cùng");
    }

    /**
     * Test ID: 2
     * Test case: Lấy danh sách tồn kho sản phẩm ở trang cuối cùng
     * 
     * Mục tiêu:
     * - Kiểm tra controller trả về đúng số lượng sản phẩm ở trang cuối
     * - Kiểm tra phân trang hoạt động đúng với trang cuối
     * 
     * Dữ liệu test:
     * - Database có sẵn 102 sản phẩm
     * - Thêm 2 sản phẩm mới trong test
     * - Sử dụng page = 11 (trang cuối), size = 10
     * 
     * Kết quả mong muốn:
     * - Status code: 200 OK
     * - Tổng số bản ghi: 104 (102 + 2)
     * - Số bản ghi trong trang hiện tại: 4 (số dư của 104/10)
     * - Trang hiện tại: 11
     * - Kích thước trang: 10
     * - Tổng số trang: 11
     * - Là trang cuối: true
     */
    @Test
    @Transactional
    void getProductInventories_WithLastPage_ShouldReturnRemainingProducts() {
        // Act
        ResponseEntity<ListResponse<ProductInventoryResponse>> response = 
            inventoryController.getProductInventories(11, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(104, response.getBody().getTotalElements(), "Tổng số bản ghi phải là 104 (102 + 2)");
        assertEquals(4, response.getBody().getContent().size(), "Số bản ghi trong trang cuối phải là 4 (số dư của 104/10)");
        assertEquals(11, response.getBody().getPage(), "Trang hiện tại phải là 11");
        assertEquals(10, response.getBody().getSize(), "Kích thước trang phải là 10");
        assertEquals(11, response.getBody().getTotalPages(), "Tổng số trang phải là 11");
        assertTrue(response.getBody().isLast(), "Đây phải là trang cuối cùng");
    }

    /**
     * Test ID: 3
     * Test case: Lấy tồn kho của một phiên bản sản phẩm với ID hợp lệ
     * 
     * Mục tiêu:
     * - Kiểm tra controller trả về thông tin tồn kho của một phiên bản sản phẩm cụ thể
     * - Kiểm tra response có đúng format và status
     * 
     * Dữ liệu test:
     * - Variant ID = 1 (ID tồn tại trong database)
     * 
     * Kết quả mong muốn:
     * - Status code: 200 OK
     * - Response body không null
     * - Response body có chứa thông tin của variant với ID = 1
     */
    @Test
    @Transactional
    void getVariantInventory_WithValidId_ShouldReturnVariantInventory() {
        // Act
        ResponseEntity<VariantInventoryResponse> response = inventoryController.getVariantInventory(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody(), "Response body không được null");
        assertEquals(1L, response.getBody().getVariant().getId(), "ID của variant phải là 1");
    }

    /**
     * Test ID: 4
     * Test case: Lấy tồn kho của một phiên bản sản phẩm với ID không tồn tại
     * 
     * Mục tiêu:
     * - Kiểm tra controller xử lý đúng khi yêu cầu thông tin tồn kho của phiên bản không tồn tại
     * 
     * Dữ liệu test:
     * - Variant ID = 999 (ID không tồn tại trong database)
     * 
     * Kết quả mong muốn:
     * - Ném ra ResourceNotFoundException
     * - Thông báo lỗi: "Không tìm thấy sản phẩm tồn kho với ID: 999"
     */
    @Test
    @Transactional
    void getVariantInventory_WithNonExistentId_ShouldThrowException() {
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> inventoryController.getVariantInventory(999L)
        );
        assertEquals("Không tìm thấy sản phẩm tồn kho với ID: 999", exception.getMessage());
    }
} 