package com.electro.service.order;

import com.electro.dto.waybill.GhnCancelOrderResponse;
import com.electro.dto.waybill.GhnCreateOrderResponse;
import com.electro.dto.waybill.WaybillRequest;
import com.electro.dto.waybill.WaybillResponse;
import com.electro.entity.cashbook.PaymentMethodType;
import com.electro.entity.order.Order;
import com.electro.entity.waybill.Waybill;
import com.electro.entity.waybill.WaybillLog;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.waybill.WaybillMapper;
import com.electro.repository.order.OrderRepository;
import com.electro.repository.waybill.WaybillRepository;
import com.electro.repository.waybill.WaybillLogRepository;
import com.electro.config.payment.paypal.PayPalHttpClient;
import com.electro.service.waybill.WaybillServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private WaybillRepository waybillRepository;

    @Mock
    private WaybillLogRepository waybillLogRepository;

    @Mock
    private PayPalHttpClient payPalHttpClient;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WaybillServiceImpl waybillService;

    @Mock
    private WaybillMapper waybillMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private Waybill waybill;

    @Value("${ghn.api.path}")
    private String ghnApiPath = "https://dev-online-gateway.ghn.vn/shiip/public-api/v2";

    @Value("${ghn.token}")
    private String ghnToken = "cee52cd3-8a9d-11ed-9ccc-a2c11deda90c";

    @Value("${ghn.shopId}")
    private String ghnShopId = "121327";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(orderService, "ghnToken", "cee52cd3-8a9d-11ed-9ccc-a2c11deda90c");
        ReflectionTestUtils.setField(orderService, "ghnShopId", "121327");
        ReflectionTestUtils.setField(orderService, "ghnApiPath", "https://dev-online-gateway.ghn.vn/shiip/public-api/v2");
        // Khởi tạo mock dữ liệu
        order = new Order();
        order.setCode("ORD123");
        order.setStatus(1);  // Đơn hàng đang xử lý

        waybill = new Waybill();
        waybill.setCode("WB123");
        waybill.setStatus(1);  // Vận đơn đang chờ lấy hàng
    }

    // Test khi không tìm thấy đơn hàng theo mã code
// => Phải ném ra ResourceNotFoundException với thông điệp phù hợp
//TC_ORDER_014
    @Test
    void testCancelOrder_OrderNotFound_ShouldThrowException() {
        // Mô phỏng khi không tìm thấy đơn hàng với mã code "ORD123"
        when(orderRepository.findByCode("ORD123")).thenReturn(java.util.Optional.empty());

        // Kiểm tra ngoại lệ khi không tìm thấy đơn hàng
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            orderService.cancelOrder("ORD123");  // Gọi phương thức cancelOrder với mã đơn hàng không tồn tại
        });

        // Kiểm tra thông báo ngoại lệ
        assertEquals("Order not found with order_code: 'ORD123'", exception.getMessage()); // Kiểm tra thông báo ngoại lệ
    }


    // "0WNQBIZHJP3U" ma don nay

    // GHN tra ve loi
    // Test trường hợp GHN API trả về lỗi HTTP (500 INTERNAL_SERVER_ERROR)
// => Phải ném RuntimeException với thông điệp báo lỗi API
//TC_ORDER_015
    @Test
    void testCancelOrder_GHNApiError_ShouldThrowRuntimeException() {
        // Mô phỏng khi tìm thấy đơn hàng và vận đơn
        when(orderRepository.findByCode("0WNQBIZHJP3U")).thenReturn(java.util.Optional.of(order));
        when(waybillRepository.findByOrderId(order.getId())).thenReturn(java.util.Optional.of(waybill));

        // Giả lập GHN API trả về lỗi 500
        String cancelOrderApiPath = "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/switch-status/cancel";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Token", "mockToken");
        headers.add("ShopId", "mockShopId");

        when(restTemplate.postForEntity(eq(cancelOrderApiPath), any(), eq(GhnCancelOrderResponse.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        // Kiểm tra ngoại lệ khi GHN API trả về lỗi
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.cancelOrder("0WNQBIZHJP3U");  // Gọi phương thức cancelOrder
        });

        assertEquals("Error when calling Cancel Order GHN API", exception.getMessage());  // Kiểm tra thông báo ngoại lệ
    }


    //TC_ORDER_016
    // don dang giao khong the huy
    // Test khi đơn hàng đang trong trạng thái giao (status >= 3)
// => Không được phép hủy, phải ném RuntimeException với thông điệp tương ứng

    @Test
    void testCancelOrder_OrderInDelivery_ShouldThrowException() {
        // Mô phỏng khi đơn hàng đang trong trạng thái giao hàng
        order.setStatus(3);  // Đơn hàng đang giao

        when(orderRepository.findByCode("ORD123")).thenReturn(java.util.Optional.of(order));

        // Kiểm tra ngoại lệ khi đơn hàng đang giao
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.cancelOrder("ORD123");  // Gọi phương thức cancelOrder với đơn hàng đang giao
        });

        assertEquals("Order with code ORD123 is in delivery or has been cancelled. Please check again!", exception.getMessage());
    }


    //TC_ORDER_017
    // tu choi khong co waybill
    // Test hủy đơn hàng thành công khi không có vận đơn
