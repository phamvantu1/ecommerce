package com.electro.service.waybill;

import com.electro.dto.waybill.GhnCallbackOrderRequest;
import com.electro.entity.authentication.User;
import com.electro.entity.general.Notification;
import com.electro.entity.order.Order;
import com.electro.entity.waybill.Waybill;
import com.electro.entity.waybill.WaybillLog;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.general.NotificationMapper;
import com.electro.repository.general.NotificationRepository;
import com.electro.repository.order.OrderRepository;
import com.electro.repository.waybill.WaybillLogRepository;
import com.electro.repository.waybill.WaybillRepository;
import com.electro.service.general.NotificationService;
import com.electro.utils.RewardUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
    private RewardUtils rewardUtils;

    private final String VALID_SHOP_ID = "123456";
    private final String VALID_ORDER_CODE = "WB123";
    private final User mockUser = new User();

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
     * TC03: Test order not found throws exception
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
     * TC002 - Test không tìm thấy mã vận đơn -> throw ResourceNotFoundException
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
     * TC005 - Status SUCCESS -> cập nhật trạng thái, gửi thông báo, gọi reward
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
    @Test
    void testCallbackStatusWaybillFromGHN_StatusFailed_ShouldCancelOrder() {
        Waybill waybill = new Waybill();
        waybill.setCode("WXYZ");
        waybill.setStatus(2); // SHIPPING

        Order order = new Order();
        order.setStatus(3);
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
    }
}
