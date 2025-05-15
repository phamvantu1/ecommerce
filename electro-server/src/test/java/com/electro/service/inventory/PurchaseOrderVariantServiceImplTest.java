package com.electro.service.inventory;

import com.electro.dto.ListResponse;
import com.electro.dto.inventory.PurchaseOrderVariantRequest;
import com.electro.dto.inventory.PurchaseOrderVariantResponse;
import com.electro.entity.address.Address;
import com.electro.entity.address.District;
import com.electro.entity.address.Province;
import com.electro.entity.address.Ward;
import com.electro.entity.inventory.Destination;
import com.electro.entity.inventory.PurchaseOrder;
import com.electro.entity.inventory.PurchaseOrderVariant;
import com.electro.entity.inventory.PurchaseOrderVariantKey;
import com.electro.entity.product.Product;
import com.electro.entity.product.Supplier;
import com.electro.entity.product.Variant;
import com.electro.repository.address.AddressRepository;
import com.electro.repository.address.DistrictRepository;
import com.electro.repository.address.ProvinceRepository;
import com.electro.repository.address.WardRepository;
import com.electro.repository.inventory.DestinationRepository;
import com.electro.repository.inventory.PurchaseOrderRepository;
import com.electro.repository.inventory.PurchaseOrderVariantRepository;
import com.electro.repository.product.ProductRepository;
import com.electro.repository.product.SupplierRepository;
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
class PurchaseOrderVariantServiceImplTest {
    @Autowired
    private PurchaseOrderVariantService purchaseOrderVariantService;
    @Autowired
    private PurchaseOrderVariantRepository purchaseOrderVariantRepository;
    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private VariantRepository variantRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private DestinationRepository destinationRepository;
    @Autowired
    private ProvinceRepository provinceRepository;
    @Autowired
    private DistrictRepository districtRepository;
    @Autowired
    private WardRepository wardRepository;
    @Autowired
    private AddressRepository addressRepository;

    private PurchaseOrderVariant purchaseOrderVariant;
    private PurchaseOrder purchaseOrder;
    private Variant variant;
    private PurchaseOrderVariantKey purchaseOrderVariantKey;

    @BeforeEach
    void setUp() {
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

        // Tạo province
        Province province = new Province();
        province.setName("Test Province");
        province.setCode("TEST-PROV-001");
        province = provinceRepository.save(province);

        // Tạo district
        District district = new District();
        district.setName("Test District");
        district.setCode("TEST-DIST-001");
        district.setProvince(province);
        district = districtRepository.save(district);

        // Tạo ward
        Ward ward = new Ward();
        ward.setName("Test Ward");
        ward.setCode("TEST-WARD-001");
        ward.setDistrict(district);
        ward = wardRepository.save(ward);

        // Tạo address cho supplier
        Address supplierAddress = new Address();
        supplierAddress.setLine("123 Supplier Street");
        supplierAddress.setProvince(province);
        supplierAddress.setDistrict(district);
        supplierAddress.setWard(ward);
        supplierAddress = addressRepository.save(supplierAddress);

        // Tạo address cho destination
        Address destinationAddress = new Address();
        destinationAddress.setLine("456 Destination Street");
        destinationAddress.setProvince(province);
        destinationAddress.setDistrict(district);
        destinationAddress.setWard(ward);
        destinationAddress = addressRepository.save(destinationAddress);

        // Tạo supplier
        Supplier supplier = new Supplier();
        supplier.setDisplayName("Test Supplier");
        supplier.setCode("TEST-SUP-001");
        supplier.setEmail("supplier@test.com");
        supplier.setPhone("0123456789");
        supplier.setAddress(supplierAddress);
        supplier.setStatus(1);
        supplier = supplierRepository.save(supplier);

        // Tạo destination
        Destination destination = new Destination();
        destination.setContactFullname("Test Contact");
        destination.setContactEmail("contact@test.com");
        destination.setContactPhone("0987654321");
        destination.setAddress(destinationAddress);
        destination.setStatus(1);
        destination = destinationRepository.save(destination);

        // Tạo purchase order
        purchaseOrder = new PurchaseOrder();
        purchaseOrder.setCode("PO-001");
        purchaseOrder.setSupplier(supplier);
        purchaseOrder.setDestination(destination);
        purchaseOrder.setTotalAmount(1000.0);
        purchaseOrder.setStatus(1);
        purchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        // Tạo purchase order variant
        purchaseOrderVariantKey = new PurchaseOrderVariantKey(purchaseOrder.getId(), variant.getId());
        purchaseOrderVariant = new PurchaseOrderVariant();
        purchaseOrderVariant.setPurchaseOrderVariantKey(purchaseOrderVariantKey);
        purchaseOrderVariant.setPurchaseOrder(purchaseOrder);
        purchaseOrderVariant.setVariant(variant);
        purchaseOrderVariant.setQuantity(10);
        purchaseOrderVariant = purchaseOrderVariantRepository.save(purchaseOrderVariant);
    }

