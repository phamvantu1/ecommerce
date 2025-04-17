package com.electro.quynh;

import com.electro.constant.AppConstants;
import com.electro.controller.client.ClientOrderController;
import com.electro.dto.ListResponse;
import com.electro.dto.client.ClientConfirmedOrderResponse;
import com.electro.dto.client.ClientOrderDetailResponse;
import com.electro.dto.client.ClientSimpleOrderRequest;
import com.electro.dto.client.ClientSimpleOrderResponse;
import com.electro.dto.general.NotificationResponse;
import com.electro.entity.authentication.User;
import com.electro.entity.general.Notification;
import com.electro.entity.order.Order;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.client.ClientOrderMapper;
import com.electro.mapper.general.NotificationMapper;
import com.electro.repository.general.NotificationRepository;
import com.electro.repository.order.OrderRepository;
import com.electro.service.general.NotificationService;
import com.electro.service.order.OrderService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho ClientOrderController, kiểm tra các chức năng liên quan đến quản lý đơn hàng của client.
 * Sử dụng Mockito để giả lập repository, service và mapper, đảm bảo kiểm tra logic độc lập với DB.
 */
class ClientOrderControllerTest {

    @InjectMocks
    private ClientOrderController controller;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ClientOrderMapper clientOrderMapper;

    @Mock
    private OrderService orderService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletRequest request;

