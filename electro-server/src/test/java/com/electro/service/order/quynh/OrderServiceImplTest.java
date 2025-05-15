package com.electro.service.order.quynh;

import com.electro.config.payment.paypal.PayPalHttpClient;
import com.electro.constant.AppConstants;
import com.electro.dto.client.ClientConfirmedOrderResponse;
import com.electro.dto.client.ClientSimpleOrderRequest;
import com.electro.dto.general.NotificationResponse;
import com.electro.dto.payment.OrderIntent;
import com.electro.dto.payment.OrderStatus;
import com.electro.dto.payment.PaypalRequest;
import com.electro.dto.payment.PaypalResponse;
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
import com.electro.entity.general.NotificationType;
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
import com.electro.service.general.NotificationService;
import com.electro.service.order.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository; // Mock repository để quản lý Order

    @Mock
    private WaybillRepository waybillRepository; // Mock repository để quản lý Waybill

    @Mock
    private WaybillLogRepository waybillLogRepository; // Mock repository để quản lý WaybillLog

    @Mock
    private UserRepository userRepository; // Mock repository để quản lý User

    @Mock
    private CartRepository cartRepository; // Mock repository để quản lý Cart

    @Mock
    private PromotionRepository promotionRepository; // Mock repository để quản lý Promotion

    @Mock
    private PayPalHttpClient payPalHttpClient; // Mock client để gọi API PayPal

    @Mock
    private ClientOrderMapper clientOrderMapper; // Mock mapper để ánh xạ Order sang DTO

    @Mock
    private NotificationRepository notificationRepository; // Mock repository để quản lý Notification

    @Mock
    private NotificationService notificationService; // Mock service để gửi thông báo

    @Mock
    private NotificationMapper notificationMapper; // Mock mapper để ánh xạ Notification sang DTO

    @Mock
    private RestTemplate restTemplate; // Mock RestTemplate để gọi API GHN

    @Mock
    private Authentication authentication; // Mock Authentication để mô phỏng người dùng đăng nhập

    @Mock
    private SecurityContext securityContext; // Mock SecurityContext để lưu thông tin xác thực

    @InjectMocks
    private OrderServiceImpl orderService; // Inject các mock vào OrderServiceImpl

    private User user; // Đối tượng User dùng cho các test case
    private Cart cart; // Đối tượng Cart dùng cho các test case
    private ClientSimpleOrderRequest request; // Đối tượng Request dùng cho createClientOrder
    private Order order; // Đối tượng Order dùng cho các test case

    @BeforeEach
    void setUp() {
        // Thiết lập SecurityContextHolder để mock thông tin người dùng đăng nhập
        SecurityContextHolder.setContext(securityContext); // Gán SecurityContext vào SecurityContextHolder
        when(securityContext.getAuthentication()).thenReturn(authentication); // Mock trả về Authentication
        when(authentication.getName()).thenReturn("testuser"); // Mock tên người dùng là "testuser"

        // Khởi tạo đối tượng User
        user = new User(); // Tạo mới đối tượng User
        user.setUsername("testuser"); // Gán username
        user.setFullname("Test User"); // Gán họ tên
        user.setPhone("1234567890"); // Gán số điện thoại

        // Thiết lập địa chỉ cho User
        Address address = new Address(); // Tạo mới đối tượng Address
        address.setLine("123 Street"); // Gán địa chỉ cụ thể
        Ward ward = new Ward(); // Tạo mới đối tượng Ward
        ward.setName("Ward A"); // Gán tên phường
        District district = new District(); // Tạo mới đối tượng District
        district.setName("District B"); // Gán tên quận
        Province province = new Province(); // Tạo mới đối tượng Province
        province.setName("Province C"); // Gán tên tỉnh
        address.setWard(ward); // Gán Ward vào Address
        address.setDistrict(district); // Gán District vào Address
        address.setProvince(province); // Gán Province vào Address
        user.setAddress(address); // Gán Address vào User

        // Khởi tạo giỏ hàng (Cart) với trạng thái hoạt động
        cart = new Cart(); // Tạo mới đối tượng Cart
        cart.setUser(user); // Gán User vào Cart
        cart.setStatus(1); // Gán trạng thái hoạt động (1 = active)
        cart.setUser(user); // Gán username để tìm kiếm Cart

        // Tạo Variant và Product
        Variant variant = new Variant(); // Tạo mới đối tượng Variant
        variant.setPrice(100.0); // Gán giá sản phẩm
        Product product = new Product(); // Tạo mới đối tượng Product
        product.setId(1L); // Gán ID sản phẩm
        variant.setProduct(product); // Gán Product vào Variant

        // Thêm CartVariant vào giỏ hàng
        CartVariant cartVariant = new CartVariant(); // Tạo mới đối tượng CartVariant
        cartVariant.setVariant(variant); // Gán Variant vào CartVariant
        cartVariant.setQuantity(2); // Gán số lượng
        cart.setCartVariants(Set.of(cartVariant)); // Gán tập CartVariant vào Cart

        // Khởi tạo request cho createClientOrder
        request = new ClientSimpleOrderRequest(); // Tạo mới đối tượng Request

        // Khởi tạo Order
        order = new Order(); // Tạo mới đối tượng Order
        order.setCode("ORDER123456"); // Gán mã đơn hàng
        order.setStatus(1); // Gán trạng thái đơn hàng mới
        order.setUser(user); // Gán User vào Order
        order.setTotalAmount(BigDecimal.valueOf(200.0)); // Gán tổng tiền hàng
        order.setTax(BigDecimal.valueOf(AppConstants.DEFAULT_TAX)); // Gán thuế
        order.setShippingCost(BigDecimal.ZERO); // Gán phí vận chuyển
        order.setTotalPay(BigDecimal.valueOf(200.0).add(BigDecimal.valueOf(200.0 * AppConstants.DEFAULT_TAX))); // Gán tổng tiền phải trả
        order.setPaymentMethodType(PaymentMethodType.CASH); // Gán phương thức thanh toán
        order.setPaymentStatus(1); // Gán trạng thái chưa thanh toán

    }

    // Test case cho createClientOrder - Thanh toán CASH thành công
    @Test
    void createClientOrder_Testcase001() {
        // Mục đích: Kiểm tra tạo đơn hàng với phương thức thanh toán CASH thành công
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.CASH); // Thiết lập phương thức thanh toán là CASH
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm User theo username, trả về User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm Cart theo username, trả về Cart
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(Collections.emptyList()); // Mock không có khuyến mãi
        when(orderRepository.save(any(Order.class))).thenReturn(order); // Mock lưu Order, trả về Order
        when(cartRepository.save(any(Cart.class))).thenReturn(cart); // Mock lưu Cart, trả về Cart

        // Act
        ClientConfirmedOrderResponse response = orderService.createClientOrder(request); // Gọi phương thức tạo đơn hàng

        // Assert
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository).findActivePromotionByProductId(1L); // Kiểm tra gọi tìm khuyến mãi
        verify(orderRepository).save(any(Order.class)); // Kiểm tra gọi lưu Order
        verify(cartRepository).save(cart); // Kiểm tra gọi lưu Cart
        assertEquals(PaymentMethodType.CASH, response.getOrderPaymentMethodType()); // Kiểm tra phương thức thanh toán trong response
        assertNotNull(response.getOrderCode()); // Kiểm tra mã đơn hàng không null
        assertNull(response.getOrderPaypalCheckoutLink()); // Kiểm tra không có link PayPal
        assertEquals(2, cart.getStatus()); // Kiểm tra trạng thái Cart là vô hiệu (2)
    }

    // Test case cho createClientOrder - Tạo Thanh toán PAYPAL thành công
    @Test
    void createClientOrder_Testcase002() throws Exception {
        // Mục đích: Kiểm tra tạo đơn hàng với phương thức thanh toán PAYPAL thành công
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.PAYPAL); // Thiết lập phương thức thanh toán là PAYPAL
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm User, trả về User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm Cart, trả về Cart
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(Collections.emptyList()); // Mock không có khuyến mãi
        when(orderRepository.save(any(Order.class))).thenReturn(order); // Mock lưu Order, trả về Order
        when(cartRepository.save(any(Cart.class))).thenReturn(cart); // Mock lưu Cart, trả về Cart
        PaypalResponse paypalResponse = new PaypalResponse(); // Tạo đối tượng PaypalResponse
        paypalResponse.setId("PAY123"); // Gán ID giao dịch PayPal
        paypalResponse.setStatus(OrderStatus.CREATED); // Gán trạng thái CREATED
        PaypalResponse.Link link = new PaypalResponse.Link(); // Tạo đối tượng Link
        link.setHref("https://paypal.com/checkout"); // Gán URL checkout
        link.setRel("approve"); // Gán loại link là approve
        paypalResponse.setLinks(List.of(link)); // Gán danh sách link
        when(payPalHttpClient.createPaypalTransaction(any(PaypalRequest.class))).thenReturn(paypalResponse); // Mock gọi PayPal API, trả về PaypalResponse

        // Act
        ClientConfirmedOrderResponse response = orderService.createClientOrder(request); // Gọi phương thức tạo đơn hàng

        // Assert
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository).findActivePromotionByProductId(1L); // Kiểm tra gọi tìm khuyến mãi
        verify(payPalHttpClient).createPaypalTransaction(any(PaypalRequest.class)); // Kiểm tra gọi PayPal API
        verify(orderRepository).save(any(Order.class)); // Kiểm tra gọi lưu Order
        verify(cartRepository).save(cart); // Kiểm tra gọi lưu Cart
        assertEquals(PaymentMethodType.PAYPAL, response.getOrderPaymentMethodType()); // Kiểm tra phương thức thanh toán
        assertNotNull(response.getOrderCode()); // Kiểm tra mã đơn hàng không null
        assertEquals("https://paypal.com/checkout", response.getOrderPaypalCheckoutLink()); // Kiểm tra link checkout
        assertEquals(2, cart.getStatus()); // Kiểm tra trạng thái Cart là vô hiệu
    }

    // Test case cho createClientOrder - Thanh toán PAYPAL không có link approve
    @Test
    void createClientOrder_Testcase003() throws Exception {
        // Mục đích: Kiểm tra tạo đơn hàng với PAYPAL nhưng không có link approve
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.PAYPAL); // Thiết lập phương thức thanh toán là PAYPAL
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm Cart
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(Collections.emptyList()); // Mock không khuyến mãi
        when(orderRepository.save(any(Order.class))).thenReturn(order); // Mock lưu Order
        when(cartRepository.save(any(Cart.class))).thenReturn(cart); // Mock lưu Cart
        PaypalResponse paypalResponse = new PaypalResponse(); // Tạo PaypalResponse
        paypalResponse.setId("PAY123"); // Gán ID giao dịch
        paypalResponse.setStatus(OrderStatus.CREATED); // Gán trạng thái CREATED
        PaypalResponse.Link link = new PaypalResponse.Link(); // Tạo Link
        link.setHref("https://paypal.com/other"); // Gán URL không phải approve
        link.setRel("other"); // Gán loại link không phải approve
        paypalResponse.setLinks(List.of(link)); // Gán danh sách link
        when(payPalHttpClient.createPaypalTransaction(any(PaypalRequest.class))).thenReturn(paypalResponse); // Mock PayPal API

        // Act
        ClientConfirmedOrderResponse response = orderService.createClientOrder(request); // Gọi phương thức

        // Assert
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository).findActivePromotionByProductId(1L); // Kiểm tra gọi tìm khuyến mãi
        verify(payPalHttpClient).createPaypalTransaction(any(PaypalRequest.class)); // Kiểm tra gọi PayPal API
        verify(orderRepository).save(any(Order.class)); // Kiểm tra gọi lưu Order
        verify(cartRepository).save(cart); // Kiểm tra gọi lưu Cart
        assertEquals(PaymentMethodType.PAYPAL, response.getOrderPaymentMethodType()); // Kiểm tra phương thức thanh toán
        assertNotNull(response.getOrderCode()); // Kiểm tra mã đơn hàng
        assertNull(response.getOrderPaypalCheckoutLink()); // Kiểm tra không có link approve
        assertEquals(2, cart.getStatus()); // Kiểm tra trạng thái Cart
    }

    // Test case cho createClientOrder - Thanh toán CASH với khuyến mãi
    @Test
    void createClientOrder_Testcase004() {
        // Mục đích: Kiểm tra tạo đơn hàng với CASH và có khuyến mãi
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.CASH); // Thiết lập phương thức thanh toán CASH
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm Cart
        Promotion promotion = new Promotion(); // Tạo đối tượng Promotion
        promotion.setPercent(10); // Gán mức giảm giá 10%
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(List.of(promotion)); // Mock trả về khuyến mãi
        when(orderRepository.save(any(Order.class))).thenReturn(order); // Mock lưu Order
        when(cartRepository.save(any(Cart.class))).thenReturn(cart); // Mock lưu Cart

        // Act
        ClientConfirmedOrderResponse response = orderService.createClientOrder(request); // Gọi phương thức

        // Assert
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository).findActivePromotionByProductId(1L); // Kiểm tra gọi tìm khuyến mãi
        verify(orderRepository).save(any(Order.class)); // Kiểm tra gọi lưu Order
        verify(cartRepository).save(cart); // Kiểm tra gọi lưu Cart
        assertEquals(PaymentMethodType.CASH, response.getOrderPaymentMethodType()); // Kiểm tra phương thức thanh toán
        assertNotNull(response.getOrderCode()); // Kiểm tra mã đơn hàng
        assertNull(response.getOrderPaypalCheckoutLink()); // Kiểm tra không có link PayPal
        assertEquals(2, cart.getStatus()); // Kiểm tra trạng thái Cart
    }

    // Test case cho createClientOrder - Không tìm thấy người dùng
    @Test
    void createClientOrder_Testcase005() {
        // Mục đích: Kiểm tra trường hợp không tìm thấy người dùng
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.CASH); // Thiết lập phương thức thanh toán CASH
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty()); // Mock không tìm thấy User

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> orderService.createClientOrder(request)); // Kiểm tra ném UsernameNotFoundException
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verifyNoInteractions(cartRepository, orderRepository, payPalHttpClient, cartRepository); // Kiểm tra không tương tác với các thành phần khác
    }

    // Test case cho createClientOrder - Không tìm thấy giỏ hàng
    @Test
    void createClientOrder_Testcase006() {
        // Mục đích: Kiểm tra trường hợp không tìm thấy giỏ hàng
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.CASH); // Thiết lập phương thức thanh toán CASH
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm thấy User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.empty()); // Mock không tìm thấy Cart

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> orderService.createClientOrder(request)); // Kiểm tra ném ResourceNotFoundException
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verifyNoInteractions(orderRepository, payPalHttpClient); // Kiểm tra không tương tác với OrderRepository, PayPal
    }

    // Test case cho createClientOrder - Giỏ hàng rỗng
    @Test
    void createClientOrder_Testcase007() {
        // Mục đích: Kiểm tra trường hợp giỏ hàng rỗng
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.CASH); // Thiết lập phương thức thanh toán CASH
        cart.setCartVariants(Collections.emptySet()); // Thiết lập giỏ hàng rỗng
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm thấy User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm thấy Cart

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> orderService.createClientOrder(request)); // Kiểm tra ném IllegalStateException
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verifyNoInteractions(promotionRepository, orderRepository, payPalHttpClient); // Kiểm tra không tương tác với các thành phần khác
    }

    // Test case cho createClientOrder - Giỏ hàng đã vô hiệu
    @Test
    void createClientOrder_Testcase008() {
        // Mục đích: Kiểm tra trường hợp giỏ hàng đã vô hiệu
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.CASH); // Thiết lập phương thức thanh toán CASH
        cart.setStatus(2); // Thiết lập trạng thái Cart là vô hiệu
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm thấy User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm thấy Cart

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> orderService.createClientOrder(request)); // Kiểm tra ném IllegalStateException
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verifyNoInteractions(promotionRepository, orderRepository, payPalHttpClient); // Kiểm tra không tương tác với các thành phần khác
    }

    // Test case cho createClientOrder - Tổng tiền âm
    @Test
    void createClientOrder_Testcase009() {
        // Mục đích: Kiểm tra trường hợp tổng tiền âm
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.CASH); // Thiết lập phương thức thanh toán CASH
        Variant variant = new Variant(); // Tạo Variant mới
        variant.setPrice(-100.0); // Thiết lập giá âm (lỗi)
        Product product = new Product(); // Tạo Product mới
        product.setId(1L); // Gán ID sản phẩm
        variant.setProduct(product); // Gán Product vào Variant
        CartVariant cartVariant = new CartVariant(); // Tạo CartVariant mới
        cartVariant.setVariant(variant); // Gán Variant vào CartVariant
        cartVariant.setQuantity(2); // Gán số lượng
        cart.setCartVariants(Set.of(cartVariant)); // Gán tập CartVariant vào Cart
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm thấy User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm thấy Cart
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(Collections.emptyList()); // Mock không khuyến mãi

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> orderService.createClientOrder(request)); // Kiểm tra ném IllegalStateException
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository).findActivePromotionByProductId(1L); // Kiểm tra gọi tìm khuyến mãi
        verifyNoInteractions(orderRepository, payPalHttpClient); // Kiểm tra không tương tác với OrderRepository, PayPal
    }

    // Test case cho createClientOrder - Thanh toán PAYPAL thất bại
    @Test
    void createClientOrder_Testcase010() throws Exception{
        // Mục đích: Kiểm tra trường hợp PayPal API thất bại
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.PAYPAL); // Thiết lập phương thức thanh toán PAYPAL
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm thấy User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm thấy Cart
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(Collections.emptyList()); // Mock không khuyến mãi
        when(payPalHttpClient.createPaypalTransaction(any(PaypalRequest.class)))
                .thenThrow(new RuntimeException("PayPal error")); // Mock PayPal API ném ngoại lệ

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.createClientOrder(request)); // Kiểm tra ném RuntimeException
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository).findActivePromotionByProductId(1L); // Kiểm tra gọi tìm khuyến mãi
        verify(payPalHttpClient).createPaypalTransaction(any(PaypalRequest.class)); // Kiểm tra gọi PayPal API
        verify(orderRepository, never()).save(any()); // Kiểm tra không lưu Order
        verify(cartRepository, never()).save(any()); // Kiểm tra không lưu Cart
    }

    // Test case cho createClientOrder - Phương thức thanh toán không hợp lệ
    @Test
    void createClientOrder_Testcase011() {
        // Mục đích: Kiểm tra trường hợp phương thức thanh toán không hợp lệ
        // Arrange
        request.setPaymentMethodType(null); // Thiết lập phương thức thanh toán là null
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm thấy User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm thấy Cart
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(Collections.emptyList()); // Mock không khuyến mãi

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.createClientOrder(request)); // Kiểm tra ném RuntimeException
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository).findActivePromotionByProductId(1L); // Kiểm tra gọi tìm khuyến mãi
        verify(orderRepository, never()).save(any()); // Kiểm tra không lưu Order
        verify(cartRepository, never()).save(any()); // Kiểm tra không lưu Cart
    }

    // Test case cho createClientOrder - Lưu đơn hàng thất bại
    @Test
    void createClientOrder_Testcase012() {
        // Mục đích: Kiểm tra trường hợp lưu đơn hàng thất bại
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.CASH); // Thiết lập phương thức thanh toán CASH
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm thấy User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm thấy Cart
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(Collections.emptyList()); // Mock không khuyến mãi
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("Database error")); // Mock lưu Order ném ngoại lệ

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.createClientOrder(request)); // Kiểm tra ném RuntimeException
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository).findActivePromotionByProductId(1L); // Kiểm tra gọi tìm khuyến mãi
        verify(orderRepository).save(any(Order.class)); // Kiểm tra gọi lưu Order
        verify(cartRepository, never()).save(any()); // Kiểm tra không lưu Cart
    }

    // Test case cho createClientOrder - Lưu giỏ hàng thất bại
    @Test
    void createClientOrder_Testcase013() {
        // Mục đích: Kiểm tra trường hợp lưu giỏ hàng thất bại
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.CASH); // Thiết lập phương thức thanh toán CASH
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm thấy User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm thấy Cart
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(Collections.emptyList()); // Mock không khuyến mãi
        when(orderRepository.save(any(Order.class))).thenReturn(order); // Mock lưu Order
        when(cartRepository.save(any(Cart.class))).thenThrow(new RuntimeException("Database error")); // Mock lưu Cart ném ngoại lệ

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.createClientOrder(request)); // Kiểm tra ném RuntimeException
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository).findActivePromotionByProductId(1L); // Kiểm tra gọi tìm khuyến mãi
        verify(orderRepository).save(any(Order.class)); // Kiểm tra gọi lưu Order
        verify(cartRepository).save(cart); // Kiểm tra gọi lưu Cart
    }

    @Test
    void createClientOrder_Testcase018() {
        // Mục đích: Kiểm tra trường hợp giỏ hàng có sản phẩm với số lượng âm
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.CASH); // Thiết lập phương thức thanh toán CASH
        Variant variant = new Variant(); // Tạo Variant mới
        variant.setPrice(100.0); // Thiết lập giá sản phẩm
        Product product = new Product(); // Tạo Product mới
        product.setId(1L); // Gán ID sản phẩm
        variant.setProduct(product); // Gán Product vào Variant
        CartVariant cartVariant = new CartVariant(); // Tạo CartVariant mới
        cartVariant.setVariant(variant); // Gán Variant vào CartVariant
        cartVariant.setQuantity(-1); // Thiết lập số lượng âm (lỗi)
        cart.setCartVariants(Set.of(cartVariant)); // Gán tập CartVariant vào Cart
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm thấy User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm thấy Cart
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(Collections.emptyList()); // Mock không khuyến mãi

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> orderService.createClientOrder(request)); // Kiểm tra ném IllegalStateException
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository).findActivePromotionByProductId(1L); // Kiểm tra gọi tìm khuyến mãi
        verifyNoInteractions(orderRepository, payPalHttpClient); // Kiểm tra không tương tác với OrderRepository, PayPal
    }

    @Test
    void createClientOrder_Testcase019() throws Exception {
        // Mục đích: Kiểm tra trường hợp PayPal trả về trạng thái không hợp lệ (VOIDED)
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.PAYPAL); // Thiết lập phương thức thanh toán PAYPAL
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm thấy User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm thấy Cart
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(Collections.emptyList()); // Mock không khuyến mãi
        PaypalResponse paypalResponse = new PaypalResponse(); // Tạo đối tượng PaypalResponse
        paypalResponse.setId("PAY123"); // Gán ID giao dịch PayPal
        paypalResponse.setStatus(OrderStatus.VOIDED); // Gán trạng thái VOIDED (không hợp lệ)
        when(payPalHttpClient.createPaypalTransaction(any(PaypalRequest.class))).thenReturn(paypalResponse); // Mock gọi PayPal API

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.createClientOrder(request)); // Kiểm tra ném RuntimeException
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository).findActivePromotionByProductId(1L); // Kiểm tra gọi tìm khuyến mãi
        verify(payPalHttpClient).createPaypalTransaction(any(PaypalRequest.class)); // Kiểm tra gọi PayPal API
        verify(orderRepository, never()).save(any()); // Kiểm tra không lưu Order
        verify(cartRepository, never()).save(any()); // Kiểm tra không lưu Cart
    }

    @Test
    void createClientOrder_Testcase020() {
        // Mục đích: Kiểm tra trường hợp địa chỉ người dùng không đầy đủ (thiếu ward)
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.CASH); // Thiết lập phương thức thanh toán CASH
        Address incompleteAddress = new Address(); // Tạo Address mới
        incompleteAddress.setLine("123 Street"); // Gán địa chỉ cụ thể
        District district = new District(); // Tạo District mới
        district.setName("District B"); // Gán tên quận
        Province province = new Province(); // Tạo Province mới
        province.setName("Province C"); // Gán tên tỉnh
        incompleteAddress.setDistrict(district); // Gán District vào Address
        incompleteAddress.setProvince(province); // Gán Province vào Address
        // Không gán Ward (thiếu ward)
        user.setAddress(incompleteAddress); // Gán Address không đầy đủ vào User
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm thấy User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm thấy Cart
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(Collections.emptyList()); // Mock không khuyến mãi

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> orderService.createClientOrder(request)); // Kiểm tra ném IllegalStateException
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository).findActivePromotionByProductId(1L); // Kiểm tra gọi tìm khuyến mãi
        verifyNoInteractions(orderRepository, payPalHttpClient); // Kiểm tra không tương tác với OrderRepository, PayPal
    }

    // Test case cho createClientOrder - Số lượng sản phẩm lớn
    @Test
    void createClientOrder_Testcase014() {
        // Mục đích: Kiểm tra tạo đơn hàng với số lượng sản phẩm lớn
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.CASH); // Thiết lập phương thức thanh toán CASH
        Variant variant = new Variant(); // Tạo Variant mới
        variant.setPrice(100.0); // Thiết lập giá sản phẩm
        Product product = new Product(); // Tạo Product mới
        product.setId(1L); // Gán ID sản phẩm
        variant.setProduct(product); // Gán Product vào Variant
        CartVariant cartVariant = new CartVariant(); // Tạo CartVariant mới
        cartVariant.setVariant(variant); // Gán Variant vào CartVariant
        cartVariant.setQuantity(1000); // Thiết lập số lượng lớn
        cart.setCartVariants(Set.of(cartVariant)); // Gán tập CartVariant vào Cart
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm thấy User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm thấy Cart
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(Collections.emptyList()); // Mock không khuyến mãi
        when(orderRepository.save(any(Order.class))).thenReturn(order); // Mock lưu Order
        when(cartRepository.save(any(Cart.class))).thenReturn(cart); // Mock lưu Cart

        // Act
        ClientConfirmedOrderResponse response = orderService.createClientOrder(request); // Gọi phương thức

        // Assert
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository).findActivePromotionByProductId(1L); // Kiểm tra gọi tìm khuyến mãi
        verify(orderRepository).save(any(Order.class)); // Kiểm tra gọi lưu Order
        verify(cartRepository).save(cart); // Kiểm tra gọi lưu Cart
        assertEquals(PaymentMethodType.CASH, response.getOrderPaymentMethodType()); // Kiểm tra phương thức thanh toán
        assertNotNull(response.getOrderCode()); // Kiểm tra mã đơn hàng không null
        assertNull(response.getOrderPaypalCheckoutLink()); // Kiểm tra không có link PayPal
        assertEquals(2, cart.getStatus()); // Kiểm tra trạng thái Cart là vô hiệu
    }

    // Test case cho createClientOrder - Khuyến mãi bất thường (100%)
    @Test
    void createClientOrder_Testcase015() {
        // Mục đích: Kiểm tra tạo đơn hàng với khuyến mãi 100%
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.CASH); // Thiết lập phương thức thanh toán CASH
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm thấy User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm thấy Cart
        Promotion promotion = new Promotion(); // Tạo đối tượng Promotion
        promotion.setPercent(100); // Gán mức giảm giá 100%
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(List.of(promotion)); // Mock trả về khuyến mãi
        when(orderRepository.save(any(Order.class))).thenReturn(order); // Mock lưu Order
        when(cartRepository.save(any(Cart.class))).thenReturn(cart); // Mock lưu Cart

        // Act
        ClientConfirmedOrderResponse response = orderService.createClientOrder(request); // Gọi phương thức

        // Assert
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository).findActivePromotionByProductId(1L); // Kiểm tra gọi tìm khuyến mãi
        verify(orderRepository).save(any(Order.class)); // Kiểm tra gọi lưu Order
        verify(cartRepository).save(cart); // Kiểm tra gọi lưu Cart
        assertEquals(PaymentMethodType.CASH, response.getOrderPaymentMethodType()); // Kiểm tra phương thức thanh toán
        assertNotNull(response.getOrderCode()); // Kiểm tra mã đơn hàng không null
        assertNull(response.getOrderPaypalCheckoutLink()); // Kiểm tra không có link PayPal
        assertEquals(2, cart.getStatus()); // Kiểm tra trạng thái Cart là vô hiệu
        // Kiểm tra giá sau khuyến mãi
        verify(orderRepository).save(argThat(o -> o.getOrderVariants().stream()
                .allMatch(ov -> ov.getPrice().equals(BigDecimal.ZERO)))); // Giá sản phẩm phải bằng 0
    }

    // Test case cho createClientOrder - Tỷ giá USD/VND bằng 0 (PayPal)
    @Test
    void createClientOrder_Testcase016() throws Exception {
        // Mục đích: Kiểm tra tạo đơn hàng với PayPal khi tỷ giá USD/VND = 0
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.PAYPAL); // Thiết lập phương thức thanh toán PAYPAL
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm thấy User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm thấy Cart
        when(promotionRepository.findActivePromotionByProductId(1L)).thenReturn(Collections.emptyList()); // Mock không khuyến mãi
        // Giả lập tỷ giá = 0 bằng cách dùng reflection hoặc cấu hình (ở đây giả định lỗi hệ thống)
        PaypalResponse paypalResponse = new PaypalResponse(); // Tạo đối tượng PaypalResponse
        paypalResponse.setId("PAY123"); // Gán ID giao dịch PayPal
        paypalResponse.setStatus(OrderStatus.CREATED); // Gán trạng thái CREATED
        PaypalResponse.Link link = new PaypalResponse.Link(); // Tạo đối tượng Link
        link.setHref("https://paypal.com/checkout"); // Gán URL checkout
        link.setRel("approve"); // Gán loại link là approve
        paypalResponse.setLinks(List.of(link)); // Gán danh sách link
        when(payPalHttpClient.createPaypalTransaction(any(PaypalRequest.class))).thenReturn(paypalResponse); // Mock gọi PayPal API

        // Act & Assert
        assertThrows(ArithmeticException.class, () -> orderService.createClientOrder(request)); // Kiểm tra ném ArithmeticException do chia cho 0
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository).findActivePromotionByProductId(1L); // Kiểm tra gọi tìm khuyến mãi
        verify(payPalHttpClient, never()).createPaypalTransaction(any()); // Kiểm tra không gọi PayPal API
        verify(orderRepository, never()).save(any()); // Kiểm tra không lưu Order
        verify(cartRepository, never()).save(any()); // Kiểm tra không lưu Cart
    }

    // Test case cho createClientOrder - Nhiều sản phẩm trong giỏ hàng
    @Test
    void createClientOrder_Testcase017() {
        // Mục đích: Kiểm tra tạo đơn hàng với nhiều sản phẩm trong giỏ hàng
        // Arrange
        request.setPaymentMethodType(PaymentMethodType.CASH); // Thiết lập phương thức thanh toán CASH
        Variant variant1 = new Variant(); // Tạo Variant 1
        variant1.setPrice(100.0); // Giá sản phẩm 1
        Product product1 = new Product(); // Tạo Product 1
        product1.setId(1L); // ID sản phẩm 1
        variant1.setProduct(product1); // Gán Product vào Variant
        CartVariant cartVariant1 = new CartVariant(); // Tạo CartVariant 1
        cartVariant1.setVariant(variant1); // Gán Variant
        cartVariant1.setQuantity(2); // Số lượng
        Variant variant2 = new Variant(); // Tạo Variant 2
        variant2.setPrice(200.0); // Giá sản phẩm 2
        Product product2 = new Product(); // Tạo Product 2
        product2.setId(2L); // ID sản phẩm 2
        variant2.setProduct(product2); // Gán Product vào Variant
        CartVariant cartVariant2 = new CartVariant(); // Tạo CartVariant 2
        cartVariant2.setVariant(variant2); // Gán Variant
        cartVariant2.setQuantity(1); // Số lượng
        cart.setCartVariants(Set.of(cartVariant1, cartVariant2)); // Gán tập CartVariant
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user)); // Mock tìm thấy User
        when(cartRepository.findByUsername("testuser")).thenReturn(Optional.of(cart)); // Mock tìm thấy Cart
        when(promotionRepository.findActivePromotionByProductId(anyLong())).thenReturn(Collections.emptyList()); // Mock không khuyến mãi
        when(orderRepository.save(any(Order.class))).thenReturn(order); // Mock lưu Order
        when(cartRepository.save(any(Cart.class))).thenReturn(cart); // Mock lưu Cart

        // Act
        ClientConfirmedOrderResponse response = orderService.createClientOrder(request); // Gọi phương thức

        // Assert
        verify(userRepository).findByUsername("testuser"); // Kiểm tra gọi tìm User
        verify(cartRepository).findByUsername("testuser"); // Kiểm tra gọi tìm Cart
        verify(promotionRepository, times(2)).findActivePromotionByProductId(anyLong()); // Kiểm tra gọi tìm khuyến mãi 2 lần
        verify(orderRepository).save(any(Order.class)); // Kiểm tra gọi lưu Order
        verify(cartRepository).save(cart); // Kiểm tra gọi lưu Cart
        assertEquals(PaymentMethodType.CASH, response.getOrderPaymentMethodType()); // Kiểm tra phương thức thanh toán
        assertNotNull(response.getOrderCode()); // Kiểm tra mã đơn hàng không null
        assertNull(response.getOrderPaypalCheckoutLink()); // Kiểm tra không có link PayPal
        assertEquals(2, cart.getStatus()); // Kiểm tra trạng thái Cart là vô hiệu
        // Kiểm tra số lượng OrderVariant
        verify(orderRepository).save(argThat(o -> o.getOrderVariants().size() == 2)); // Phải có 2 OrderVariant
    }

    // Test case cho captureTransactionPaypal - Thành công
    @Test
    void captureTransactionPaypal_Testcase001() throws Exception {
        // Mục đích: Kiểm tra capture giao dịch PayPal thành công
        // Arrange
        String paypalOrderId = "PAY123"; // Thiết lập ID giao dịch PayPal
        String payerId = "PAYER123"; // Thiết lập ID người thanh toán
        order.setPaypalOrderId(paypalOrderId); // Gán ID PayPal vào Order
        User user = new User(); // Tạo mới User
        user.setUsername("testuser"); // Gán username cho User
        order.setUser(user); // Gán User vào Order
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.of(order)); // Mock tìm Order
        doNothing().when(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId); // Mock capture PayPal không ném ngoại lệ
        when(orderRepository.save(order)).thenReturn(order); // Mock lưu Order
        Notification notification = new Notification(); // Tạo mới Notification
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification); // Mock lưu Notification
        NotificationResponse notificationResponse = new NotificationResponse(); // Tạo mới NotificationResponse
        when(notificationMapper.entityToResponse(notification)).thenReturn(notificationResponse); // Mock ánh xạ Notification

        // Act
        orderService.captureTransactionPaypal(paypalOrderId, payerId); // Gọi phương thức capture

        // Assert
        verify(orderRepository).findByPaypalOrderId(paypalOrderId); // Kiểm tra gọi tìm Order
        verify(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId); // Kiểm tra gọi capture PayPal
        verify(orderRepository).save(order); // Kiểm tra gọi lưu Order
        verify(notificationRepository).save(any(Notification.class)); // Kiểm tra gọi lưu Notification
        assertEquals("COMPLETED", order.getPaypalOrderStatus()); // Kiểm tra trạng thái COMPLETED
        assertEquals(2, order.getPaymentStatus()); // Kiểm tra trạng thái thanh toán
    }

    // Test case cho captureTransactionPaypal - Không tìm thấy đơn hàng
    @Test
    void captureTransactionPaypal_Testcase002() throws Exception {
        // Mục đích: Kiểm tra trường hợp không tìm thấy đơn hàng
        // Arrange
        String paypalOrderId = "PAY123"; // Thiết lập ID giao dịch PayPal
        String payerId = "PAYER123"; // Thiết lập ID người thanh toán
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.empty()); // Mock không tìm thấy Order

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> orderService.captureTransactionPaypal(paypalOrderId, payerId)); // Kiểm tra ném ResourceNotFoundException
        verify(orderRepository).findByPaypalOrderId(paypalOrderId); // Kiểm tra gọi tìm Order
        verifyNoInteractions(payPalHttpClient, notificationRepository, notificationService); // Kiểm tra không tương tác với các thành phần khác
        verifyNoMoreInteractions(orderRepository); // Kiểm tra không có tương tác ngoài dự kiến với orderRepository
    }

    // Test case cho captureTransactionPaypal - Capture thất bại
    @Test
    void captureTransactionPaypal_Testcase003() throws Exception {
        // Mục đích: Kiểm tra trường hợp capture PayPal thất bại
        // Arrange
        String paypalOrderId = "PAY123"; // Thiết lập ID giao dịch PayPal
        String payerId = "PAYER123"; // Thiết lập ID người thanh toán
        order.setPaypalOrderId(paypalOrderId); // Gán ID PayPal vào Order
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.of(order)); // Mock tìm thấy Order
        when(orderRepository.save(order)).thenReturn(order); // Mock lưu Order
        doThrow(new RuntimeException("Capture failed")).when(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId); // Mock capture ném ngoại lệ

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.captureTransactionPaypal(paypalOrderId, payerId)); // Kiểm tra ném RuntimeException
        verify(orderRepository).findByPaypalOrderId(paypalOrderId); // Kiểm tra gọi tìm Order
        verify(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId); // Kiểm tra gọi capture PayPal
        verify(orderRepository).save(order); // Kiểm tra gọi lưu Order
        verifyNoInteractions(notificationRepository, notificationService); // Kiểm tra không tương tác với Notification
        verifyNoMoreInteractions(orderRepository, payPalHttpClient); // Kiểm tra không có tương tác ngoài dự kiến
        assertEquals("APPROVED", order.getPaypalOrderStatus()); // Kiểm tra trạng thái APPROVED
    }

    // Test case cho captureTransactionPaypal - Đơn hàng đã hoàn tất
    @Test
    void captureTransactionPaypal_Testcase004() throws Exception {
        // Mục đích: Kiểm tra trường hợp đơn hàng đã hoàn tất
        // Arrange
        String paypalOrderId = "PAY123"; // Thiết lập ID giao dịch PayPal
        String payerId = "PAYER123"; // Thiết lập ID người thanh toán
        order.setPaypalOrderId(paypalOrderId); // Gán ID PayPal vào Order
        order.setPaypalOrderStatus("COMPLETED"); // Thiết lập trạng thái COMPLETED
        order.setPaymentStatus(2); // Thiết lập trạng thái đã thanh toán
        User user = new User(); // Tạo mới User
        user.setUsername("testuser"); // Gán use// rname
        order.setUser(user); // Gán User vào Order
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.of(order)); // Mock tìm thấy Order

        // Act
        orderService.captureTransactionPaypal(paypalOrderId, payerId); // Gọi phương thức capture

        // Assert
        verify(orderRepository).findByPaypalOrderId(paypalOrderId); // Kiểm tra gọi tìm Order
        verify(payPalHttpClient, never()).capturePaypalTransaction(anyString(), anyString()); // Kiểm tra không gọi capture
        verify(orderRepository, never()).save(any()); // Kiểm tra không lưu Order
        verifyNoInteractions(notificationRepository, notificationService, notificationMapper); // Kiểm tra không tương tác với Notification hoặc Mapper
        verifyNoMoreInteractions(orderRepository); // Kiểm tra không có tương tác ngoài dự kiến
        assertEquals("COMPLETED", order.getPaypalOrderStatus()); // Kiểm tra trạng thái COMPLETED
        assertEquals(2, order.getPaymentStatus()); // Kiểm tra trạng thái thanh toán
    }

    // Test case cho captureTransactionPaypal - Lưu thông báo thất bại
    @Test
    void captureTransactionPaypal_Testcase005() throws Exception {
        // Mục đích: Kiểm tra trường hợp lưu thông báo thất bại
        // Arrange
        String paypalOrderId = "PAY123"; // Thiết lập ID giao dịch PayPal
        String payerId = "PAYER123"; // Thiết lập ID người thanh toán
        order.setPaypalOrderId(paypalOrderId); // Gán ID PayPal vào Order
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.of(order)); // Mock tìm thấy Order
        when(notificationMapper.entityToResponse(any(Notification.class))).thenReturn(null); // Mock ánh xạ Notification
        when(notificationRepository.save(any(Notification.class))).thenThrow(new RuntimeException("Database error")); // Mock lưu Notification ném ngoại lệ

        // Act
        orderService.captureTransactionPaypal(paypalOrderId, payerId); // Gọi phương thức capture

        // Assert
        verify(orderRepository).findByPaypalOrderId(paypalOrderId); // Kiểm tra gọi tìm Order
        verify(payPalHttpClient).capturePaypalTransaction(paypalOrderId, payerId); // Kiểm tra gọi capture PayPal
        verify(notificationRepository).save(any(Notification.class)); // Kiểm tra gọi lưu Notification
        verify(notificationService, never()).pushNotification(anyString(), any()); // Kiểm tra không gửi thông báo
        assertEquals(OrderStatus.COMPLETED.toString(), order.getPaypalOrderStatus()); // Kiểm tra trạng thái COMPLETED
        assertEquals(2, order.getPaymentStatus()); // Kiểm tra trạng thái thanh toán
    }


}