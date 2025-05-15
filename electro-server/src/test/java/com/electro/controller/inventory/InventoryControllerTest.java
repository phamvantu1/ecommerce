package com.electro.controller.inventory;

import com.electro.constant.AppConstants;
import com.electro.constant.FieldName;
import com.electro.constant.ResourceName;
import com.electro.dto.ListResponse;
import com.electro.dto.inventory.ProductInventoryResponse;
import com.electro.dto.inventory.VariantInventoryResponse;
import com.electro.entity.inventory.DocketVariant;
import com.electro.entity.inventory.DocketVariantKey;
import com.electro.entity.product.Product;
import com.electro.entity.product.Variant;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.product.ProductInventoryMapper;
import com.electro.mapper.product.VariantInventoryMapper;
import com.electro.projection.inventory.ProductInventory;
import com.electro.projection.inventory.VariantInventory;
import com.electro.repository.inventory.DocketVariantRepository;
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
        // Tạo dữ liệu test
        Product product1 = createProduct(1L, "Product 1");
        Product product2 = createProduct(2L, "Product 2");
        productRepository.saveAll(Arrays.asList(product1, product2));

        Variant variant1 = createVariant(1L, "Variant 1");
        Variant variant2 = createVariant(2L, "Variant 2");
        variantRepository.saveAll(Arrays.asList(variant1, variant2));

        DocketVariant docketVariant1 = createDocketVariant(1L, 1L, 10);
        DocketVariant docketVariant2 = createDocketVariant(1L, 2L, 20);
        docketVariantRepository.saveAll(Arrays.asList(docketVariant1, docketVariant2));
    }

    /**
     * Test ID: INV-CT-001
     * Test case: Lấy danh sách tồn kho sản phẩm với trang và kích thước mặc định
     * Mục tiêu: Kiểm tra controller trả về danh sách tồn kho sản phẩm với status OK
     */
    @Test
    @Transactional
    void getProductInventories_WithDefaultPagination_ShouldReturnProductInventories() {
        // Act
        ResponseEntity<ListResponse<ProductInventoryResponse>> response = 
            inventoryController.getProductInventories(
                Integer.parseInt(AppConstants.DEFAULT_PAGE_NUMBER),
                Integer.parseInt(AppConstants.DEFAULT_PAGE_SIZE)
            );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getContent().isEmpty());
        
        ProductInventoryResponse firstProduct = response.getBody().getContent().get(0);
        assertEquals(30, firstProduct.getInventory());
        assertEquals(0, firstProduct.getWaitingForDelivery());
        assertEquals(30, firstProduct.getCanBeSold());
        assertEquals(0, firstProduct.getAreComing());
    }

    /**
     * Test ID: INV-CT-002
     * Test case: Lấy danh sách tồn kho phiên bản sản phẩm với trang và kích thước mặc định
     * Mục tiêu: Kiểm tra controller trả về danh sách tồn kho phiên bản sản phẩm với status OK
     */
    @Test
    @Transactional
    void getVariantInventories_WithDefaultPagination_ShouldReturnVariantInventories() {
        // Act
        ResponseEntity<ListResponse<VariantInventoryResponse>> response = 
            inventoryController.getVariantInventories(
                Integer.parseInt(AppConstants.DEFAULT_PAGE_NUMBER),
                Integer.parseInt(AppConstants.DEFAULT_PAGE_SIZE)
            );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getContent().isEmpty());
        
        VariantInventoryResponse firstVariant = response.getBody().getContent().get(0);
        assertEquals(30, firstVariant.getInventory());
        assertEquals(0, firstVariant.getWaitingForDelivery());
        assertEquals(30, firstVariant.getCanBeSold());
        assertEquals(0, firstVariant.getAreComing());
    }

    /**
     * Test ID: INV-CT-003
     * Test case: Lấy tồn kho của một phiên bản sản phẩm với ID hợp lệ
     * Mục tiêu: Kiểm tra controller trả về thông tin tồn kho của phiên bản sản phẩm với status OK
     */
    @Test
    @Transactional
    void getVariantInventory_WithValidId_ShouldReturnVariantInventory() {
        // Act
        ResponseEntity<VariantInventoryResponse> response = inventoryController.getVariantInventory(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(30, response.getBody().getInventory());
        assertEquals(0, response.getBody().getWaitingForDelivery());
        assertEquals(30, response.getBody().getCanBeSold());
        assertEquals(0, response.getBody().getAreComing());
    }

    /**
     * Test ID: INV-CT-004
     * Test case: Lấy tồn kho của một phiên bản sản phẩm với ID không tồn tại
     * Mục tiêu: Kiểm tra controller ném ra ResourceNotFoundException
     */
    @Test
    @Transactional
    void getVariantInventory_WithNonExistentId_ShouldThrowException() {
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> inventoryController.getVariantInventory(999L));
    }

    // Helper methods
    private Product createProduct(Long id, String name) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        return product;
    }

    private Variant createVariant(Long id, String name) {
        Variant variant = new Variant();
        variant.setId(id);
        variant.setSku(name);
        return variant;
    }

    private DocketVariant createDocketVariant(Long docketId, Long variantId, Integer quantity) {
        DocketVariant docketVariant = new DocketVariant();
        docketVariant.setDocketVariantKey(new DocketVariantKey(docketId, variantId));
        docketVariant.setQuantity(quantity);
        return docketVariant;
    }
} 