// => Chỉ cập nhật trạng thái đơn hàng thành 5 (Hủy), không gọi GHN API

    @Test
    void testCancelOrder_SuccessWithoutWaybill() {
        String code = "ORD124";
        Order order = new Order();
        order.setId(2L);
        order.setCode(code);
        order.setStatus(2);

        when(orderRepository.findByCode(code)).thenReturn(Optional.of(order));
        when(waybillRepository.findByOrderId(2L)).thenReturn(Optional.empty());

        orderService.cancelOrder(code);

        assertEquals(5, order.getStatus());
        verify(orderRepository).save(order);
        verifyNoInteractions(restTemplate);
    }


    //TC_ORDER_018
    // Test hủy đơn hàng thành công có kèm vận đơn, GHN trả về kết quả thành công
// => Cập nhật trạng thái đơn hàng, trạng thái vận đơn, và ghi log vận đơn

    @Test
    void testCancelOrder_SuccessWithWaybill() {
        String code = "74UJIRYKI1NN";
        Order order = new Order();
        order.setId(1L);
        order.setCode(code);
        order.setStatus(1);

        Waybill waybill = new Waybill();
        waybill.setId(100L);
        waybill.setCode("LBCPQ3");
        waybill.setOrder(order);
        waybill.setStatus(1);

        GhnCancelOrderResponse mockResponse = new GhnCancelOrderResponse();
        GhnCancelOrderResponse.Data$ data = new GhnCancelOrderResponse.Data$();
        data.setOrderCode(code);
        data.setResult(true);
        mockResponse.setData(List.of(data));

        when(orderRepository.findByCode(code)).thenReturn(Optional.of(order));
        when(waybillRepository.findByOrderId(1L)).thenReturn(Optional.of(waybill));
        when(restTemplate.postForEntity(anyString(), any(), eq(GhnCancelOrderResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        orderService.cancelOrder(code);

        assertEquals(5, order.getStatus());
        assertEquals(4, waybill.getStatus());

        verify(orderRepository).save(order);
        verify(waybillRepository).save(waybill);
        verify(waybillLogRepository).save(any(WaybillLog.class));
    }


// Test GHN trả về kết quả không thành công (result = false)
// => Chỉ cập nhật trạng thái đơn hàng thành 5, không cập nhật vận đơn, không ghi log

    //TC_ORDER_019
    @Test
    void testCancelOrder_GhnResponseFalse() {
        String code = "74UJIRYKI1NN";
        Order order = new Order();
        order.setId(4L);
        order.setCode(code);
        order.setStatus(1);

        Waybill waybill = new Waybill();
        waybill.setId(104L);
        waybill.setCode("LBCPQ3");
        waybill.setOrder(order);
        waybill.setStatus(1);

        GhnCancelOrderResponse.Data$ data = new GhnCancelOrderResponse.Data$();
        data.setOrderCode(code);
        data.setResult(false);

        GhnCancelOrderResponse response = new GhnCancelOrderResponse();
        response.setData(List.of(data));

        when(orderRepository.findByCode(code)).thenReturn(Optional.of(order));
        when(waybillRepository.findByOrderId(4L)).thenReturn(Optional.of(waybill));
        when(restTemplate.postForEntity(anyString(), any(), eq(GhnCancelOrderResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        orderService.cancelOrder(code);

        // order vẫn bị set 5
        assertEquals(5, order.getStatus());
        // waybill vẫn giữ nguyên
        assertEquals(1, waybill.getStatus());

        verify(orderRepository).save(order);
        verify(waybillRepository, never()).save(waybill);
        verify(waybillLogRepository, never()).save(any());
    }


    @Test
    void testFindById_Success() {
        Waybill waybill = new Waybill();
        waybill.setId(1L);
        WaybillResponse response = new WaybillResponse();

        when(waybillRepository.findById(1L)).thenReturn(Optional.of(waybill));
        when(waybillMapper.entityToResponse(waybill)).thenReturn(response);

        WaybillResponse result = waybillService.findById(1L);
        assertNotNull(result);
        verify(waybillRepository).findById(1L);
        verify(waybillMapper).entityToResponse(waybill);
    }

    @Test
    void testFindById_NotFound() {
        when(waybillRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> waybillService.findById(1L));
    }

    @Test
    void testSave_ExistingWaybill_ThrowsException() {
        WaybillRequest request = new WaybillRequest();
        request.setOrderId(1L);
        when(waybillRepository.findByOrderId(1L)).thenReturn(Optional.of(new Waybill()));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> waybillService.save(request));
        assertTrue(exception.getMessage().contains("already had a waybill"));
    }

    @Test
    void testSave_OrderNotFound_ThrowsException() {
        WaybillRequest request = new WaybillRequest();
        request.setOrderId(1L);
        when(waybillRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> waybillService.save(request));
    }

    @Test
    void testSaveWaybill_OrderAlreadyHasWaybill() {
        WaybillRequest waybillRequest = new WaybillRequest();
        waybillRequest.setOrderId(1L);

        when(waybillRepository.findByOrderId(1L)).thenReturn(Optional.of(new Waybill()));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            waybillService.save(waybillRequest);
        });

        assertEquals("This order already had a waybill. Please choose another order!", exception.getMessage());
    }

    @Test
    void testSaveWaybill_GhnApiError() {
        WaybillRequest waybillRequest = new WaybillRequest();
        waybillRequest.setOrderId(1L);

        Order order = new Order();
        order.setId(1L);
        order.setStatus(1);
        order.setPaymentMethodType(PaymentMethodType.CASH);
        order.setTotalPay(BigDecimal.valueOf(100000));


        when(waybillRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(GhnCreateOrderResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            waybillService.save(waybillRequest);
        });

        assertEquals("Error when calling Create Order GHN API", exception.getMessage());
    }





}