    /**
     * Thiết lập trước mỗi test case: khởi tạo mock.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Test lấy danh sách đơn hàng khi có đơn hàng.
     * Kịch bản: Người dùng đã đăng nhập, có đơn hàng trong DB, trả về danh sách phân trang.
     * Liên kết mã nguồn: Phương thức getAllOrders (dòng 40-50).
     */
    @Test
    void getAllOrders_WithOrders_ReturnsPagedList() {
        // Chuẩn bị dữ liệu
        String username = "testuser";
        Order order = new Order();
        ClientSimpleOrderResponse response = new ClientSimpleOrderResponse();
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        when(authentication.getName()).thenReturn(username);
        when(orderRepository.findAllByUsername(eq(username), eq("createdAt,desc"), eq(null), any(PageRequest.class)))
                .thenReturn(orderPage);
        when(clientOrderMapper.entityToResponse(order)).thenReturn(response);

        // Thực hiện test
        ResponseEntity<ListResponse<ClientSimpleOrderResponse>> result = controller.getAllOrders(
                authentication, 1, 10, "createdAt,desc", null);

        // Kiểm tra kết quả
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getContent().size());
        assertEquals(response, result.getBody().getContent().get(0));
        // Kiểm tra DB: Đảm bảo gọi repository đúng tham số
        verify(orderRepository).findAllByUsername(eq(username), eq("createdAt,desc"), eq(null), any(PageRequest.class));
        verify(orderRepository, never()).save(any());
    }

    /**
     * Test lấy danh sách đơn hàng khi không có đơn hàng.
     * Kịch bản: Người dùng đã đăng nhập, không có đơn hàng, trả về danh sách rỗng.
     * Liên kết mã nguồn: Phương thức getAllOrders (dòng 40-50).
     */
    @Test
    void getAllOrders_NoOrders_ReturnsEmptyList() {
        // Chuẩn bị dữ liệu
        String username = "testuser";
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList());
        when(authentication.getName()).thenReturn(username);
        when(orderRepository.findAllByUsername(eq(username), eq("createdAt,desc"), eq(null), any(PageRequest.class)))
                .thenReturn(emptyPage);

        // Thực hiện test
        ResponseEntity<ListResponse<ClientSimpleOrderResponse>> result = controller.getAllOrders(
                authentication, 1, 10, "createdAt,desc", null);

        // Kiểm tra kết quả
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().getContent().isEmpty());
        // Kiểm tra DB: Đảm bảo gọi repository đúng tham số
        verify(orderRepository).findAllByUsername(eq(username), eq("createdAt,desc"), eq(null), any(PageRequest.class));
        verify(orderRepository, never()).save(any());
    }

    /**
     * Test lấy danh sách đơn hàng với tham số filter.
     * Kịch bản: Người dùng sử dụng filter để lọc đơn hàng, trả về danh sách phù hợp.
     * Liên kết mã nguồn: Phương thức getAllOrders (dòng 40-50).
     */
    @Test
    void getAllOrders_WithFilter_ReturnsFilteredList() {
        // Chuẩn bị dữ liệu
        String username = "testuser";
        String filter = "status:1";
        Order order = new Order();
        ClientSimpleOrderResponse response = new ClientSimpleOrderResponse();
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        when(authentication.getName()).thenReturn(username);
        when(orderRepository.findAllByUsername(eq(username), eq("createdAt,desc"), eq(filter), any(PageRequest.class)))
                .thenReturn(orderPage);
        when(clientOrderMapper.entityToResponse(order)).thenReturn(response);

        // Thực hiện test
        ResponseEntity<ListResponse<ClientSimpleOrderResponse>> result = controller.getAllOrders(
                authentication, 1, 10, "createdAt,desc", filter);

        // Kiểm tra kết quả
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getContent().size());
        // Kiểm tra DB: Đảm bảo gọi repository với filter
        verify(orderRepository).findAllByUsername(eq(username), eq("createdAt,desc"), eq(filter), any(PageRequest.class));
        verify(orderRepository, never()).save(any());
    }

    /**
     * Test lấy chi tiết đơn hàng khi đơn hàng tồn tại.
     * Kịch bản: Mã đơn hàng hợp lệ, trả về chi tiết đơn hàng.
     * Liên kết mã nguồn: Phương thức getOrder (dòng 52-58).
     */
    @Test
    void getOrder_OrderExists_ReturnsOrderDetail() {
        // Chuẩn bị dữ liệu
        String orderCode = "ORDER123";
        Order order = new Order();
        ClientOrderDetailResponse response = new ClientOrderDetailResponse();
        when(orderRepository.findByCode(orderCode)).thenReturn(Optional.of(order));
        when(clientOrderMapper.entityToDetailResponse(order)).thenReturn(response);

        // Thực hiện test
        ResponseEntity<ClientOrderDetailResponse> result = controller.getOrder(orderCode);

        // Kiểm tra kết quả
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        // Kiểm tra DB: Đảm bảo gọi repository đúng
        verify(orderRepository).findByCode(orderCode);
        verify(orderRepository, never()).save(any());
    }

    /**
     * Test lấy chi tiết đơn hàng khi đơn hàng không tồn tại.
     * Kịch bản: Mã đơn hàng không hợp lệ, ném ResourceNotFoundException.
     * Liên kết mã nguồn: Phương thức getOrder (dòng 52-58).
     */
    @Test
    void getOrder_OrderNotFound_ThrowsException() {
        // Chuẩn bị dữ liệu
        String orderCode = "INVALID_CODE";
        when(orderRepository.findByCode(orderCode)).thenReturn(Optional.empty());

        // Thực hiện test
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> controller.getOrder(orderCode));
        assertTrue(exception.getMessage().contains(orderCode));
        // Kiểm tra DB: Đảm bảo không lưu dữ liệu
        verify(orderRepository).findByCode(orderCode);
        verify(orderRepository, never()).save(any());
    }

    /**
     * Test hủy đơn hàng thành công.
     * Kịch bản: Mã đơn hàng hợp lệ, hủy thành công, trả về ObjectNode rỗng.
     * Liên kết mã nguồn: Phương thức cancelOrder (dòng 60-64).
     */
    @Test
    void cancelOrder_Success_ReturnsEmptyNode() {
        // Chuẩn bị dữ liệu
        String orderCode = "ORDER123";
        doNothing().when(orderService).cancelOrder(orderCode);

        // Thực hiện test
        ResponseEntity<ObjectNode> result = controller.cancelOrder(orderCode);

        // Kiểm tra kết quả
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isEmpty());
        // Kiểm tra DB: Đảm bảo gọi service, không trực tiếp gọi repository
        verify(orderService).cancelOrder(orderCode);
        verify(orderRepository, never()).save(any());
    }

    /**
     * Test hủy đơn hàng khi đơn hàng không tồn tại.
     * Kịch bản: Mã đơn hàng không hợp lệ, ném ResourceNotFoundException từ service.
     * Liên kết mã nguồn: Phương thức cancelOrder (dòng 60-64).
     */
    @Test
    void cancelOrder_OrderNotFound_ThrowsException() {
        // Chuẩn bị dữ liệu
        String orderCode = "INVALID_CODE";
        doThrow(new ResourceNotFoundException("Order", "code", orderCode))
                .when(orderService).cancelOrder(orderCode);

        // Thực hiện test
        assertThrows(ResourceNotFoundException.class, () -> controller.cancelOrder(orderCode));
        // Kiểm tra DB: Đảm bảo không lưu dữ liệu
        verify(orderService).cancelOrder(orderCode);
        verify(orderRepository, never()).save(any());
    }

    /**
     * Test tạo đơn hàng thành công.
     * Kịch bản: Yêu cầu hợp lệ, tạo đơn hàng thành công, trả về ClientConfirmedOrderResponse.
     * Liên kết mã nguồn: Phương thức createClientOrder (dòng 66-70).
     */
    @Test
    void createClientOrder_Success_ReturnsOrderResponse() {
        // Chuẩn bị dữ liệu
        ClientSimpleOrderRequest request = new ClientSimpleOrderRequest();
        ClientConfirmedOrderResponse response = new ClientConfirmedOrderResponse();
        when(orderService.createClientOrder(request)).thenReturn(response);

        // Thực hiện test
        ResponseEntity<ClientConfirmedOrderResponse> result = controller.createClientOrder(request);

        // Kiểm tra kết quả
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
        // Kiểm tra DB: Đảm bảo gọi service, không trực tiếp gọi repository
        verify(orderService).createClientOrder(request);
        verify(orderRepository, never()).save(any());
    }

    /**
     * Test tạo đơn hàng khi giỏ hàng rỗng.
     * Kịch bản: Yêu cầu tạo đơn hàng nhưng giỏ hàng rỗng, ném RuntimeException từ service.
     * Liên kết mã nguồn: Phương thức createClientOrder (dòng 66-70).
     */
    @Test
    void createClientOrder_EmptyCart_ThrowsException() {
        // Chuẩn bị dữ liệu
        ClientSimpleOrderRequest request = new ClientSimpleOrderRequest();
        when(orderService.createClientOrder(request)).thenThrow(new RuntimeException("Empty cart"));

        // Thực hiện test
        assertThrows(RuntimeException.class, () -> controller.createClientOrder(request));
        // Kiểm tra DB: Đảm bảo không lưu dữ liệu
        verify(orderService).createClientOrder(request);
        verify(orderRepository, never()).save(any());
    }

    /**
     * Test xử lý thanh toán PayPal thành công.
     * Kịch bản: Token và PayerID hợp lệ, capture giao dịch thành công, chuyển hướng đến trang thành công.
     * Liên kết mã nguồn: Phương thức paymentSuccessAndCaptureTransaction (dòng 72-80).
     */
    @Test
    void paymentSuccessAndCaptureTransaction_Success_RedirectsToSuccessPage() {
        // Chuẩn bị dữ liệu
        String paypalOrderId = "PAYPAL123";
        String payerId = "PAYER456";
        when(request.getParameter("token")).thenReturn(paypalOrderId);
        when(request.getParameter("PayerID")).thenReturn(payerId);
        doNothing().when(orderService).captureTransactionPaypal(paypalOrderId, payerId);

        // Thực hiện test
        RedirectView result = controller.paymentSuccessAndCaptureTransaction(request);

        // Kiểm tra kết quả
        assertEquals(AppConstants.FRONTEND_HOST + "/payment/success", result.getUrl());
        // Kiểm tra DB: Đảm bảo gọi service
        verify(orderService).captureTransactionPaypal(paypalOrderId, payerId);
        verify(orderRepository, never()).save(any());
    }

    /**
     * Test xử lý thanh toán PayPal khi token không hợp lệ.
     * Kịch bản: paypalOrderId không tìm thấy đơn hàng, ném ResourceNotFoundException.
     * Liên kết mã nguồn: Phương thức paymentSuccessAndCaptureTransaction (dòng 72-80).
     */
    @Test
    void paymentSuccessAndCaptureTransaction_InvalidToken_ThrowsException() {
        // Chuẩn bị dữ liệu
        String paypalOrderId = "INVALID_PAYPAL";
        String payerId = "PAYER456";
        when(request.getParameter("token")).thenReturn(paypalOrderId);
        when(request.getParameter("PayerID")).thenReturn(payerId);
        doThrow(new ResourceNotFoundException("Order", "paypalOrderId", paypalOrderId))
                .when(orderService).captureTransactionPaypal(paypalOrderId, payerId);

        // Thực hiện test
        assertThrows(ResourceNotFoundException.class,
                () -> controller.paymentSuccessAndCaptureTransaction(request));
        // Kiểm tra DB: Đảm bảo không lưu dữ liệu
        verify(orderService).captureTransactionPaypal(paypalOrderId, payerId);
        verify(orderRepository, never()).save(any());
    }

    /**
     * Test hủy thanh toán PayPal thành công.
     * Kịch bản: paypalOrderId hợp lệ, lưu thông báo và chuyển hướng đến trang hủy.
     * Liên kết mã nguồn: Phương thức paymentCancel (dòng 82-100).
     */
    @Test
    void paymentCancel_Success_RedirectsToCancelPage() {
        // Chuẩn bị dữ liệu
        String paypalOrderId = "PAYPAL123";
        Order order = new Order();
        order.setCode("ORDER123");
        User user = new User();
        user.setUsername("testuser");
        order.setUser(user);
        Notification notification = new Notification();
        NotificationResponse notificationResponse = new NotificationResponse();
        when(request.getParameter("token")).thenReturn(paypalOrderId);
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.of(order));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.entityToResponse(notification)).thenReturn(notificationResponse);

        // Thực hiện test
        RedirectView result = controller.paymentCancel(request);

        // Kiểm tra kết quả
        assertEquals(AppConstants.FRONTEND_HOST + "/payment/cancel", result.getUrl());
        // Kiểm tra DB và thông báo
        verify(orderRepository).findByPaypalOrderId(paypalOrderId);
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationService).pushNotification(eq("testuser"), eq(notificationResponse));
    }

    /**
     * Test hủy thanh toán PayPal khi đơn hàng không tồn tại.
     * Kịch bản: paypalOrderId không tìm thấy đơn hàng, ném ResourceNotFoundException.
     * Liên kết mã nguồn: Phương thức paymentCancel (dòng 82-100).
     */
    @Test
    void paymentCancel_OrderNotFound_ThrowsException() {
        // Chuẩn bị dữ liệu
        String paypalOrderId = "INVALID_PAYPAL";
        when(request.getParameter("token")).thenReturn(paypalOrderId);
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.empty());

        // Thực hiện test
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> controller.paymentCancel(request));
        assertTrue(exception.getMessage().contains(paypalOrderId));
        // Kiểm tra DB: Đảm bảo không lưu thông báo
        verify(orderRepository).findByPaypalOrderId(paypalOrderId);
        verify(notificationRepository, never()).save(any());
    }

    /**
     * Test hủy thanh toán PayPal khi lỗi lưu thông báo.
     * Kịch bản: paypalOrderId hợp lệ, nhưng lỗi khi lưu thông báo, ném RuntimeException.
     * Liên kết mã nguồn: Phương thức paymentCancel (dòng 82-100).
     */
    @Test
    void paymentCancel_SaveNotificationFails_ThrowsException() {
        // Chuẩn bị dữ liệu
        String paypalOrderId = "PAYPAL123";
        Order order = new Order();
        order.setCode("ORDER123");
        User user = new User();
        user.setUsername("testuser");
        order.setUser(user);
        when(request.getParameter("token")).thenReturn(paypalOrderId);
        when(orderRepository.findByPaypalOrderId(paypalOrderId)).thenReturn(Optional.of(order));
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("DB error"));

        // Thực hiện test
        assertThrows(RuntimeException.class, () -> controller.paymentCancel(request));
        // Kiểm tra DB: Đảm bảo gọi lưu thông báo nhưng thất bại
        verify(orderRepository).findByPaypalOrderId(paypalOrderId);
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationService, never()).pushNotification(any(), any());
    }
}