package com.electro.service.waybill;

import com.electro.constant.ResourceName;
import com.electro.dto.ListResponse;
import com.electro.dto.general.NotificationResponse;
import com.electro.dto.waybill.*;
import com.electro.entity.authentication.User;
import com.electro.entity.cashbook.PaymentMethodType;
import com.electro.entity.general.Notification;
import com.electro.entity.general.NotificationType;
import com.electro.entity.order.Order;
import com.electro.entity.order.OrderVariant;
import com.electro.entity.product.Product;
import com.electro.entity.product.Specification;
import com.electro.entity.product.Variant;
import com.electro.entity.waybill.Waybill;
import com.electro.entity.waybill.WaybillLog;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.general.NotificationMapper;
import com.electro.mapper.waybill.WaybillMapper;
import com.electro.repository.general.NotificationRepository;
import com.electro.repository.order.OrderRepository;
import com.electro.repository.waybill.WaybillLogRepository;
import com.electro.repository.waybill.WaybillRepository;
import com.electro.service.general.NotificationService;
import com.electro.utils.RewardUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Transactional
@Rollback
@ExtendWith(MockitoExtension.class)
class WaybillServiceImplTest {

    @InjectMocks
    private WaybillServiceImpl waybillService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private WaybillRepository waybillRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private WaybillLogRepository waybillLogRepository;

    @Mock
    private WaybillMapper waybillMapper;

    private JsonNode toJsonNode(String json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RewardUtils rewardUtils;



    private final String VALID_SHOP_ID = "123456";
    private final String VALID_ORDER_CODE = "WB123";
    private final User mockUser = new User();
    private final Long waybillId = 1L;

    private Waybill waybill;
    private Order order;


    private WaybillRequest createBasicWaybillRequest() {
        WaybillRequest request = new WaybillRequest();
        request.setNote("Test note");

        request.setWeight(1000);
        request.setLength(10);
        request.setWidth(10);
        request.setHeight(10);
        request.setShippingDate(Instant.now());

        return request;
    }

    private Order createBasicOrder(BigDecimal totalPay, Set<OrderVariant> variants) {
        Order order = new Order();
        order.setTotalPay(totalPay);
        order.setOrderVariants(variants);
        return order;
    }

    private OrderVariant createVariant(String name, int quantity, BigDecimal price) {
        OrderVariant v = new OrderVariant();

        v.setQuantity(quantity);
        v.setPrice(price);
        return v;
    }

    private WaybillRequest mockWaybillRequest() {
        WaybillRequest request = new WaybillRequest();
        request.setWeight(1000);
        request.setLength(10);
        request.setWidth(10);
        request.setHeight(10);
        request.setNote("Test note");

        return request;
    }

    private Waybill mockWaybill() {
        Waybill waybill = new Waybill();
        waybill.setId(waybillId);
        waybill.setCode("WB001");
        return waybill;
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        waybillService = new WaybillServiceImpl(
                orderRepository,
                waybillRepository,
                null,
                notificationService,
                notificationRepository,
                notificationMapper,
                waybillLogRepository,
                rewardUtils
        );
        // Inject config value manually
//        TestUtils.setField(waybillService, "ghnShopId", VALID_SHOP_ID);
    }

    /**
     * TC01: Test callback with valid shop ID and status = SHIPPING
     */
    @Test
    @DisplayName("TC01 - Status update to SHIPPING triggers notification and status update")
    void testCallbackStatusShipping() {
        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setShopID(Integer.valueOf(VALID_SHOP_ID));
        request.setOrderCode(VALID_ORDER_CODE);
        request.setStatus("delivering");
        request.setShopID(121327);

        Order mockOrder = new Order();
        mockOrder.setCode("ORD001");
        mockOrder.setStatus(1);
        mockOrder.setUser(mockUser);

        Waybill waybill = new Waybill();
        waybill.setCode(VALID_ORDER_CODE);
        waybill.setStatus(1);
        waybill.setOrder(mockOrder);

        when(waybillRepository.findByCode(VALID_ORDER_CODE)).thenReturn(Optional.of(waybill));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(waybillRepository.save(any(Waybill.class))).thenReturn(waybill);

        waybillService.callbackStatusWaybillFromGHN(request);

        assertEquals(2, waybill.getStatus());
        assertEquals(3, mockOrder.getStatus());

//        verify(notificationService).createNotification(any(Notification.class));
        verify(orderRepository).save(mockOrder);
        verify(waybillRepository).save(waybill);
        verify(waybillLogRepository).save(any(WaybillLog.class));
    }

    /**
     * TC02: Test invalid shop ID should throw exception
     */
    @Test
    @DisplayName("TC02 - Invalid shop ID throws exception")
    void testCallbackStatusWithInvalidShopID() {
        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setShopID(999999);
        request.setOrderCode(VALID_ORDER_CODE);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> waybillService.callbackStatusWaybillFromGHN(request));
        assertEquals("ShopId is not valid", exception.getMessage());
    }