    /**
     * Test ID: POV-SV-001
     * Test case: Tìm tất cả purchase order variants với phân trang
     * Mục tiêu: Kiểm tra service trả về đúng danh sách purchase order variants có phân trang
     */
    @Test
    void findAll_ShouldReturnPagedPurchaseOrderVariants() {
        ListResponse<PurchaseOrderVariantResponse> result = purchaseOrderVariantService.findAll(0, 10, "id,desc", null, null, false);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(purchaseOrderVariant.getQuantity(), result.getContent().get(0).getQuantity());
    }

    /**
     * Test ID: POV-SV-002
     * Test case: Tìm tất cả purchase order variants với filter
     * Mục tiêu: Kiểm tra service trả về đúng danh sách purchase order variants theo filter
     */
    @Test
    void findAll_WithFilter_ShouldReturnFilteredPurchaseOrderVariants() {
        ListResponse<PurchaseOrderVariantResponse> result = purchaseOrderVariantService.findAll(0, 10, "id,desc", "quantity==10", null, false);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(10, result.getContent().get(0).getQuantity());
    }

    /**
     * Test ID: POV-SV-003
     * Test case: Tìm tất cả purchase order variants với search
     * Mục tiêu: Kiểm tra service trả về đúng danh sách purchase order variants theo search
     */
    @Test
    void findAll_WithSearch_ShouldReturnSearchedPurchaseOrderVariants() {
        ListResponse<PurchaseOrderVariantResponse> result = purchaseOrderVariantService.findAll(0, 10, "id,desc", null, "TEST-SKU-001", false);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals("TEST-SKU-001", result.getContent().get(0).getVariant().getSku());
    }

    /**
     * Test ID: POV-SV-004
     * Test case: Tìm tất cả purchase order variants với all=true
     * Mục tiêu: Kiểm tra service trả về tất cả purchase order variants không phân trang
     */
    @Test
    void findAll_WithAllTrue_ShouldReturnAllPurchaseOrderVariants() {
        ListResponse<PurchaseOrderVariantResponse> result = purchaseOrderVariantService.findAll(0, 10, "id,desc", null, null, true);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
    }

    /**
     * Test ID: POV-SV-005
     * Test case: Tìm purchase order variant theo ID tồn tại
     * Mục tiêu: Kiểm tra service trả về đúng purchase order variant
     */
    @Test
    void findById_WithExistingId_ShouldReturnPurchaseOrderVariant() {
        PurchaseOrderVariantResponse result = purchaseOrderVariantService.findById(purchaseOrderVariantKey);
        assertNotNull(result);
        assertEquals(purchaseOrderVariant.getQuantity(), result.getQuantity());
        assertEquals(purchaseOrderVariant.getVariant().getId(), result.getVariant().getId());
    }

    /**
     * Test ID: POV-SV-006
     * Test case: Tìm purchase order variant theo ID không tồn tại
     * Mục tiêu: Kiểm tra service ném ra ResourceNotFoundException
     */
    @Test
    void findById_WithNonExistentId_ShouldThrowException() {
        PurchaseOrderVariantKey nonExistentKey = new PurchaseOrderVariantKey(999L, 999L);
        assertThrows(RuntimeException.class, () -> purchaseOrderVariantService.findById(nonExistentKey));
    }

