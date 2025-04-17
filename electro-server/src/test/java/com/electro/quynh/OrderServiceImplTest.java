package com.electro.quynh;

import com.electro.config.payment.paypal.PayPalHttpClient;
import com.electro.dto.client.ClientConfirmedOrderResponse;
import com.electro.dto.client.ClientSimpleOrderRequest;
import com.electro.dto.payment.OrderStatus;
import com.electro.dto.payment.PaypalRequest;
import com.electro.dto.payment.PaypalResponse;
import com.electro.dto.waybill.GhnCancelOrderRequest;
import com.electro.dto.waybill.GhnCancelOrderResponse;
import com.electro.entity.address.Address;
import com.electro.entity.address.District;
import com.electro.entity.address.Province;
import com.electro.entity.address.Ward;
import com.electro.entity.authentication.User;
import com.electro.entity.cart.Cart;
import com.electro.entity.cart.CartVariant;
import com.electro.entity.cashbook.PaymentMethodType;
import com.electro.entity.general.Notification;
import com.electro.entity.order.Order;
import com.electro.entity.order.OrderResource;
import com.electro.entity.order.OrderVariant;
import com.electro.entity.product.Product;
import com.electro.entity.product.Variant;
import com.electro.entity.promotion.Promotion;
import com.electro.entity.waybill.Waybill;
import com.electro.entity.waybill.WaybillLog;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.client.ClientOrderMapper;
import com.electro.mapper.general.NotificationMapper;
import com.electro.repository.authentication.UserRepository;
import com.electro.repository.cart.CartRepository;
import com.electro.repository.general.NotificationRepository;
import com.electro.repository.order.OrderRepository;
import com.electro.repository.promotion.PromotionRepository;
import com.electro.repository.waybill.WaybillLogRepository;
import com.electro.repository.waybill.WaybillRepository;
import com.electro.service.auth.VerificationService;
import com.electro.service.general.NotificationService;
import com.electro.service.order.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho OrderServiceImpl, kiểm tra các chức năng hủy đơn hàng, tạo đơn hàng và capture thanh toán PayPal.
 * Sử dụng Mockito để giả lập repository, service và HTTP client.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private VerificationService verificationService;

    @Mock
    private WaybillRepository waybillRepository;

    @Mock
    private WaybillLogRepository waybillLogRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PayPalHttpClient payPalHttpClient;

    @Mock
    private ClientOrderMapper clientOrderMapper;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationMapper notificationMapper;

    private User user;
    private Cart cart;
    private CartVariant cartVariant;
    private final String orderCode = "ORDER123";
    private final Long orderId = 100L;
    private final String username = "testuser";

    @BeforeEach
    void setup() {
        // Mock user và address
        user = mockUser();

        // Mock variant và product
        Variant variant = mockVariant(1L);

        // CartVariant
        cartVariant = new CartVariant();
        cartVariant.setVariant(variant);
        cartVariant.setQuantity(2);

        // Cart
        cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);
        cart.setStatus(1);
        cart.setCartVariants(Set.of(cartVariant));

        // Liên kết ngược CartVariant với Cart
        cartVariant.setCart(cart);

        // Thiết lập các giá trị cấu hình
        ReflectionTestUtils.setField(orderServiceImpl, "ghnApiPath", "https://fake-ghn-api.com");
        ReflectionTestUtils.setField(orderServiceImpl, "ghnToken", "fake-token");
        ReflectionTestUtils.setField(orderServiceImpl, "ghnShopId", "123456");
    }

    private User mockUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setFullname("Test User");
        user.setPhone("0123456789");
        user.setAddress(mockAddress());
        user.setStatus(1); // Trạng thái hoạt động
        return user;
    }

    private Address mockAddress() {
        Address address = new Address();
        address.setLine("123 Test St");

        Province province = new Province();
        province.setName("Hanoi");

        District district = new District();
        district.setName("Ba Dinh");

        Ward ward = new Ward();
        ward.setName("Lien Quan");
        address.setProvince(province);
        address.setDistrict(district);
        address.setWard(ward);
        return address;
    }

    private Cart mockCartWithVariants() {
        Cart cart = new Cart();
        CartVariant variant = new CartVariant();
        variant.setVariant(mockVariant(1L));
        variant.setQuantity(2);
        cart.setCartVariants(Set.of(variant));
        return cart;
    }

    private Cart mockCartWithMultipleVariants() {
        Cart cart = new Cart();
        CartVariant v1 = new CartVariant();
        v1.setVariant(mockVariant(1L));
        v1.setQuantity(1);

        CartVariant v2 = new CartVariant();
        v2.setVariant(mockVariant(2L));
        v2.setQuantity(2);

        cart.setCartVariants(Set.of(v1, v2));
        return cart;
    }

    private Variant mockVariant(Long id) {
        Variant variant = new Variant();
        variant.setId(id);
        variant.setPrice(100000.0);
        Product product = new Product();
        product.setId(id);
        variant.setProduct(product);
        return variant;
    }

    private void mockAuthentication(String username) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // ✅ Tạo đơn hàng thành công với hình thức thanh toán bằng tiền mặt (CASH)
    @Test
    void createClientOrder_Testcase1() {
        // Set security context mock
        mockAuthentication("testuser");

        // Prepare request
        ClientSimpleOrderRequest request = new ClientSimpleOrderRequest();
        request.setPaymentMethodType(PaymentMethodType.CASH);

        // Mock dependencies
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(cartRepository.findByUsername(anyString())).thenReturn(Optional.of(cart));
        when(promotionRepository.findActivePromotionByProductId(anyLong())).thenReturn(Collections.emptyList());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call method under test
        try {
            ClientConfirmedOrderResponse result = orderServiceImpl.createClientOrder(request);

            // Assertions
            assertNotNull(result);
            assertEquals(PaymentMethodType.CASH, result.getOrderPaymentMethodType());
            assertNotNull(result.getOrderCode());
            assertTrue(result.getOrderCode().length() > 0);
            assertNull(result.getOrderPaypalCheckoutLink());

            System.out.println("✅ Đã tạo đơn hàng thành công với mã: " + result.getOrderCode());
        } catch (Exception e) {
            fail("Không nên throw exception: " + e.getMessage());
        }

        // Kiểm tra DB
        verify(orderRepository).save(any(Order.class));
        verify(cartRepository).save(any(Cart.class));
    }

    // ✅ Tạo đơn hàng thành công với hình thức thanh toán bằng PayPal
    @Test
    void createClientOrder_Testcase2() throws Exception {
        // Set security context mock
        mockAuthentication("testuser");

        // Prepare request
        ClientSimpleOrderRequest request = new ClientSimpleOrderRequest();
        request.setPaymentMethodType(PaymentMethodType.PAYPAL);

        // Mock PayPal response
        PaypalResponse paypalResponse = new PaypalResponse();
        paypalResponse.setId("PAYPAL123");
        paypalResponse.setStatus(OrderStatus.CREATED);
        PaypalResponse.Link link = new PaypalResponse.Link();
        link.setRel("approve");
        link.setHref("http://paypal.com/checkout");
        paypalResponse.setLinks(List.of(link));

        // Mock dependencies
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(cartRepository.findByUsername(anyString())).thenReturn(Optional.of(cart));
        when(promotionRepository.findActivePromotionByProductId(anyLong())).thenReturn(Collections.emptyList());
        when(payPalHttpClient.createPaypalTransaction(any(PaypalRequest.class))).thenReturn(paypalResponse);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call method under test
        try {
            ClientConfirmedOrderResponse result = orderServiceImpl.createClientOrder(request);

            // Assertions
            assertNotNull(result);
            assertEquals(PaymentMethodType.PAYPAL, result.getOrderPaymentMethodType());
            assertNotNull(result.getOrderCode());
            assertTrue(result.getOrderCode().length() > 0);
            assertEquals("http://paypal.com/checkout", result.getOrderPaypalCheckoutLink());

            System.out.println("✅ Đã tạo đơn hàng PayPal thành công với mã: " + result.getOrderCode());
        } catch (Exception e) {
            fail("Không nên throw exception: " + e.getMessage());
        }

        // Kiểm tra DB
        verify(orderRepository).save(any(Order.class));
        verify(cartRepository).save(any(Cart.class));
    }

    // ❌ Người dùng không tồn tại trong hệ thống
    @Test
    void createClientOrder_Testcase3() {
        // Set security context mock
        mockAuthentication("nonexistentuser");

        // Prepare request
        ClientSimpleOrderRequest request = new ClientSimpleOrderRequest();
        request.setPaymentMethodType(PaymentMethodType.CASH);

        // Mock dependencies
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // Call method under test
        assertThrows(UsernameNotFoundException.class, () -> orderServiceImpl.createClientOrder(request));

        // Kiểm tra DB
        verify(userRepository).findByUsername("nonexistentuser");
        verify(cartRepository, never()).findByUsername(any());
        verify(orderRepository, never()).save(any());
    }

    // ❌ Giỏ hàng trống
    @Test
    void createClientOrder_Testcase4() {
        // Set security context mock
        mockAuthentication("testuser");

        // Prepare empty cart
        Cart emptyCart = new Cart();
        emptyCart.setCartVariants(Collections.emptySet());

        // Prepare request
        ClientSimpleOrderRequest request = new ClientSimpleOrderRequest();
        request.setPaymentMethodType(PaymentMethodType.CASH);

        // Mock dependencies
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(emptyCart));

        // Call method under test
        assertThrows(RuntimeException.class, () -> orderServiceImpl.createClientOrder(request));

        // Kiểm tra DB
        verify(userRepository).findByUsername("testuser");
        verify(cartRepository).findByUsername("testuser");
        verify(orderRepository, never()).save(any());
        verify(cartRepository, never()).save(any());
    }

    // ❌ Không thể xác định phương thức thanh toán
    @Test
    void createClientOrder_Testcase5() {
        // Set security context mock
        mockAuthentication("testuser");

        // Prepare request with invalid payment method
        ClientSimpleOrderRequest request = new ClientSimpleOrderRequest();
        request.setPaymentMethodType(null);

        // Mock dependencies
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(cartRepository.findByUsername(anyString())).thenReturn(Optional.of(cart));

        // Call method under test
        assertThrows(RuntimeException.class, () -> orderServiceImpl.createClientOrder(request));

        // Kiểm tra DB
        verify(userRepository).findByUsername("testuser");
        verify(cartRepository).findByUsername("testuser");
        verify(orderRepository, never()).save(any());
        verify(cartRepository, never()).save(any());
    }

    // ❌ Đặt hàng khi không đăng nhập
    @Test
    void createClientOrder_Testcase6() {
        // Clear security context
        SecurityContextHolder.clearContext();

        // Prepare request
        ClientSimpleOrderRequest request = new ClientSimpleOrderRequest();
        request.setPaymentMethodType(PaymentMethodType.CASH);

        // Call method under test
        Exception exception = assertThrows(Exception.class, () -> orderServiceImpl.createClientOrder(request));
        assertTrue(exception.getMessage().contains("Authentication"));

        // Kiểm tra DB
        verify(userRepository, never()).findByUsername(any());
        verify(cartRepository, never()).findByUsername(any());
        verify(orderRepository, never()).save(any());
    }

    // ❌ Đặt hàng khi địa chỉ giao hàng bị trống
    @Test
    void createClientOrder_Testcase7() {
        // Set security context mock
        mockAuthentication("testuser");

        // Prepare user with null address
        User userNoAddress = mockUser();
        userNoAddress.setAddress(null);

        // Prepare request
        ClientSimpleOrderRequest request = new ClientSimpleOrderRequest();
        request.setPaymentMethodType(PaymentMethodType.CASH);

        // Mock dependencies
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(userNoAddress));
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart));

        // Call method under test
        assertThrows(NullPointerException.class, () -> orderServiceImpl.createClientOrder(request));

        // Kiểm tra DB
        verify(userRepository).findByUsername("testuser");
        verify(cartRepository).findByUsername("testuser");
        verify(orderRepository, never()).save(any());
    }

    // ❌ Không tìm thấy giỏ hàng của người dùng
    @Test
    void createClientOrder_Testcase8() {
        // Set security context mock
        mockAuthentication("testuser");

        // Prepare request
        ClientSimpleOrderRequest request = new ClientSimpleOrderRequest();
        request.setPaymentMethodType(PaymentMethodType.CASH);

        // Mock dependencies
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(cartRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // Call method under test
        assertThrows(ResourceNotFoundException.class, () -> orderServiceImpl.createClientOrder(request));

        // Kiểm tra DB
        verify(userRepository).findByUsername("testuser");
        verify(cartRepository).findByUsername("testuser");
        verify(orderRepository, never()).save(any());
    }

    // ✅ Đặt hàng với nhiều sản phẩm cùng lúc
    @Test
    void createClientOrder_Testcase9() {
        // Set security context mock
        mockAuthentication("testuser");

        // Prepare cart with multiple variants
        Cart multiVariantCart = mockCartWithMultipleVariants();

        // Prepare request
        ClientSimpleOrderRequest request = new ClientSimpleOrderRequest();
        request.setPaymentMethodType(PaymentMethodType.CASH);

        // Mock dependencies
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(multiVariantCart));
        when(promotionRepository.findActivePromotionByProductId(anyLong())).thenReturn(Collections.emptyList());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call method under test
        try {
            ClientConfirmedOrderResponse response = orderServiceImpl.createClientOrder(request);

            // Assertions
            assertNotNull(response);
            assertNotNull(response.getOrderCode());
            assertEquals(PaymentMethodType.CASH, response.getOrderPaymentMethodType());

            System.out.println("✅ Đã tạo đơn hàng với nhiều sản phẩm thành công với mã: " + response.getOrderCode());
        } catch (Exception e) {
            fail("Không nên throw exception: " + e.getMessage());
        }

        // Kiểm tra DB
        verify(orderRepository).save(any(Order.class));
        verify(cartRepository).save(any(Cart.class));
    }

    // ✅ Đặt hàng khi có khuyến mãi
    @Test
    void createClientOrder_Testcase10() {
        // Set security context mock
        mockAuthentication("testuser");

        // Prepare promotion
        Promotion promotion = new Promotion();
        promotion.setPercent(20);

        // Prepare request
        ClientSimpleOrderRequest request = new ClientSimpleOrderRequest();
        request.setPaymentMethodType(PaymentMethodType.CASH);

        // Mock dependencies
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart));
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(List.of(promotion));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.getOrderVariants().forEach(v -> v.setOrder(order));
            return order;
        });
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call method under test
        try {
            ClientConfirmedOrderResponse response = orderServiceImpl.createClientOrder(request);

            // Assertions
            assertNotNull(response);
            assertNotNull(response.getOrderCode());
            assertEquals(PaymentMethodType.CASH, response.getOrderPaymentMethodType());

            System.out.println("✅ Đã tạo đơn hàng với khuyến mãi thành công với mã: " + response.getOrderCode());
        } catch (Exception e) {
            fail("Không nên throw exception: " + e.getMessage());
        }

        // Kiểm tra DB
        verify(orderRepository).save(argThat(order ->
                order.getTotalAmount().equals(BigDecimal.valueOf(160000.0)) // 100000 * 0.8 * 2
        ));
        verify(cartRepository).save(any(Cart.class));
    }

    // ❌ Đặt hàng khi sản phẩm không còn tồn tại (variant bị null)
    @Test
    void createClientOrder_Testcase11() {
        // Set security context mock
        mockAuthentication("testuser");

        // Prepare cart with null variant
        Cart invalidCart = new Cart();
        CartVariant invalidVariant = new CartVariant();
        invalidVariant.setVariant(null);
        invalidVariant.setQuantity(2);
        invalidCart.setCartVariants(Set.of(invalidVariant));

        // Prepare request
        ClientSimpleOrderRequest request = new ClientSimpleOrderRequest();
        request.setPaymentMethodType(PaymentMethodType.CASH);

        // Mock dependencies
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(invalidCart));

        // Call method under test
        assertThrows(NullPointerException.class, () -> orderServiceImpl.createClientOrder(request));

        // Kiểm tra DB
        verify(userRepository).findByUsername("testuser");
        verify(cartRepository).findByUsername("testuser");
        verify(orderRepository, never()).save(any());
        verify(cartRepository, never()).save(any());
    }

    // ❌ Đặt hàng khi người dùng bị vô hiệu hóa / không hoạt động
    @Test
    void createClientOrder_Testcase12() {
        // Set security context mock
        mockAuthentication("testuser");

        // Prepare inactive user
        User inactiveUser = mockUser();
        inactiveUser.setStatus(2); // Inactive

        // Prepare request
        ClientSimpleOrderRequest request = new ClientSimpleOrderRequest();
        request.setPaymentMethodType(PaymentMethodType.CASH);

        // Mock dependencies
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(inactiveUser));
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart));

        // Call method under test
        ClientConfirmedOrderResponse response = orderServiceImpl.createClientOrder(request);

        // Assertions
        assertNotNull(response);
        assertNotNull(response.getOrderCode());
        System.out.println("⚠️ Đã tạo đơn hàng cho người dùng không hoạt động với mã: " + response.getOrderCode());

        // Kiểm tra DB
        verify(orderRepository).save(any(Order.class));
        verify(cartRepository).save(any(Cart.class));
    }

    // ✅ Hủy đơn hàng không có vận đơn
    @Test
    void testCancelOrder_HuyDonKhongCoVanDon() {
        // Prepare data
        Order order = new Order();
        order.setStatus(2);
        order.setId(orderId);

        // Mock dependencies
        when(orderRepository.findByCode(orderCode)).thenReturn(Optional.of(order));
        when(waybillRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call method under test
        orderServiceImpl.cancelOrder(orderCode);

        // Assertions
        assertEquals(5, order.getStatus());
        System.out.println("✅ Đã hủy đơn hàng không có vận đơn với mã: " + orderCode);

        // Kiểm tra DB
        verify(orderRepository).save(order);
        verify(waybillRepository, never()).save(any());
        verify(waybillLogRepository, never()).save(any());
    }

    // ❌ Không tìm thấy đơn hàng khi hủy
    @Test
    void testCancelOrder_KhongTimThayDonHang() {
        // Mock dependencies
        when(orderRepository.findByCode(orderCode)).thenReturn(Optional.empty());

        // Call method under test
        assertThrows(ResourceNotFoundException.class, () -> orderServiceImpl.cancelOrder(orderCode));

        // Kiểm tra DB
        verify(orderRepository).findByCode(orderCode);
        verify(orderRepository, never()).save(any());
        verify(waybillRepository, never()).findByOrderId(any());
    }

    // ❌ Đơn hàng đã giao hoặc đã hủy
    @Test
    void testCancelOrder_DonHangDaGiaoHoacHuy() {
        // Prepare data
        Order order = new Order();
        order.setStatus(3);

        // Mock dependencies
        when(orderRepository.findByCode(orderCode)).thenReturn(Optional.of(order));

        // Call method under test
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderServiceImpl.cancelOrder(orderCode));

        // Assertions
        assertTrue(exception.getMessage().contains("has been cancelled"));

        // Kiểm tra DB
        verify(orderRepository).findByCode(orderCode);
        verify(orderRepository, never()).save(any());
        verify(waybillRepository, never()).findByOrderId(any());
    }

    // ✅ Hủy đơn hàng có vận đơn
    @Test
    void testCancelOrder_HuyDonCoVanDon() {
        // Prepare data
        Order order = new Order();
        order.setStatus(1);
        order.setId(orderId);

        Waybill waybill = new Waybill();
        waybill.setId(1L);
        waybill.setOrder(order);
        waybill.setStatus(1);
        waybill.setCode("WAYBILL123");

        GhnCancelOrderResponse.Data$ data = new GhnCancelOrderResponse.Data$();
        data.setResult(true);
        GhnCancelOrderResponse response = new GhnCancelOrderResponse();
        response.setData(List.of(data));

        // Mock dependencies
        when(orderRepository.findByCode(orderCode)).thenReturn(Optional.of(order));
        when(waybillRepository.findByOrderId(orderId)).thenReturn(Optional.of(waybill));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(GhnCancelOrderResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
        when(waybillLogRepository.save(any(WaybillLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(waybillRepository.save(any(Waybill.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call method under test
        orderServiceImpl.cancelOrder(orderCode);

        // Assertions
        assertEquals(5, order.getStatus());
        assertEquals(4, waybill.getStatus());
        System.out.println("✅ Đã hủy đơn hàng có vận đơn với mã: " + orderCode);

        // Kiểm tra DB
        verify(orderRepository).save(order);
        verify(waybillRepository).save(waybill);
        verify(waybillLogRepository).save(any(WaybillLog.class));
    }

    // ❌ Hủy đơn hàng khi API GHN thất bại
    @Test
    void testCancelOrder_GhnApiThatBai() {
        // Prepare data
        Order order = new Order();
        order.setStatus(1);
        order.setId(orderId);

        Waybill waybill = new Waybill();
        waybill.setId(1L);
        waybill.setOrder(order);
        waybill.setStatus(1);
        waybill.setCode("WAYBILL123");

        // Mock dependencies
        when(orderRepository.findByCode(orderCode)).thenReturn(Optional.of(order));
        when(waybillRepository.findByOrderId(orderId)).thenReturn(Optional.of(waybill));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(GhnCancelOrderResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));

        // Call method under test
        assertThrows(RuntimeException.class, () -> orderServiceImpl.cancelOrder(orderCode));

        // Kiểm tra DB
        verify(orderRepository).findByCode(orderCode);
        verify(waybillRepository).findByOrderId(orderId);
        verify(orderRepository, never()).save(any());
        verify(waybillRepository, never()).save(any());
        verify(waybillLogRepository, never()).save(any());
    }

    // ✅ Capture thanh toán PayPal thành công
    @Test
    void captureTransactionPaypal_Testcase1() throws Exception {
        // Prepare data
        String paypalOrderId = "PAYPAL123";
        String payerId = "PAYER456";

        User user = mockUser();
        Order order = new Order();
        order.setPaypalOrderId(paypalOrderId);
        order.setUser(user);
        order.setCode("ORDER-CODE-001");

        Notification notification = new Notification();

        // Mock dependencies
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.of(order));
        doNothing().when(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.entityToResponse(any(Notification.class))).thenReturn(null);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call method under test
        try {
            orderServiceImpl.captureTransactionPaypal(paypalOrderId, payerId);

            // Assertions
            assertEquals(OrderStatus.COMPLETED.toString(), order.getPaypalOrderStatus());
            assertEquals(2, order.getPaymentStatus());

            System.out.println("✅ Đã capture thanh toán PayPal thành công cho đơn hàng: " + order.getCode());
        } catch (Exception e) {
            fail("Không nên throw exception: " + e.getMessage());
        }

        // Kiểm tra DB
        verify(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId);
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationService).pushNotification(eq(user.getUsername()), any());
    }

    // ❌ Capture thanh toán PayPal khi không tìm thấy đơn hàng
    @Test
    void captureTransactionPaypal_Testcase2() throws Exception {
        // Prepare data
        String paypalOrderId = "PAYPAL_NOT_FOUND";
        String payerId = "PAYER123";

        // Mock dependencies
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.empty());

        // Call method under test
        assertThrows(ResourceNotFoundException.class,
                () -> orderServiceImpl.captureTransactionPaypal(paypalOrderId, payerId));

        // Kiểm tra DB
        verify(orderRepository).findByPaypalOrderId(paypalOrderId);
        verify(payPalHttpClient, never()).capturePaypalTransaction(any(), any());
        verify(notificationRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    // ❌ Capture thanh toán PayPal khi API PayPal thất bại
    @Test
    void captureTransactionPaypal_Testcase3() throws Exception {
        // Prepare data
        String paypalOrderId = "PAYPAL123";
        String payerId = "PAYER456";

        User user = mockUser();
        Order order = new Order();
        order.setPaypalOrderId(paypalOrderId);
        order.setUser(user);
        order.setCode("ORDER-CODE-001");

        // Mock dependencies
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.of(order));
        doThrow(new RuntimeException("API failure")).when(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call method under test
        orderServiceImpl.captureTransactionPaypal(paypalOrderId, payerId);

        // Assertions
        assertEquals(OrderStatus.APPROVED.toString(), order.getPaypalOrderStatus());

        // Kiểm tra DB
        verify(orderRepository).findByPaypalOrderId(paypalOrderId);
        verify(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId);
        verify(notificationRepository, never()).save(any());
        verify(orderRepository).save(order);
    }

    // ❌ Capture thanh toán PayPal khi đơn hàng đã hoàn tất
    @Test
    void captureTransactionPaypal_Testcase4() throws Exception {
        // Prepare data
        String paypalOrderId = "PAYPAL_COMPLETED";
        String payerId = "PAYER789";

        User user = mockUser();
        Order order = new Order();
        order.setPaypalOrderId(paypalOrderId);
        order.setUser(user);
        order.setCode("ORDER-CODE-002");
        order.setPaypalOrderStatus(OrderStatus.COMPLETED.toString());

        // Mock dependencies
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.of(order));
        doNothing().when(payPalHttpClient).capturePaypalTransaction(any(), any());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call method under test
        orderServiceImpl.captureTransactionPaypal(paypalOrderId, payerId);

        // Assertions
        assertEquals(OrderStatus.COMPLETED.toString(), order.getPaypalOrderStatus());
        System.out.println("⚠️ Capture đơn hàng đã hoàn tất: " + order.getCode());

        // Kiểm tra DB
        verify(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId);
        verify(orderRepository).save(order);
    }

    // ❌ Capture thanh toán PayPal khi đơn hàng chưa được phê duyệt
    @Test
    void captureTransactionPaypal_Testcase5() throws Exception {
        // Prepare data
        String paypalOrderId = "PAYPAL_NOT_APPROVED";
        String payerId = "PAYER123";

        User user = mockUser();
        Order order = new Order();
        order.setPaypalOrderId(paypalOrderId);
        order.setUser(user);
        order.setCode("ORDER-CODE-003");
        order.setPaypalOrderStatus(OrderStatus.CREATED.toString());

        // Mock dependencies
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.of(order));
        doNothing().when(payPalHttpClient).capturePaypalTransaction(any(), any());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call method under test
        orderServiceImpl.captureTransactionPaypal(paypalOrderId, payerId);

        // Assertions
        assertEquals(OrderStatus.COMPLETED.toString(), order.getPaypalOrderStatus());
        System.out.println("⚠️ Capture đơn hàng chưa phê duyệt: " + order.getCode());

        // Kiểm tra DB
        verify(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId);
        verify(orderRepository, times(2)).save(order);
    }

    // ❌ Capture thanh toán PayPal khi lưu đơn hàng thất bại
    @Test
    void captureTransactionPaypal_Testcase6() throws Exception {
        // Prepare data
        String paypalOrderId = "PAYPAL_SAVE_FAIL";
        String payerId = "PAYER456";

        User user = mockUser();
        Order order = new Order();
        order.setPaypalOrderId(paypalOrderId);
        order.setUser(user);
        order.setCode("ORDER-CODE-004");

        // Mock dependencies
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.of(order));
        doNothing().when(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId);
        doThrow(new RuntimeException("DB error")).when(orderRepository).save(order);
        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

        // Call method under test
        try {
            orderServiceImpl.captureTransactionPaypal(paypalOrderId, payerId);
        } catch (RuntimeException e) {
            assertEquals("DB error", e.getMessage());
        }

        // Assertions
        assertEquals(OrderStatus.COMPLETED.toString(), order.getPaypalOrderStatus());

        // Kiểm tra DB
        verify(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId);
        verify(orderRepository).save(order);
        verify(notificationRepository).save(any());
    }

    // ✅ Capture thanh toán PayPal khi lưu thông báo thất bại
    @Test
    void captureTransactionPaypal_Testcase7() throws Exception {
        // Prepare data
        String paypalOrderId = "PAYPAL_NOTIFICATION_FAIL";
        String payerId = "PAYER789";

        User user = mockUser();
        Order order = new Order();
        order.setPaypalOrderId(paypalOrderId);
        order.setUser(user);
        order.setCode("ORDER-CODE-005");

        // Mock dependencies
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.of(order));
        doNothing().when(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId);
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("DB error while saving notification"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call method under test
        try {
            orderServiceImpl.captureTransactionPaypal(paypalOrderId, payerId);

            // Assertions
            assertEquals(OrderStatus.COMPLETED.toString(), order.getPaypalOrderStatus());
            assertEquals(2, order.getPaymentStatus());

            System.out.println("✅ Capture thanh toán PayPal thành công dù lưu thông báo thất bại: " + order.getCode());
        } catch (Exception e) {
            fail("Không nên throw exception: " + e.getMessage());
        }

        // Kiểm tra DB
        verify(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId);
        verify(notificationRepository).save(any());
        verify(notificationService, never()).pushNotification(any(), any());
        verify(orderRepository, times(2)).save(order);
    }

    // ✅ Capture thanh toán PayPal khi gửi thông báo thất bại
    @Test
    void captureTransactionPaypal_Testcase8() throws Exception {
        // Prepare data
        String paypalOrderId = "PAYPAL_PUSH_FAIL";
        String payerId = "PAYER123";

        User user = mockUser();
        Order order = new Order();
        order.setPaypalOrderId(paypalOrderId);
        order.setUser(user);
        order.setCode("ORDER-CODE-006");

        Notification notification = new Notification();

        // Mock dependencies
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.of(order));
        doNothing().when(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.entityToResponse(any(Notification.class))).thenReturn(null);
        doThrow(new RuntimeException("Push notification failure"))
                .when(notificationService).pushNotification(any(), any());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call method under test
        try {
            orderServiceImpl.captureTransactionPaypal(paypalOrderId, payerId);

            // Assertions
            assertEquals(OrderStatus.COMPLETED.toString(), order.getPaypalOrderStatus());
            assertEquals(2, order.getPaymentStatus());

            System.out.println("✅ Capture thanh toán PayPal thành công dù gửi thông báo thất bại: " + order.getCode());
        } catch (Exception e) {
            fail("Không nên throw exception: " + e.getMessage());
        }

        // Kiểm tra DB
        verify(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId);
        verify(notificationRepository).save(any());
        verify(notificationService).pushNotification(eq(user.getUsername()), any());
        verify(orderRepository, times(2)).save(order);
    }
}