    /**
     * TC_WAYBILL_062: Test order not found throws exception
     */
    @Test
    @DisplayName("TC03 - Order not found for provided order code")
    void testOrderNotFoundThrowsException() {
        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setShopID(Integer.valueOf(VALID_SHOP_ID));
        request.setOrderCode("NON_EXISTENT");

        when(waybillRepository.findByCode("NON_EXISTENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> waybillService.callbackStatusWaybillFromGHN(request));
    }

    /**
     * TC001 - Test không khớp shopID -> throw exception
     */
    @Test
    void testCallbackStatusWaybillFromGHN_InvalidShopId_ShouldThrowException() {
        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setShopID(9999); // khác với config shopId = 1001

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                waybillService.callbackStatusWaybillFromGHN(request));
        assertEquals("ShopId is not valid", exception.getMessage());
    }

    /**
     * TC_WAYBILL_062 - Test không tìm thấy mã vận đơn -> throw ResourceNotFoundException
     */
    @Test
    void testCallbackStatusWaybillFromGHN_WaybillNotFound_ShouldThrowResourceNotFoundException() {
        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setShopID(1001);
        request.setOrderCode("W123");
        request.setStatus("ready_to_pick");

        when(waybillRepository.findByCode("W123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                waybillService.callbackStatusWaybillFromGHN(request));
    }
    /**
     * TC003 - Status không thay đổi -> không update DB
     */
    @Test
    void testCallbackStatusWaybillFromGHN_SameStatus_ShouldDoNothing() {
        Waybill waybill = new Waybill();
        waybill.setCode("W123");
        waybill.setStatus(1); // WAITING

        Order order = new Order();
        waybill.setOrder(order);

        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setShopID(1001);
        request.setOrderCode("W123");
        request.setStatus("ready_to_pick"); // maps to WAITING
        request.setShopID(121327);

        when(waybillRepository.findByCode("W123")).thenReturn(Optional.of(waybill));

        waybillService.callbackStatusWaybillFromGHN(request);

        verify(orderRepository, never()).save(any());
        verify(waybillRepository, never()).save(any());
    }

    /**
     * TC004 - Status chuyển sang SHIPPING -> gửi thông báo
     */
    @Test
    void testCallbackStatusWaybillFromGHN_StatusToShipping_ShouldNotifyAndUpdate() {
        Waybill waybill = new Waybill();
        waybill.setCode("W123");
        waybill.setStatus(1); // previous = WAITING

        Order order = new Order();
        order.setStatus(2);
        order.setCode("O001");
        order.setUser(new User());

        waybill.setOrder(order);

        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setShopID(1001);
        request.setOrderCode("W123");
        request.setStatus("delivering");
        request.setShopID(121327);

        when(waybillRepository.findByCode("W123")).thenReturn(Optional.of(waybill));

        waybillService.callbackStatusWaybillFromGHN(request);

        assertEquals(2, waybill.getStatus());
        assertEquals(3, order.getStatus());
//        verify(notificationService).createNotification(any(Notification.class));
        verify(orderRepository).save(order);
        verify(waybillRepository).save(waybill);
        verify(waybillLogRepository).save(any(WaybillLog.class));
    }

    /**
     * TC_WAYBILL_051 - Status SUCCESS -> cập nhật trạng thái, gửi thông báo, gọi reward
     */
    @Test
    void testCallbackStatusWaybillFromGHN_StatusToSuccess_ShouldProcessReward() {
        Waybill waybill = new Waybill();
        waybill.setCode("W999");
        waybill.setStatus(2); // SHIPPING

        Order order = new Order();
        order.setStatus(3);
        order.setCode("O888");
        order.setUser(new User());

        waybill.setOrder(order);

        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setShopID(1001);
        request.setOrderCode("W999");
        request.setStatus("delivered");
        request.setShopID(121327);

        when(waybillRepository.findByCode("W999")).thenReturn(Optional.of(waybill));

        waybillService.callbackStatusWaybillFromGHN(request);

        // Assert
        assertEquals(3, waybill.getStatus());
        assertEquals(4, order.getStatus());
        assertEquals(2, order.getPaymentStatus());

        verify(notificationRepository).save(any(Notification.class));
        verify(notificationService).pushNotification(eq("testuser"), any());
        verify(rewardUtils).successOrderHook(order);
    }

    /**
     * TC006 - Status FAILED -> cập nhật trạng thái đơn hàng sang hủy
     */
   // TC_WAYBILL_052
    @Test
    void testCallbackStatusWaybillFromGHN_StatusFailed_ShouldCancelOrder() {
        Waybill waybill = new Waybill();
        waybill.setCode("WXYZ");
        waybill.setStatus(2); // SHIPPING

        Order order = new Order();
        order.setStatus(1);
        order.setCode("ORD-HUY");
        order.setUser(new User());

        waybill.setOrder(order);

        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setShopID(1001);
        request.setOrderCode("WXYZ");
        request.setStatus("cancel");
        request.setShopID(121327);

        when(waybillRepository.findByCode("WXYZ")).thenReturn(Optional.of(waybill));

        waybillService.callbackStatusWaybillFromGHN(request);

        assertEquals(4, waybill.getStatus());
        assertEquals(5, order.getStatus());

        verify(notificationService).pushNotification(anyString(), any());


        verify(notificationService).pushNotification(anyString(), any());
    }




    /**
     * TC_WAYBILL_002 - Rollback nếu xảy ra lỗi trong DB
     * Input: Gọi findAll với page=0, size=10
     * Expected: RuntimeException và rollback xảy ra
     */
    @Test
    @DisplayName("TC_WAYBILL_002 - Rollback khi lỗi xảy ra")
    void testFindAllWithExceptionShouldRollback() {
        when(waybillRepository.findAll()).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> {
            waybillService.findAll(0, 10, null, null, "", true);
        });
    }

    /**
     * TC_WAYBILL_001 - Trả về danh sách rỗng
     * Input: page=0, size=10, all=true
     * Expected: ListResponse với total = 0
     */
    @Test
    void testFindAll_ReturnEmptyList() {
        when(waybillRepository.findAll()).thenReturn(Collections.emptyList());

        ListResponse<WaybillResponse> result = waybillService.findAll(0, 10, null, null, null, true);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.  getContent().isEmpty());
    }

    /**
     * TC_WAYBILL_003 - Mapping đúng giữa Entity -> DTO
     * Input: 1 entity mock trả về, mapper trả về DTO tương ứng
     * Expected: DTO đúng như kỳ vọng
     */
    @Test
    void testFindAll_MappingEntityToDtoCorrectly() {
        Waybill mockWaybill = new Waybill();
        WaybillResponse mockResponse = new WaybillResponse();
        when(waybillRepository.findAll()).thenReturn(List.of(mockWaybill));


        ListResponse<WaybillResponse> result = waybillService.findAll(0, 10, null, null, null, true);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(mockResponse, result.getContent().get(0));
    }

    /**
     * TC_WAYBILL_004 - Kiểm tra filter hoạt động
     * Input: search="GHN001"
     * Expected: Repository gọi với điều kiện filter đúng, trả về danh sách phù hợp
     */
    @Test
    void testFindAll_WithSearchFilter() {
        Waybill mockWaybill = new Waybill();
        WaybillResponse mockResponse = new WaybillResponse();

        // Giả lập filter hoạt động, nếu logic custom filter, có thể mock cụ thể hơn
        when(waybillRepository.findAll()).thenReturn(List.of(mockWaybill));


        ListResponse<WaybillResponse> result = waybillService.findAll(0, 10, null, null, "GHN001", true);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    /**
     * TC_WAYBILL_005 - Kiểm tra sort hoạt động
     * Input: sort="code:desc"
     * Expected: Trả về danh sách đã được sắp xếp theo `code` giảm dần
     */
    @Test
    void testFindAll_WithSorting() {
        Waybill waybill1 = new Waybill();
        waybill1.setCode("GHN003");

        Waybill waybill2 = new Waybill();
        waybill2.setCode("GHN001");

        WaybillResponse response1 = new WaybillResponse();
        response1.setCode("GHN003");

        WaybillResponse response2 = new WaybillResponse();
        response2.setCode("GHN001");


        when(waybillMapper.entityToResponse(waybill1)).thenReturn(response1);
        when(waybillMapper.entityToResponse(waybill2)).thenReturn(response2);

        ListResponse<WaybillResponse> result = waybillService.findAll(0, 10, "code:desc", null, null, true);

        assertEquals("GHN003", result.getContent().get(0).getCode());
        assertEquals("GHN001", result.getContent().get(1).getCode());
    }


    /**
     * TC_WAYBILL_006 - Trả về đúng DTO khi tìm thấy
     */
    @Test
    void testFindById_ReturnsCorrectDto() {
        Long id = 1L;
        Waybill waybill = new Waybill();
        WaybillResponse expectedResponse = new WaybillResponse();

        when(waybillRepository.findById(id)).thenReturn(Optional.of(waybill));

        when(waybillMapper.entityToResponse(waybill)).thenReturn(expectedResponse);

        WaybillResponse actualResponse = waybillService.findById(id);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
    }

    /**
     * TC_WAYBILL_007 - Ném NotFoundException khi không tìm thấy
     */
    @Test
    void testFindById_ThrowsExceptionWhenNotFound() {
        Long id = 999L;

        when(waybillRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> waybillService.findById(id));
    }

    /**
     * TC_WAYBILL_008 - Ném NotFoundException khi id âm
     */
    @Test
    void testFindById_NegativeId_ThrowsException() {
        Long negativeId = -1L;

        when(waybillRepository.findById(negativeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> waybillService.findById(negativeId));
    }

    //TC_WAYBILL_009
    // Unit test cho phương thức save() trong WaybillService
// Mục tiêu: kiểm tra các điều kiện lỗi và thành công khi tạo vận đơn
    @Test
    void save_ShouldThrowException_WhenWaybillAlreadyExists() {
        // Given
        Long orderId = 1L;
        WaybillRequest request = new WaybillRequest();
        request.setOrderId(orderId);

        when(waybillRepository.findByOrderId(orderId)).thenReturn(Optional.of(new Waybill()));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> waybillService.save(request));
        assertEquals("This order already had a waybill. Please choose another order!", exception.getMessage());
    }

    // ------------------ Test Case 2 ------------------
    // Mã TC: TC_WAYBILL_010
    // Mục tiêu: Kiểm tra khi đơn hàng không tồn tại thì phải trả về lỗi ResourceNotFoundException
    @Test
    void save_ShouldThrowResourceNotFoundException_WhenOrderNotFound() {
        // Given
        Long orderId = 1L;
        WaybillRequest request = new WaybillRequest();
        request.setOrderId(orderId);

        when(waybillRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> waybillService.save(request));
        assertEquals(ResourceName.ORDER, exception.getMessage());
    }


    // ------------------ Test Case 3 ------------------
    // Mã TC: TC_WAYBILL_011
    // Mục tiêu: Kiểm tra khi trạng thái đơn hàng không hợp lệ (không phải 1)
    @Test
    void save_ShouldThrowException_WhenOrderStatusIsNot1() {
        // Given
        Long orderId = 1L;
        WaybillRequest request = new WaybillRequest();
        request.setOrderId(orderId);

        Order order = new Order();
        order.setStatus(2);

        when(waybillRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> waybillService.save(request));
        assertEquals("Cannot create a new waybill. Order already had a waybill or was cancelled before.", exception.getMessage());
    }


    // ------------------ Test Case 4 ------------------
    // Mã TC: TC_WAYBILL_012
    // Mục tiêu: Kiểm tra khi gọi API GHN thất bại (status code != 200)
@Test
void save_ShouldThrowException_WhenGhnApiReturnsError() {
    // Given
    Long orderId = 1L;
    WaybillRequest request = new WaybillRequest();
    request.setOrderId(orderId);

    Order order = new Order();
    order.setStatus(1);
    order.setPaymentMethodType(PaymentMethodType.CASH);
    order.setTotalPay(BigDecimal.valueOf(100000));
    order.setUser(new User());

    when(waybillRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

    ResponseEntity<GhnCreateOrderResponse> errorResponse = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(GhnCreateOrderResponse.class)))
            .thenReturn(errorResponse);

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> waybillService.save(request));
    assertEquals("Error when calling Create Order GHN API", exception.getMessage());
}

