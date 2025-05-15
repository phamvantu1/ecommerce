package com.electro.service.inventory;

import com.electro.constant.ResourceName;
import com.electro.constant.SearchFields;
import com.electro.dto.ListResponse;
import com.electro.dto.order.OrderVariantRequest;
import com.electro.dto.order.OrderVariantResponse;
import com.electro.entity.address.Address;
import com.electro.entity.address.District;
import com.electro.entity.address.Province;
import com.electro.entity.address.Ward;
import com.electro.entity.authentication.User;
import com.electro.entity.cashbook.PaymentMethodType;
import com.electro.entity.customer.CustomerResource;
import com.electro.entity.order.Order;
import com.electro.entity.order.OrderResource;
import com.electro.entity.order.OrderVariant;
import com.electro.entity.order.OrderVariantKey;
import com.electro.entity.product.Product;
import com.electro.entity.product.Variant;
import com.electro.repository.address.AddressRepository;
import com.electro.repository.address.DistrictRepository;
import com.electro.repository.address.ProvinceRepository;
import com.electro.repository.address.WardRepository;
import com.electro.repository.authentication.UserRepository;
import com.electro.repository.customer.CustomerResourceRepository;
import com.electro.repository.order.OrderRepository;
import com.electro.repository.order.OrderResourceRepository;
import com.electro.repository.order.OrderVariantRepository;
import com.electro.repository.product.ProductRepository;
import com.electro.repository.product.VariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderVariantServiceImplTest {

    @Autowired
    private OrderVariantService orderVariantService;

    @Autowired
    private OrderVariantRepository orderVariantRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderResourceRepository orderResourceRepository;

    @Autowired
    private CustomerResourceRepository customerResourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VariantRepository variantRepository;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private AddressRepository addressRepository;

    private OrderVariant orderVariant;
    private Order order;
    private Variant variant;
    private OrderVariantKey orderVariantKey;

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

        // Tạo customer resource
        CustomerResource customerResource = new CustomerResource();
        customerResource.setName("Test Customer");
        customerResource.setCode("TEST-CUSTOMER-001");
        customerResource.setDescription("Test Customer Description");
        customerResource.setColor("#00FF00");
        customerResource.setStatus(1);
        customerResource = customerResourceRepository.save(customerResource);

        // Tạo order resource
        OrderResource orderResource = new OrderResource();
        orderResource.setName("Test Resource");
        orderResource.setCode("TEST-RESOURCE-001");
        orderResource.setColor("#FF0000");
        orderResource.setCustomerResource(customerResource);
        orderResource.setStatus(1);
        orderResource = orderResourceRepository.save(orderResource);

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

        // Tạo address
        Address address = new Address();
        address.setLine("123 Test Street");
        address.setProvince(province);
        address.setDistrict(district);
        address.setWard(ward);
        address = addressRepository.save(address);

        // Tạo user
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");
        user.setFullname("Test User");
        user.setEmail("test@example.com");
        user.setPhone("0123456789");
        user.setGender("M");
        user.setAddress(address);
        user.setStatus(1);
        user = userRepository.save(user);

        // Tạo order
        order = new Order();
        order.setCode("TEST-ORDER-001");
        order.setStatus(1);
        order.setToName("Test User");
        order.setToPhone("0123456789");
        order.setToAddress("123 Test Street");
        order.setToWardName("Test Ward");
        order.setToDistrictName("Test District");
        order.setToProvinceName("Test Province");
        order.setOrderResource(orderResource);
        order.setUser(user);
        order.setTotalAmount(new BigDecimal("1500.00"));
        order.setTax(new BigDecimal("150.00"));
        order.setShippingCost(new BigDecimal("50.00"));
        order.setTotalPay(new BigDecimal("1700.00"));
        order.setPaymentMethodType(PaymentMethodType.CASH);
        order.setPaymentStatus(1);
        order = orderRepository.save(order);

        // Tạo order variant
        orderVariantKey = new OrderVariantKey(order.getId(), variant.getId());
        orderVariant = new OrderVariant();
        orderVariant.setOrderVariantKey(orderVariantKey);
        orderVariant.setOrder(order);
        orderVariant.setVariant(variant);
        orderVariant.setQuantity(10);
        orderVariant = orderVariantRepository.save(orderVariant);
    }

    /**
     * Test ID: OV-SV-001
     * Test case: Tìm tất cả order variants với phân trang
     * Mục tiêu: Kiểm tra service trả về đúng danh sách order variants có phân trang
     */
    @Test
    void findAll_ShouldReturnPagedOrderVariants() {
        // Act
        ListResponse<OrderVariantResponse> result = orderVariantService.findAll(0, 10, "id,desc", null, null, false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(orderVariant.getQuantity(), result.getContent().get(0).getQuantity());
    }

    /**
     * Test ID: OV-SV-002
     * Test case: Tìm tất cả order variants với filter
     * Mục tiêu: Kiểm tra service trả về đúng danh sách order variants theo filter
     */
    @Test
    void findAll_WithFilter_ShouldReturnFilteredOrderVariants() {
        // Act
        ListResponse<OrderVariantResponse> result = orderVariantService.findAll(0, 10, "id,desc", 
            "quantity==10", null, false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(10, result.getContent().get(0).getQuantity());
    }

    /**
     * Test ID: OV-SV-003
     * Test case: Tìm tất cả order variants với search
     * Mục tiêu: Kiểm tra service trả về đúng danh sách order variants theo search
     */
    @Test
    void findAll_WithSearch_ShouldReturnSearchedOrderVariants() {
        // Act
        ListResponse<OrderVariantResponse> result = orderVariantService.findAll(0, 10, "id,desc", 
            null, "TEST-SKU-001", false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals("TEST-SKU-001", result.getContent().get(0).getVariant().getSku());
    }

    /**
     * Test ID: OV-SV-004
     * Test case: Tìm tất cả order variants với all=true
     * Mục tiêu: Kiểm tra service trả về tất cả order variants không phân trang
     */
    @Test
    void findAll_WithAllTrue_ShouldReturnAllOrderVariants() {
        // Act
        ListResponse<OrderVariantResponse> result = orderVariantService.findAll(0, 10, "id,desc", 
            null, null, true);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
    }

    /**
     * Test ID: OV-SV-005
     * Test case: Tìm order variant theo ID tồn tại
     * Mục tiêu: Kiểm tra service trả về đúng order variant
     */
    @Test
    void findById_WithExistingId_ShouldReturnOrderVariant() {
        // Act
        OrderVariantResponse result = orderVariantService.findById(orderVariantKey);

        // Assert
        assertNotNull(result);
        assertEquals(orderVariant.getQuantity(), result.getQuantity());
        assertEquals(orderVariant.getVariant().getId(), result.getVariant().getId());
    }

    /**
     * Test ID: OV-SV-006
     * Test case: Tìm order variant theo ID không tồn tại
     * Mục tiêu: Kiểm tra service ném ra ResourceNotFoundException
     */
    @Test
    void findById_WithNonExistentId_ShouldThrowException() {
        // Arrange
        OrderVariantKey nonExistentKey = new OrderVariantKey(999L, 999L);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderVariantService.findById(nonExistentKey));
    }

    /**
     * Test ID: OV-SV-007
     * Test case: Lưu order variant mới
     * Mục tiêu: Kiểm tra service lưu thành công order variant mới
     */
    @Test
    void save_WithNewOrderVariant_ShouldSaveSuccessfully() {
        // Arrange
        OrderVariantRequest request = new OrderVariantRequest();
        request.setVariantId(variant.getId());
        request.setQuantity(20);

        // Act
        OrderVariantResponse result = orderVariantService.save(request);

        // Assert
        assertNotNull(result);
        assertEquals(request.getQuantity(), result.getQuantity());
        assertEquals(request.getVariantId(), result.getVariant().getId());
    }

    /**
     * Test ID: OV-SV-008
     * Test case: Cập nhật order variant
     * Mục tiêu: Kiểm tra service cập nhật thành công order variant
     */
    @Test
    void save_WithExistingId_ShouldUpdateSuccessfully() {
        // Arrange
        OrderVariantRequest request = new OrderVariantRequest();
        request.setVariantId(variant.getId());
        request.setQuantity(30);

        // Act
        OrderVariantResponse result = orderVariantService.save(orderVariantKey, request);

        // Assert
        assertNotNull(result);
        assertEquals(request.getQuantity(), result.getQuantity());
        assertEquals(request.getVariantId(), result.getVariant().getId());
    }

    /**
     * Test ID: OV-SV-009
     * Test case: Cập nhật order variant với ID không tồn tại
     * Mục tiêu: Kiểm tra service ném ra ResourceNotFoundException
     */
    @Test
    void save_WithNonExistentId_ShouldThrowException() {
        // Arrange
        OrderVariantKey nonExistentKey = new OrderVariantKey(999L, 999L);
        OrderVariantRequest request = new OrderVariantRequest();
        request.setVariantId(variant.getId());
        request.setQuantity(30);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderVariantService.save(nonExistentKey, request));
    }

    /**
     * Test ID: OV-SV-010
     * Test case: Xóa order variant
     * Mục tiêu: Kiểm tra service xóa thành công order variant
     */
    @Test
    void delete_ShouldDeleteOrderVariant() {
        // Act
        orderVariantService.delete(orderVariantKey);

        // Assert
        assertFalse(orderVariantRepository.existsById(orderVariantKey));
    }

    /**
     * Test ID: OV-SV-011
     * Test case: Xóa nhiều order variants
     * Mục tiêu: Kiểm tra service xóa thành công nhiều order variants
     */
    @Test
    void delete_WithMultipleIds_ShouldDeleteAllOrderVariants() {
        // Arrange
        OrderVariantKey key2 = new OrderVariantKey(order.getId(), variant.getId());
        List<OrderVariantKey> ids = Arrays.asList(orderVariantKey, key2);

        // Act
        orderVariantService.delete(ids);

        // Assert
        ids.forEach(id -> assertFalse(orderVariantRepository.existsById(id)));
    }

    /**
     * Test ID: OV-SV-012
     * Test case: Xóa order variant với ID không tồn tại
     * Mục tiêu: Kiểm tra service không ném ra exception khi xóa ID không tồn tại
     */
    @Test
    void delete_WithNonExistentId_ShouldNotThrowException() {
        // Arrange
        OrderVariantKey nonExistentKey = new OrderVariantKey(999L, 999L);

        // Act & Assert
        assertDoesNotThrow(() -> orderVariantService.delete(nonExistentKey));
    }

    /**
     * Test ID: OV-SV-013
     * Test case: Xóa nhiều order variants với danh sách rỗng
     * Mục tiêu: Kiểm tra service không ném ra exception khi xóa danh sách rỗng
     */
    @Test
    void delete_WithEmptyList_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> orderVariantService.delete(Collections.emptyList()));
    }
} 