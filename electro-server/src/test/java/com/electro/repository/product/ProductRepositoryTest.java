package com.electro.repository.product;

import com.electro.entity.inventory.Docket;
import com.electro.entity.inventory.DocketReason;
import com.electro.entity.inventory.DocketVariant;
import com.electro.entity.inventory.Warehouse;
import com.electro.entity.product.Product;
import com.electro.entity.product.Variant;
import com.electro.repository.inventory.DocketReasonRepository;
import com.electro.repository.inventory.DocketRepository;
import com.electro.repository.inventory.DocketVariantRepository;
import com.electro.repository.inventory.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VariantRepository variantRepository;

    @Autowired
    private DocketVariantRepository docketVariantRepository;

    @Autowired
    private DocketRepository docketRepository;

    @Autowired
    private DocketReasonRepository docketReasonRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    private Product product;
    private Variant variant;
    private Docket docket;
    private DocketVariant docketVariant;

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
        docket.setType(1); // Nhập kho
        docket.setStatus(3); // Hoàn thành
        docket = docketRepository.save(docket);

        // Tạo docket variant
        docketVariant = new DocketVariant();
        docketVariant.setDocket(docket);
        docketVariant.setVariant(variant);
        docketVariant.setQuantity(10);
        docketVariant = docketVariantRepository.save(docketVariant);
    }

    /**
     * Test ID: PR-001
     * Test case: Tìm sản phẩm đã có phiếu nhập/xuất
     * Mục tiêu: Kiểm tra repository trả về đúng danh sách sản phẩm có phiếu
     */
    @Test
    void findDocketedProducts_ShouldReturnProductsWithDockets() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findDocketedProducts(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(product.getId(), result.getContent().get(0).getId());
    }

    /**
     * Test ID: PR-002
     * Test case: Tìm sản phẩm với saleable=true
     * Mục tiêu: Kiểm tra repository trả về đúng danh sách sản phẩm có thể bán
     */
    @Test
    void findByParams_WithSaleableTrue_ShouldReturnSaleableProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByParams(null, null, null, true, false, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(product.getId(), result.getContent().get(0).getId());
    }

    /**
     * Test ID: PR-003
     * Test case: Tìm sản phẩm với newable=true
     * Mục tiêu: Kiểm tra repository trả về đúng danh sách sản phẩm mới nhất
     */
    @Test
    void findByParams_WithNewableTrue_ShouldReturnNewestProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByParams(null, null, null, false, true, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(product.getId(), result.getContent().get(0).getId());
    }

    /**
     * Test ID: PR-004
     * Test case: Tìm sản phẩm với sort=lowest-price
     * Mục tiêu: Kiểm tra repository trả về đúng danh sách sản phẩm sắp xếp theo giá thấp nhất
     */
    @Test
    void findByParams_WithLowestPriceSort_ShouldReturnSortedProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByParams(null, "lowest-price", null, false, false, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(product.getId(), result.getContent().get(0).getId());
    }

    /**
     * Test ID: PR-005
     * Test case: Tìm sản phẩm với sort=highest-price
     * Mục tiêu: Kiểm tra repository trả về đúng danh sách sản phẩm sắp xếp theo giá cao nhất
     */
    @Test
    void findByParams_WithHighestPriceSort_ShouldReturnSortedProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByParams(null, "highest-price", null, false, false, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(product.getId(), result.getContent().get(0).getId());
    }

    /**
     * Test ID: PR-006
     * Test case: Tìm sản phẩm với sort=random
     * Mục tiêu: Kiểm tra repository trả về đúng danh sách sản phẩm sắp xếp ngẫu nhiên
     */
    @Test
    void findByParams_WithRandomSort_ShouldReturnRandomProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByParams(null, "random", null, false, false, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(product.getId(), result.getContent().get(0).getId());
    }

    /**
     * Test ID: PR-007
     * Test case: Tìm sản phẩm với filter
     * Mục tiêu: Kiểm tra repository trả về đúng danh sách sản phẩm theo filter
     */
    @Test
    void findByParams_WithFilter_ShouldReturnFilteredProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByParams("code==TEST-PROD-001", null, null, false, false, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(product.getId(), result.getContent().get(0).getId());
    }

    /**
     * Test ID: PR-008
     * Test case: Tìm sản phẩm với search
     * Mục tiêu: Kiểm tra repository trả về đúng danh sách sản phẩm theo search
     */
    @Test
    void findByParams_WithSearch_ShouldReturnSearchedProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByParams(null, null, "Test Product", false, false, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(product.getId(), result.getContent().get(0).getId());
    }

    /**
     * Test ID: PR-009
     * Test case: Tìm sản phẩm theo slug tồn tại
     * Mục tiêu: Kiểm tra repository trả về đúng sản phẩm
     */
    @Test
    void findBySlug_WithExistingSlug_ShouldReturnProduct() {
        Optional<Product> result = productRepository.findBySlug("test-product-001");

        assertTrue(result.isPresent());
        assertEquals(product.getId(), result.get().getId());
    }

    /**
     * Test ID: PR-010
     * Test case: Tìm sản phẩm theo slug không tồn tại
     * Mục tiêu: Kiểm tra repository trả về empty
     */
    @Test
    void findBySlug_WithNonExistentSlug_ShouldReturnEmpty() {
        Optional<Product> result = productRepository.findBySlug("non-existent-slug");

        assertFalse(result.isPresent());
    }

    /**
     * Test ID: PR-011
     * Test case: Đếm số lượng sản phẩm
     * Mục tiêu: Kiểm tra repository trả về đúng số lượng sản phẩm
     */
    @Test
    void countByProductId_ShouldReturnCorrectCount() {
        int count = productRepository.countByProductId();

        assertEquals(1, count);
    }
} 