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
     * Test ID: 6
     * Test case: Tìm tất cả count variants với phân trang
     * 
     * Mục tiêu:
     * - Kiểm tra service trả về đúng danh sách count variants có phân trang
     * - Kiểm tra thông tin phân trang chính xác
     * 
     * Dữ liệu test:
     * - Page: 1
     * - Size: 5
     * - Sort: countVariantKey,desc
     * - Filter: null
     * - Search: null
     * - All: false
     * 
     * Kết quả mong muốn:
     * - Danh sách không null
     * - Số lượng bản ghi = 5 (do page size = 5, mặc dù tổng số bản ghi là 6)
     * - Tổng số bản ghi = 6 (4 bản ghi có sẵn + 1 bản ghi từ setUp + 1 bản ghi thêm mới)
     */
    @Test
    void findAll_ShouldReturnPagedCountVariants() {
        // Arrange
        // Tạo thêm một variant mới
        Variant newVariant = new Variant();
        newVariant.setSku("TEST-SKU-002");
        newVariant.setProduct(product);
        newVariant.setCost(200.0);
        newVariant.setPrice(250.0);
        newVariant.setStatus(1);
        newVariant = variantRepository.save(newVariant);

        // Tạo thêm một count variant mới
        CountVariantKey newKey = new CountVariantKey(count.getId(), newVariant.getId());
        CountVariant newCountVariant = new CountVariant();
        newCountVariant.setCountVariantKey(newKey);
        newCountVariant.setCount(count);
        newCountVariant.setVariant(newVariant);
        newCountVariant.setInventory(15);
        newCountVariant.setActualInventory(15);
        countVariantRepository.save(newCountVariant);

        // Act
        ListResponse<CountVariantResponse> result = countVariantService.findAll(1, 5, "countVariantKey,desc", null, null, false);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.getContent().size()); // Chỉ trả về 5 bản ghi do page size = 5
        assertEquals(6, result.getTotalElements()); // Tổng số bản ghi là 6
    }

    /**
     * Test ID: 7
     * Test case: Tìm tất cả count variants với filter
     * 
     * Mục tiêu:
     * - Kiểm tra service trả về đúng danh sách count variants theo filter
     * - Kiểm tra filter inventory hoạt động chính xác
     * 
     * Dữ liệu test:
     * - Page: 1
     * - Size: 10
     * - Sort: countVariantKey,desc
     * - Filter: inventory==10
     * - Search: null
     * - All: false
     * 
     * Kết quả mong muốn:
     * - Danh sách không null
     * - Số lượng bản ghi = 1 (số bản ghi có inventory = 10 từ setUp)
     * - Tổng số bản ghi = 1
     * - Inventory = 10
     */
    @Test
    void findAll_WithFilter_ShouldReturnFilteredCountVariants() {
        // Act
        ListResponse<CountVariantResponse> result = countVariantService.findAll(1, 10, "countVariantKey,desc", 
            "inventory==10", null, false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(10, result.getContent().get(0).getInventory());
    }

    /**
     * Test ID: 8
     * Test case: Tìm tất cả count variants với search
     * 
     * Mục tiêu:
     * - Kiểm tra service trả về đúng danh sách count variants theo search
     * - Kiểm tra tìm kiếm theo SKU hoạt động chính xác
     * 
     * Dữ liệu test:
     * - Page: 1
     * - Size: 10
     * - Sort: countVariantKey,desc
     * - Filter: null
     * - Search: variant.sku==TEST-SKU-001
     * - All: false
     * 
     * Kết quả mong muốn:
     * - Danh sách không null
     * - Số lượng bản ghi = 1 (số bản ghi có SKU = TEST-SKU-001 từ setUp)
     * - Tổng số bản ghi = 1
     * - SKU = TEST-SKU-001
     */
    @Test
    void findAll_WithSearch_ShouldReturnSearchedCountVariants() {
        // Act
        ListResponse<CountVariantResponse> result = countVariantService.findAll(1, 10, "countVariantKey,desc", 
            null, "variant.sku==TEST-SKU-001", false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals("TEST-SKU-001", result.getContent().get(0).getVariant().getSku());
    }

    /**
     * Test ID: 9
     * Test case: Tìm tất cả count variants với all=true
     * 
     * Mục tiêu:
     * - Kiểm tra service trả về tất cả count variants không phân trang
     * - Kiểm tra thông tin phân trang khi all=true
     * 
     * Dữ liệu test:
     * - Page: 1
     * - Size: 5
     * - Sort: countVariantKey,desc
     * - Filter: null
     * - Search: null
     * - All: true
     * 
     * Kết quả mong muốn:
     * - Danh sách không null
     * - Số lượng bản ghi = 6 (4 bản ghi có sẵn + 1 bản ghi từ setUp + 1 bản ghi thêm mới)
     * - Tổng số bản ghi = 6
     */
    @Test
    void findAll_WithAllTrue_ShouldReturnAllCountVariants() {
        // Arrange
        // Tạo thêm một variant mới
        Variant newVariant = new Variant();
        newVariant.setSku("TEST-SKU-002");
        newVariant.setProduct(product);
        newVariant.setCost(200.0);
        newVariant.setPrice(250.0);
        newVariant.setStatus(1);
        newVariant = variantRepository.save(newVariant);

        // Tạo thêm một count variant mới
        CountVariantKey newKey = new CountVariantKey(count.getId(), newVariant.getId());
        CountVariant newCountVariant = new CountVariant();
        newCountVariant.setCountVariantKey(newKey);
        newCountVariant.setCount(count);
        newCountVariant.setVariant(newVariant);
        newCountVariant.setInventory(15);
        newCountVariant.setActualInventory(15);
        countVariantRepository.save(newCountVariant);

        // Act
        ListResponse<CountVariantResponse> result = countVariantService.findAll(1, 5, "countVariantKey,desc", 
            null, null, true);

        // Assert
        assertNotNull(result);
        assertEquals(6, result.getContent().size());
        assertEquals(6, result.getTotalElements());
    }

    /**
     * Test ID: 10
     * Test case: Tìm count variant theo ID tồn tại
     * 
     * Mục tiêu:
     * - Kiểm tra service trả về đúng count variant
     * - Kiểm tra thông tin trả về chính xác
     * 
     * Dữ liệu test:
     * - CountVariantKey: countVariantKey (đã tồn tại)
     * 
     * Kết quả mong muốn:
     * - Response không null
     * - Variant ID chính xác
     * - Inventory và Actual Inventory chính xác
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
     * Test ID: 11
     * Test case: Tìm count variant theo ID không tồn tại
     * 
     * Mục tiêu:
     * - Kiểm tra service ném ra ResourceNotFoundException
     * 
     * Dữ liệu test:
     * - CountVariantKey: nonExistentKey (không tồn tại)
     * 
     * Kết quả mong muốn:
     * - Throw RuntimeException
     * - Thông báo lỗi "Không tìm thấy count variant với ID không tồn tại"
     */
    @Test
    void findById_WithNonExistentId_ShouldThrowException() {
        // Arrange
        CountVariantKey nonExistentKey = new CountVariantKey(999L, 999L);
        String expectedMessage = "Không tìm thấy count variant với ID không tồn tại";

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, 
            () -> countVariantService.findById(nonExistentKey));
        
        assertEquals(expectedMessage, exception.getMessage());
    }

    /**
     * Test ID: 12
     * Test case: Lưu count variant mới
     * 
     * Mục tiêu:
     * - Kiểm tra service lưu thành công count variant mới
     * - Kiểm tra thông tin được lưu chính xác
     * 
     * Dữ liệu test:
     * - CountVariantRequest:
     *   + variantId: variant.getId()
     *   + inventory: 20
     *   + actualInventory: 20
     * 
     * Kết quả mong muốn:
     * - Response không null
     * - Variant ID trong response = request
     * - Inventory = 20
     * - Actual Inventory = 20
     * - Bản ghi được lưu trong database
     */
    @Test
    void save_WithNewCountVariant_ShouldSaveSuccessfully() {
        // Arrange
        CountVariantRequest request = new CountVariantRequest();
        request.setVariantId(variant.getId());
        request.setInventory(20);
        request.setActualInventory(20);

        // Tạo một count mới để test
        Count newCount = new Count();
        newCount.setCode("TEST-COUNT-002");
        newCount.setWarehouse(warehouse);
        newCount.setStatus(1);
        newCount = countRepository.save(newCount);

        // Tạo CountVariant mới với đầy đủ thông tin
        CountVariant newCountVariant = new CountVariant();
        CountVariantKey key = new CountVariantKey(newCount.getId(), variant.getId());
        newCountVariant.setCountVariantKey(key);
        newCountVariant.setCount(newCount);
        newCountVariant.setVariant(variant);
        newCountVariant.setInventory(request.getInventory());
        newCountVariant.setActualInventory(request.getActualInventory());
        countVariantRepository.save(newCountVariant);

        // Act
        CountVariantResponse result = countVariantService.save(request);

        // Assert
        assertNotNull(result);
        assertEquals(request.getVariantId(), result.getVariant().getId());
        assertEquals(request.getInventory(), result.getInventory());
        assertEquals(request.getActualInventory(), result.getActualInventory());

        // Verify saved in database
        CountVariant saved = countVariantRepository.findById(key).orElse(null);
        assertNotNull(saved);
        assertEquals(request.getInventory(), saved.getInventory());
        assertEquals(request.getActualInventory(), saved.getActualInventory());
    }

    /**
     * Test ID: 13
     * Test case: Cập nhật count variant
     * 
     * Mục tiêu:
     * - Kiểm tra service cập nhật thành công count variant
     * - Kiểm tra thông tin được cập nhật chính xác
     * 
     * Dữ liệu test:
     * - CountVariantKey: countVariantKey (đã tồn tại)
     * - CountVariantRequest:
     *   + variantId: variant.getId()
     *   + inventory: 30
     *   + actualInventory: 30
     * 
     * Kết quả mong muốn:
     * - Response không null
     * - Variant ID trong response = request
     * - Inventory = 30
     * - Actual Inventory = 30
     * - Bản ghi được cập nhật trong database
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
     * Test ID: 14
     * Test case: Xóa count variant
     * 
     * Mục tiêu:
     * - Kiểm tra service xóa thành công count variant
     * - Kiểm tra bản ghi đã được xóa khỏi database
     * 
     * Dữ liệu test:
     * - CountVariantKey: countVariantKey (đã tồn tại)
     * 
     * Kết quả mong muốn:
     * - Không có exception
     * - Bản ghi không còn tồn tại trong database
     */
    @Test
    void delete_ShouldDeleteCountVariant() {
        // Act
        countVariantService.delete(countVariantKey);

        // Assert
        assertFalse(countVariantRepository.existsById(countVariantKey));
    }

    /**
     * Test ID: 15
     * Test case: Xóa nhiều count variants
     * 
     * Mục tiêu:
     * - Kiểm tra service xóa thành công nhiều count variants
     * - Kiểm tra các bản ghi đã được xóa khỏi database
     * 
     * Dữ liệu test:
     * - Danh sách CountVariantKey: [countVariantKey]
     * 
     * Kết quả mong muốn:
     * - Không có exception
     * - Các bản ghi không còn tồn tại trong database
     */
    @Test
    void delete_WithMultipleIds_ShouldDeleteAllCountVariants() {
        // Arrange
        List<CountVariantKey> ids = Arrays.asList(countVariantKey);

        // Act
        countVariantService.delete(ids);

        // Assert
        ids.forEach(id -> assertFalse(countVariantRepository.existsById(id)));
    }

    /**
     * Test ID: 16
     * Test case: Xóa count variant với ID không tồn tại
     * 
     * Mục tiêu:
     * - Kiểm tra service ném ra exception khi xóa ID không tồn tại
     * 
     * Dữ liệu test:
     * - CountVariantKey: nonExistentKey (không tồn tại)
     * 
     * Kết quả mong muốn:
     * - Throw RuntimeException
     * - Thông báo lỗi "Không tìm thấy count variant với ID không tồn tại"
     */
    @Test
    void delete_WithNonExistentId_ShouldThrowException() {
        // Arrange
        CountVariantKey nonExistentKey = new CountVariantKey(999L, 999L);
        String expectedMessage = "Không tìm thấy count variant với ID không tồn tại";

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, 
            () -> countVariantService.delete(nonExistentKey));
        
        assertEquals(expectedMessage, exception.getMessage());
    }

    /**
     * Test ID: 17
     * Test case: Xóa nhiều count variants với danh sách rỗng
     * 
     * Mục tiêu:
     * - Kiểm tra service không ném ra exception khi xóa danh sách rỗng
     * 
     * Dữ liệu test:
     * - Danh sách rỗng
     * 
     * Kết quả mong muốn:
     * - Không có exception
     */
    @Test
    void delete_WithEmptyList_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> countVariantService.delete(Collections.emptyList()));
    }
} 