    /**
     * Test ID: POV-SV-007
     * Test case: Lưu purchase order variant mới
     * Mục tiêu: Kiểm tra service lưu thành công purchase order variant mới
     */
    @Test
    void save_WithNewPurchaseOrderVariant_ShouldSaveSuccessfully() {
        PurchaseOrderVariantRequest request = new PurchaseOrderVariantRequest();
        request.setVariantId(variant.getId());
        request.setQuantity(20);
        PurchaseOrderVariantResponse result = purchaseOrderVariantService.save(request);
        assertNotNull(result);
        assertEquals(request.getQuantity(), result.getQuantity());
        assertEquals(request.getVariantId(), result.getVariant().getId());
    }

    /**
     * Test ID: POV-SV-008
     * Test case: Cập nhật purchase order variant
     * Mục tiêu: Kiểm tra service cập nhật thành công purchase order variant
     */
    @Test
    void save_WithExistingId_ShouldUpdateSuccessfully() {
        PurchaseOrderVariantRequest request = new PurchaseOrderVariantRequest();
        request.setVariantId(variant.getId());
        request.setQuantity(30);
        PurchaseOrderVariantResponse result = purchaseOrderVariantService.save(purchaseOrderVariantKey, request);
        assertNotNull(result);
        assertEquals(request.getQuantity(), result.getQuantity());
        assertEquals(request.getVariantId(), result.getVariant().getId());
    }

    /**
     * Test ID: POV-SV-009
     * Test case: Cập nhật purchase order variant với ID không tồn tại
     * Mục tiêu: Kiểm tra service ném ra ResourceNotFoundException
     */
    @Test
    void save_WithNonExistentId_ShouldThrowException() {
        PurchaseOrderVariantKey nonExistentKey = new PurchaseOrderVariantKey(999L, 999L);
        PurchaseOrderVariantRequest request = new PurchaseOrderVariantRequest();
        request.setVariantId(variant.getId());
        request.setQuantity(30);
        assertThrows(RuntimeException.class, () -> purchaseOrderVariantService.save(nonExistentKey, request));
    }

    /**
     * Test ID: POV-SV-010
     * Test case: Xóa purchase order variant
     * Mục tiêu: Kiểm tra service xóa thành công purchase order variant
     */
    @Test
    void delete_ShouldDeletePurchaseOrderVariant() {
        purchaseOrderVariantService.delete(purchaseOrderVariantKey);
        assertFalse(purchaseOrderVariantRepository.existsById(purchaseOrderVariantKey));
    }

    /**
     * Test ID: POV-SV-011
     * Test case: Xóa nhiều purchase order variants
     * Mục tiêu: Kiểm tra service xóa thành công nhiều purchase order variants
     */
    @Test
    void delete_WithMultipleIds_ShouldDeleteAllPurchaseOrderVariants() {
        PurchaseOrderVariantKey key2 = new PurchaseOrderVariantKey(purchaseOrder.getId(), variant.getId());
        List<PurchaseOrderVariantKey> ids = Arrays.asList(purchaseOrderVariantKey, key2);
        purchaseOrderVariantService.delete(ids);
        ids.forEach(id -> assertFalse(purchaseOrderVariantRepository.existsById(id)));
    }

    /**
     * Test ID: POV-SV-012
     * Test case: Xóa purchase order variant với ID không tồn tại
     * Mục tiêu: Kiểm tra service không ném ra exception khi xóa ID không tồn tại
     */
    @Test
    void delete_WithNonExistentId_ShouldNotThrowException() {
        PurchaseOrderVariantKey nonExistentKey = new PurchaseOrderVariantKey(999L, 999L);
        assertDoesNotThrow(() -> purchaseOrderVariantService.delete(nonExistentKey));
    }

    /**
     * Test ID: POV-SV-013
     * Test case: Xóa nhiều purchase order variants với danh sách rỗng
     * Mục tiêu: Kiểm tra service không ném ra exception khi xóa danh sách rỗng
     */
    @Test
    void delete_WithEmptyList_ShouldNotThrowException() {
        assertDoesNotThrow(() -> purchaseOrderVariantService.delete(Collections.emptyList()));
    }
} 