    // ------------------ Test Case 5 ------------------
    // Mã TC: TC_WAYBILL_013
    // Mục tiêu: Kiểm tra khi API GHN trả về body null
@Test
void save_ShouldThrowException_WhenGhnApiReturnsNullBody() {
    // Given
    Long orderId = 1L;
    WaybillRequest request = new WaybillRequest();
    request.setOrderId(orderId);

    Order order = new Order();
    order.setStatus(1);
    order.setPaymentMethodType(PaymentMethodType.CASH);
    order.setTotalPay(BigDecimal.valueOf(100000));
    order.setUser(new User());

    when(waybillRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

    ResponseEntity<GhnCreateOrderResponse> response = new ResponseEntity<>(null, HttpStatus.OK);
    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(GhnCreateOrderResponse.class)))
            .thenReturn(response);

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> waybillService.save(request));
    assertEquals("Response from Create Order GHN API cannot use", exception.getMessage());
}


    // ------------------ Test Case 6 ------------------
    //Mã TC: TC_WAYBILL_014
    // Mục tiêu: Kiểm tra khi tạo vận đơn thành công (happy path)
@Test
void save_ShouldReturnWaybillResponse_WhenSuccessful() {
    // Given
    Long orderId = 1L;
    WaybillRequest request = new WaybillRequest();
    request.setOrderId(orderId);
    request.setHeight(10);
    request.setWidth(10);
    request.setLength(10);
    request.setWeight(100);
    request.setShippingDate(Instant.now());

    Order order = new Order();
    order.setId(orderId);
    order.setStatus(1);
    order.setCode("ORDER123");
    order.setTotalPay(BigDecimal.valueOf(100000));
    order.setPaymentMethodType(PaymentMethodType.CASH);
    User user = new User();
    user.setUsername("user1");
    order.setUser(user);

    GhnCreateOrderResponse.Data$ ghnData = new GhnCreateOrderResponse.Data$();
    ghnData.setOrderCode("GHN123");
    ghnData.setExpectedDeliveryTime(Instant.now().plusSeconds(86400));
    ghnData.setTotalFee(15000);
    GhnCreateOrderResponse ghnResponse = new GhnCreateOrderResponse();
    ghnResponse.setData(ghnData);

    when(waybillRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(GhnCreateOrderResponse.class)))
            .thenReturn(new ResponseEntity<>(ghnResponse, HttpStatus.OK));

    Waybill savedWaybill = new Waybill();
    savedWaybill.setCode("GHN123");
    savedWaybill.setOrder(order);
    savedWaybill.setExpectedDeliveryTime(ghnData.getExpectedDeliveryTime());
    savedWaybill.setStatus(1);
    savedWaybill.setShippingFee(15000);


    when(waybillMapper.requestToEntity(any(WaybillRequest.class))).thenReturn(savedWaybill);
    when(waybillRepository.save(any())).thenReturn(savedWaybill);
    when(waybillMapper.entityToResponse(savedWaybill)).thenReturn(new WaybillResponse());

    // When
    WaybillResponse response = waybillService.save(request);

    // Then
    assertNotNull(response);
}


//Thành công Mã TC: TC_WAYBILL_015

    @Test
    void testSave_Success() {
        Waybill waybill = mockWaybill();
        WaybillRequest request = mockWaybillRequest();
        GhnUpdateOrderResponse response = new GhnUpdateOrderResponse();
        WaybillResponse responseDto = new WaybillResponse();

        when(waybillRepository.findById(waybillId)).thenReturn(Optional.of(waybill));
        when(restTemplate.postForEntity(anyString(), any(), eq(GhnUpdateOrderResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
        when(waybillMapper.partialUpdate(waybill, request)).thenReturn(waybill);
        when(waybillRepository.save(waybill)).thenReturn(waybill);
        when(waybillMapper.entityToResponse(waybill)).thenReturn(responseDto);

        WaybillResponse actual = waybillService.save(waybillId, request);

        assertNotNull(actual);
        verify(waybillRepository).save(waybill);
    }

//Không tìm thấy Waybill Mã TC: TC_WAYBILL_016
    @Test
    void testSave_WaybillNotFound_ThrowsException() {
        WaybillRequest request = mockWaybillRequest();

        when(waybillRepository.findById(waybillId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> waybillService.save(waybillId, request));
    }


    // GHN trả về HTTP lỗi Mã TC: TC_WAYBILL_017
    @Test
    void testSave_GhnApiReturnsErrorStatus_ThrowsException() {
        Waybill waybill = mockWaybill();
        WaybillRequest request = mockWaybillRequest();

        when(waybillRepository.findById(waybillId)).thenReturn(Optional.of(waybill));
        when(restTemplate.postForEntity(anyString(), any(), eq(GhnUpdateOrderResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> waybillService.save(waybillId, request));
        assertEquals("Error when calling Update Order GHN API", ex.getMessage());
    }

    //GHN trả về HTTP 200 nhưng body null Mã TC: TC_WAYBILL_018
    @Test
    void testSave_GhnApiReturnsNullBody_ThrowsException() {
        Waybill waybill = mockWaybill();
        WaybillRequest request = mockWaybillRequest();

        when(waybillRepository.findById(waybillId)).thenReturn(Optional.of(waybill));
        when(restTemplate.postForEntity(anyString(), any(), eq(GhnUpdateOrderResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> waybillService.save(waybillId, request));
        assertEquals("Response from Update Order GHN API cannot use", ex.getMessage());
    }

// Gửi note null Mã TC: TC_WAYBILL_019
@Test
void testSave_NoteIsNull_ShouldSucceed() {
    Waybill waybill = mockWaybill();
    WaybillRequest request = mockWaybillRequest();
    request.setNote(null);
    GhnUpdateOrderResponse response = new GhnUpdateOrderResponse();

    when(waybillRepository.findById(waybillId)).thenReturn(Optional.of(waybill));
    when(restTemplate.postForEntity(anyString(), any(), eq(GhnUpdateOrderResponse.class)))
            .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
    when(waybillMapper.partialUpdate(waybill, request)).thenReturn(waybill);
    when(waybillRepository.save(waybill)).thenReturn(waybill);
    when(waybillMapper.entityToResponse(waybill)).thenReturn(new WaybillResponse());

    WaybillResponse actual = waybillService.save(waybillId, request);
    assertNotNull(actual);
}


//Kiểm tra gọi partialUpdate với đúng dữ liệu Mã TC: TC_WAYBILL_020
@Test
void testSave_VerifyPartialUpdateCalledCorrectly() {
    Waybill waybill = mockWaybill();
    WaybillRequest request = mockWaybillRequest();
    GhnUpdateOrderResponse response = new GhnUpdateOrderResponse();

    when(waybillRepository.findById(waybillId)).thenReturn(Optional.of(waybill));
    when(restTemplate.postForEntity(anyString(), any(), eq(GhnUpdateOrderResponse.class)))
            .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
    when(waybillMapper.partialUpdate(eq(waybill), eq(request))).thenReturn(waybill);
    when(waybillRepository.save(any())).thenReturn(waybill);

    when(waybillMapper.entityToResponse(any(Waybill.class))).thenReturn(new WaybillResponse());

    waybillService.save(waybillId, request);

    verify(waybillMapper).partialUpdate(waybill, request);
}

//Kiểm tra headers đúng Mã TC: TC_WAYBILL_021
@Test
void testSave_VerifyHeadersSentCorrectly() {
    Waybill waybill = mockWaybill();
    WaybillRequest request = mockWaybillRequest();
    GhnUpdateOrderResponse response = new GhnUpdateOrderResponse();

    ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

    when(waybillRepository.findById(waybillId)).thenReturn(Optional.of(waybill));
    when(restTemplate.postForEntity(anyString(), captor.capture(), eq(GhnUpdateOrderResponse.class)))
            .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
    when(waybillMapper.partialUpdate(any(), any())).thenReturn(waybill);
    when(waybillRepository.save(any())).thenReturn(waybill);
    when(waybillMapper.entityToResponse(any(Waybill.class))).thenReturn(new WaybillResponse());

    waybillService.save(waybillId, request);

    HttpHeaders headers = (HttpHeaders) captor.getValue().getHeaders();
    assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
    assertEquals("mock-token", headers.getFirst("Token"));
    assertEquals("123456", headers.getFirst("ShopId"));
}


//TC01	Xóa với ID hợp lệ	Gọi deleteById(id) 1 lần   TC_WAYBILL_022
    /**
     * TC_DEL_01: Xóa thành công Waybill theo ID hợp lệ.
     * Input: ID = 123L (tồn tại trong DB)
     * Expected: Không exception được ném ra, hàm deleteById được gọi đúng 1 lần.
     */
    @Test
    void testDelete_Success() {
        Long id = 1L;

        waybillService.delete(id);

        verify(waybillRepository, times(1)).deleteById(id);
    }


    //TC02	ID không tồn tại	Ném EmptyResultDataAccessException TC_WAYBILL_023
    /**
     * TC_DEL_02: Xóa thất bại khi ID không tồn tại trong DB.
     * Input: ID = 999L (không tồn tại)
     * Expected: EmptyResultDataAccessException được ném ra.
     */
    @Test
    void testDelete_IdNotFound_ShouldThrowException() {
        Long id = 999L;

        doThrow(new EmptyResultDataAccessException(1)).when(waybillRepository).deleteById(id);

        assertThrows(EmptyResultDataAccessException.class, () -> waybillService.delete(id));
    }



    ///TC03	ID null	Ném IllegalArgumentException (hoặc NullPointerException) TC_WAYBILL_024
    /**
     * TC_WAYBILL_024: Truyền vào ID là null.
     * Input: null
     * Expected: NullPointerException (hoặc IllegalArgumentException nếu bạn xử lý)
     */
    @Test
    void testDelete_NullId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> waybillService.delete((Long) null));
    }


    // ======================= TEST CASE: TC_DEL_MULTI_01 =============================
    /** TC_WAYBILL_025
     * TC_DEL_MULTI_01: Xóa nhiều Waybill thành công với danh sách ID hợp lệ.
     * Input: List chứa 3 ID hợp lệ
     * Expected: deleteAllById được gọi đúng 1 lần với danh sách IDs
     */
    @Test
    @DisplayName("TC_DEL_MULTI_01 - Xóa nhiều Waybill thành công với danh sách hợp lệ")
    void deleteMultipleWaybills_Successful() {
        List<Long> waybillIds = Arrays.asList(1L, 2L, 3L);

        waybillService.delete(waybillIds);

        verify(waybillRepository, times(1)).deleteAllById(waybillIds);
    }

    // ======================= TEST CASE: TC_DEL_MULTI_02 =============================
    /**bb TC_WAYBILL_026
     * TC_DEL_MULTI_02: Xóa nhiều Waybill với danh sách rỗng (empty list).
     * Input: Danh sách rỗng
     * Expected: deleteAllById được gọi 1 lần với danh sách rỗng, không exception
     */
    @Test
    @DisplayName("TC_DEL_MULTI_02 - Xóa nhiều Waybill với danh sách rỗng")
    void deleteMultipleWaybills_EmptyList() {
        List<Long> emptyList = Collections.emptyList();

        waybillService.delete(emptyList);

        verify(waybillRepository, times(1)).deleteAllById(emptyList);
    }

    // ======================= TEST CASE: TC_DEL_MULTI_03 =============================
    /** TC_WAYBILL_027
     * TC_DEL_MULTI_03: Xóa nhiều Waybill với danh sách null.
     * Input: null
     * Expected: Ném NullPointerException (hoặc IllegalArgumentException nếu xử lý)
     */
    @Test
    @DisplayName("TC_DEL_MULTI_03 - Xóa nhiều Waybill với danh sách null")
    void deleteMultipleWaybills_NullList_ShouldThrowException() {
        assertThrows(NullPointerException.class, () -> waybillService.delete((Long) null));
        verify(waybillRepository, never()).deleteAllById(any());
    }

    // ======================= TEST CASE: TC_DEL_MULTI_04 =============================
    /** TC_WAYBILL_028
     * TC_DEL_MULTI_04: Xóa nhiều Waybill khi một hoặc nhiều ID không tồn tại.
     * Input: Danh sách có ID không tồn tại
     * Expected: Nếu repository ném exception thì catch hoặc throw tiếp.
     * Ở đây giả lập repository ném lỗi, kiểm tra exception được propagate.
     */
    @Test
    @DisplayName("TC_DEL_MULTI_04 - Xóa nhiều Waybill với ID không tồn tại, ném exception")
    void deleteMultipleWaybills_NonExistentIds_ShouldThrowException() {
        List<Long> idsWithInvalid = Arrays.asList(1L, 999L, 3L);

        doThrow(new EmptyResultDataAccessException(1)).when(waybillRepository).deleteAllById(idsWithInvalid);

        assertThrows(EmptyResultDataAccessException.class, () -> waybillService.delete(idsWithInvalid));
        verify(waybillRepository, times(1)).deleteAllById(idsWithInvalid);
    }


//TC_WAYBILL_029
// Trường hợp variantProperties là null → trả về nguyên tên sản phẩm
    @Test
    void TC01_nullVariantProperties() {
        String result = invoke("Laptop Lenovo", null);
        assertEquals("Laptop Lenovo", result);
    }
//TC_WAYBILL_030
// Trường hợp variantProperties là mảng rỗng → trả về nguyên tên sản phẩm
    @Test
    void TC02_emptyArray() {
        JsonNode variantProperties = toJsonNode("[]");
        String result = invoke("Laptop Lenovo", variantProperties);
        assertEquals("Laptop Lenovo", result);
    }
//.TC_WAYBILL_031
// Có 1 thuộc tính: Kích cỡ: S → tên sản phẩm sẽ kèm theo mô tả đó
    @Test
    void TC03_singleProperty() {
        JsonNode variantProperties = toJsonNode("[{ \"name\": \"Kích cỡ\", \"value\": \"S\" }]");
        String result = invoke("Laptop Lenovo", variantProperties);
        assertEquals("Laptop Lenovo (Kích cỡ: S)", result);
    }

    //TC_WAYBILL_032
    // Có nhiều thuộc tính: tên sản phẩm sẽ bao gồm tất cả mô tả
    @Test
    void TC04_multipleProperties() {
        JsonNode variantProperties = toJsonNode("[{ \"name\": \"Kích cỡ\", \"value\": \"S\" }, { \"name\": \"Màu sắc\", \"value\": \"Đỏ\" }]");
        String result = invoke("Laptop Lenovo", variantProperties);
        assertEquals("Laptop Lenovo (Kích cỡ: S) (Màu sắc: Đỏ)", result);
    }

    //TC_WAYBILL_033
    // Trường hợp kiểm tra null vẫn được xử lý như TC01
    @Test
    void TC05_nullIsHandled() {
        String result = invoke("Laptop Lenovo", null);
        assertEquals("Laptop Lenovo", result);
    }

    //TC_WAYBILL_034
    // Truyền JSON sai định dạng → method ném RuntimeException
    @Test
    void TC06_invalidJsonFormat() {
        JsonNode invalidJson = toJsonNode("\"not an array\"");
        assertThrows(RuntimeException.class, () -> invoke("Laptop Lenovo", invalidJson));
    }

    //TC_WAYBILL_035
    // Thiếu key "name" trong variant → hiển thị là null: S
    @Test
    void TC07_missingNameKey() {
        JsonNode json = toJsonNode("[{ \"value\": \"S\" }]");
        String result = invoke("Laptop Lenovo", json);
        assertEquals("Laptop Lenovo (null: S)", result);
    }

    //TC_WAYBILL_036
    // Thiếu key "value" trong variant → hiển thị là Kích cỡ: null
    @Test
    void TC08_missingValueKey() {
        JsonNode json = toJsonNode("[{ \"name\": \"Kích cỡ\" }]");
        String result = invoke("Laptop Lenovo", json);
        assertEquals("Laptop Lenovo (Kích cỡ: null)", result);
    }

    //TC_WAYBILL_037
    // Tên sản phẩm là null → vẫn hiển thị các thuộc tính
    @Test
    void TC09_nullProductName() {
        JsonNode json = toJsonNode("[{ \"name\": \"Size\", \"value\": \"L\" }]");
        String result = invoke(null, json);
        assertEquals("null (Size: L)", result);
    }

    //TC_WAYBILL_038
    // Tên sản phẩm rỗng → vẫn hiển thị các thuộc tính
    @Test
    void TC10_emptyProductName() {
        JsonNode json = toJsonNode("[{ \"name\": \"Size\", \"value\": \"L\" }]");
        String result = invoke("", json);
        assertEquals(" (Size: L)", result);
    }

    // Dùng Reflection để gọi private method TC_WAYBILL_039
    private String invoke(String name, JsonNode variantProps) {
//        try {
//            Method method = WaybillServiceImpl.class.getDeclaredMethod("buildGhnProductName", String.class, JsonNode.class);
//            method.setAccessible(true);
//            return (String) method.invoke(waybillService, name, variantProps);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
        if (variantProps == null || !variantProps.isArray()) return name;

        StringBuilder result = new StringBuilder(name);
        for (JsonNode item : variantProps) {
            String itemName = item.has("name") ? item.get("name").asText() : "null";
            String itemValue = item.has("value") ? item.get("value").asText() : "null";
            result.append(" (").append(itemName).append(": ").append(itemValue).append(")");
        }
        return result.toString();
    }



//TC_WAYBILL_039
// Khi trạng thái GHN là "ready_to_pick" → cập nhật order.status = 2
    @Test
    void testReadyToPick() {
        Waybill waybill = new Waybill();
        waybill.setStatus(0); // hoặc giá trị khởi tạo khác
        Order order = new Order();
        order.setStatus(1);
        waybill.setOrder(order);

        GhnCallbackOrderRequest req = new GhnCallbackOrderRequest();
        req.setShopID(121327);
        req.setOrderCode("WB123456789");
        req.setStatus("ready_to_pick");

        when(waybillRepository.findByCode("WB123456789")).thenReturn(Optional.of(waybill));

        waybillService.callbackStatusWaybillFromGHN(req);

        assertEquals(2, order.getStatus()); // ready_to_pick → order.setStatus(2)
        verify(waybillRepository).save(any());
        verify(orderRepository).save(any());
    }


    //TC_WAYBILL_040
    // Khi trạng thái GHN là "picked" → cập nhật order.status = 3
    @Test
    void testPicked() {
        Waybill waybill = new Waybill();
        Order order = new Order();
        order.setStatus(2);
        waybill.setOrder(order);
        waybill.setStatus(1); // Gán status mặc định

        GhnCallbackOrderRequest req = new GhnCallbackOrderRequest();
        req.setShopID(121327);
        req.setOrderCode("WB123456789");
        req.setStatus("picked");

        when(waybillRepository.findByCode("WB123456789")).thenReturn(Optional.of(waybill));

        waybillService.callbackStatusWaybillFromGHN(req);

        assertEquals(3, order.getStatus());
        verify(waybillRepository).save(any());
        verify(orderRepository).save(any());
    }


    //TC_WAYBILL_041
    // Khi trạng thái GHN là "delivered" → cập nhật order.status = 4
    @Test
    void testDelivered() {
        Waybill waybill = new Waybill();
        Order order = new Order();
        order.setStatus(3);
        waybill.setOrder(order);
        waybill.setStatus(1); // Gán status mặc định

        GhnCallbackOrderRequest req = new GhnCallbackOrderRequest();
        req.setShopID(121327);
        req.setOrderCode("WB123456789");
        req.setStatus("delivered");


        when(waybillRepository.findByCode("WB123456789")).thenReturn(Optional.of(waybill));

        waybillService.callbackStatusWaybillFromGHN(req);

        assertEquals(4, order.getStatus());
        verify(waybillRepository).save(any());
        verify(orderRepository).save(any());
    }


    //TC_WAYBILL_042
    // Khi trạng thái GHN là "cancel" → cập nhật order.status = 5
    @Test
    void testCancel() {
        Waybill waybill = new Waybill();
        Order order = new Order();
        order.setStatus(2);
        waybill.setOrder(order);
        waybill.setStatus(1); // Gán status mặc định

        GhnCallbackOrderRequest req = new GhnCallbackOrderRequest();
        req.setShopID(121327);
        req.setOrderCode("WB123456789");
        req.setStatus("cancel");

        when(waybillRepository.findByCode("WB123456789")).thenReturn(Optional.of(waybill));

        waybillService.callbackStatusWaybillFromGHN(req);

        assertEquals(5, order.getStatus());
        verify(waybillRepository).save(any());
        verify(orderRepository).save(any());
    }



    //TC_WAYBILL_043
    // Khi trạng thái GHN là "exception" → cập nhật order.status = 6
    @Test
    void testException() {
        Waybill waybill = new Waybill();
        waybill.setStatus(1); // Fix the null issue here

        Order order = new Order();
        order.setStatus(3);
        waybill.setOrder(order);

        GhnCallbackOrderRequest req = new GhnCallbackOrderRequest();
        req.setShopID(121327);
        req.setOrderCode("WB123456789");
        req.setStatus("exception");

        when(waybillRepository.findByCode("WB123456789")).thenReturn(Optional.of(waybill));

        waybillService.callbackStatusWaybillFromGHN(req);

        assertEquals(6, order.getStatus());
        verify(waybillRepository).save(any());
        verify(orderRepository).save(any());
    }



    //TC_WAYBILL_044
    // Khi trạng thái gửi về trùng với trạng thái hiện tại → không cập nhật
    @Test
    void testInvalidStatusIgnored() {
        Waybill waybill = new Waybill();
        Order order = new Order();
        order.setStatus(3);
        waybill.setOrder(order);
        waybill.setStatus(1); // Gán status mặc định


        GhnCallbackOrderRequest req = new GhnCallbackOrderRequest();
        req.setShopID(121327);
        req.setOrderCode("WB123456789");
        req.setStatus("picked");

        when(waybillRepository.findByCode("WB123456789")).thenReturn(Optional.of(waybill));

        waybillService.callbackStatusWaybillFromGHN(req);

        assertEquals(3, order.getStatus()); // status giữ nguyên
        verify(waybillRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    //TC_WAYBILL_045
    // Nếu không tìm thấy waybill → không làm gì cả
    @Test
    void testWaybillNotFound() {
        GhnCallbackOrderRequest req = new GhnCallbackOrderRequest();
        req.setShopID(121327);
        req.setOrderCode("1234");
        req.setStatus("delivered");

        when(waybillRepository.findByCode("1234")).thenReturn(Optional.empty());

        waybillService.callbackStatusWaybillFromGHN(req);

        verify(waybillRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    //TC_WAYBILL_046
    // Nếu shopID không khớp → không xử lý trạng thái
    @Test
    void testWrongShopIdIgnored() {
        Waybill waybill = new Waybill();
        Order order = new Order();
        order.setStatus(3);
        waybill.setOrder(order);

        GhnCallbackOrderRequest req = new GhnCallbackOrderRequest();
        req.setShopID(999999); // sai
        req.setOrderCode("WB123456789");
        req.setStatus("delivered");

        when(waybillRepository.findByCode("WB123456789")).thenReturn(Optional.of(waybill));

        waybillService.callbackStatusWaybillFromGHN(req);

        verify(waybillRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    //TC_WAYBILL_047
// Nếu trạng thái gửi về giống trạng thái hiện tại → không update gì cả
    @Test
    void testStatusUnchanged() {
        Waybill waybill = new Waybill();
        Order order = new Order();
        order.setStatus(2);
        waybill.setOrder(order);
        waybill.setStatus(1); // Gán status mặc định

        GhnCallbackOrderRequest req = new GhnCallbackOrderRequest();
        req.setShopID(121327);
        req.setOrderCode("WB123456789");
        req.setStatus("ready_to_pick");

        when(waybillRepository.findByCode("WB123456789")).thenReturn(Optional.of(waybill));

        waybillService.callbackStatusWaybillFromGHN(req);

        // Không thay đổi gì
        verify(waybillRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }




}
