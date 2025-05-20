package com.electro.controller.waybill;

import com.electro.dto.waybill.GhnCallbackOrderRequest;
import com.electro.service.waybill.WaybillService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
@Transactional
@SpringBootTest
class WaybillControllerTest {

    private WaybillService waybillService;
    private WaybillController waybillController;

    @BeforeEach
    void setUp() {
        waybillService = Mockito.mock(WaybillService.class);
        waybillController = new WaybillController(waybillService);
    }

    /**TC_WAYBILL_055
     * TCCB01 - Mục tiêu: Kiểm tra controller gọi đúng phương thức service
     * Input: Đối tượng GhnCallbackOrderRequest hợp lệ
     * Expected Output: Phương thức service callbackStatusWaybillFromGHN được gọi đúng 1 lần
     */
    @Test
    void TC01_callbackStatusWaybillFromGHN_ShouldCallServiceWithCorrectInput() {
        // Arrange
        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setCodAmount(BigDecimal.valueOf(100000));
        request.setOrderCode("GHN123456");
        request.setDescription("Giao hàng thành công");
        request.setReason("Không có người nhận");
        request.setReasonCode("R001");
        request.setShopID(12345);
        request.setWidth(25);
        request.setWeight(1500);
        request.setStatus("delivered");

        // Act
        ResponseEntity<ObjectNode> response = waybillController.callbackStatusWaybillFromGHN(request);

        // Assert
        verify(waybillService, times(1)).callbackStatusWaybillFromGHN(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new ObjectNode(JsonNodeFactory.instance), response.getBody());
    }

    /**TC_WAYBILL_056
     * TC02 - Mục tiêu: Kiểm tra controller xử lý khi request null
     * Input: null
     * Expected Output: Ném NullPointerException hoặc xử lý tùy thuộc vào logic thực tế
     * Ghi chú: Test này giả lập nếu người dùng gửi null thay vì request body
     */
    @Test
    void TC02_callbackStatusWaybillFromGHN_ShouldThrowExceptionWhenRequestIsNull() {
        // Arrange
        GhnCallbackOrderRequest request = null;

        // Act & Assert
        try {
            waybillController.callbackStatusWaybillFromGHN(request);
        } catch (NullPointerException ex) {
            // Expected exception
            verify(waybillService, never()).callbackStatusWaybillFromGHN(any());
        }
    }

    /**
     * TC03 - Mục tiêu: Kiểm tra controller gọi service khi request thiếu orderCode
     * Input: request không có orderCode
     * Expected Output: Gọi service bình thường (vì không validate ở controller)
     */
    @Test
    void TC03_callbackStatusWaybillFromGHN_ShouldCallServiceEvenWhenOrderCodeIsMissing() {
        // Arrange
        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setStatus("delivered");

        // Act
        ResponseEntity<ObjectNode> response = waybillController.callbackStatusWaybillFromGHN(request);

        // Assert
        verify(waybillService, times(1)).callbackStatusWaybillFromGHN(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new ObjectNode(JsonNodeFactory.instance), response.getBody());
    }

    /**TC_WAYBILL_057
     * TC04 - Mục tiêu: Kiểm tra controller xử lý khi service ném exception
     * Input: GhnCallbackOrderRequest hợp lệ
     * Expected Output: Exception ném ra từ service được propagate lại
     * Ghi chú: Phù hợp khi muốn kiểm tra rollback hoặc lỗi logic
     */
    @Test
    void TC04_callbackStatusWaybillFromGHN_ShouldThrowExceptionWhenServiceFails() {
        // Arrange
        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setOrderCode("GHN999999");
        request.setStatus("lost");

        doThrow(new RuntimeException("Service error")).when(waybillService).callbackStatusWaybillFromGHN(request);

        // Act & Assert
        try {
            waybillController.callbackStatusWaybillFromGHN(request);
        } catch (RuntimeException ex) {
            assertEquals("Service error", ex.getMessage());
            verify(waybillService, times(1)).callbackStatusWaybillFromGHN(request);
        }
    }

    /**TC_WAYBILL_059
     * TC_WAYBILL_059 - Mục tiêu: Kiểm tra response luôn trả về ObjectNode rỗng và status OK
     * Input: request bất kỳ
     * Expected Output: ResponseEntity với status 404 và body là ObjectNode rỗng
     */
    @Test
    void TC05_callbackStatusWaybillFromGHN_ShouldReturn404WhenOrderNotFound() {
        // Arrange
        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setOrderCode("INVALID_ORDER");
        request.setStatus("cancelled");

        // Giả lập service ném exception khi order không tồn tại
        doThrow(new EntityNotFoundException("Order not found"))
                .when(waybillService).callbackStatusWaybillFromGHN(request);

        // Act
        ResponseEntity<ObjectNode> response = waybillController.callbackStatusWaybillFromGHN(request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Order not found", response.getBody().get("message").asText());
    }

    /**TC_WAYBILL_060
     * TCCB02 - Mục tiêu: Không gọi service nếu cân nặng vượt quá giới hạn
     * Input: weight = 1,000,000 (giới hạn là 100,000)
     * Expected: Không gọi service, HTTP 202, trả về message lỗi
     */
    @Test
    @DisplayName("TCCB07 - Trả về lỗi khi cân nặng quá lớn")
    void testOverweightCallback_ShouldReturnAcceptedWithError() {
        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setOrderCode("GHN_OVERWEIGHT");
        request.setStatus("delivered");
        request.setWeight(1_000_000);

        ResponseEntity<ObjectNode> response = waybillController.callbackStatusWaybillFromGHN(request);

        verify(waybillService, times(0)).callbackStatusWaybillFromGHN(any());
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals("Cân nặng vượt quá giới hạn cho phép", response.getBody().get("error").asText());
    }

    /**TC_WAYBILL_061
     * TCCB07 - Mục tiêu: Không gọi service nếu mã đơn hàng thiếu
     * Input: orderCode = null
     * Expected: Không gọi service, HTTP 202, trả về message lỗi
     */
    @Test
    @DisplayName("TCCB03 - Thiếu mã đơn hàng")
    void testMissingOrderCode_ShouldReturnAccepted() {
        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setStatus("delivered");
        request.setWeight(500);

        ResponseEntity<ObjectNode> response = waybillController.callbackStatusWaybillFromGHN(request);

        verify(waybillService, times(0)).callbackStatusWaybillFromGHN(any());
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals("Thiếu mã đơn hàng", response.getBody().get("error").asText());
    }

    /**TC_WAYBILL_062
     * TCCB08 - Mục tiêu:  don hang khong ton tai
     * Input: orderCode = "GHN999999"
     * Expected: trả thông bo khôgn tìm thay don hang
     */
    @Test
    @DisplayName("TCCB08 - Đơn hàng không tồn tại")
    void testNonExistentOrder() {
        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setOrderCode("GHN999999");
        request.setStatus("delivered");

        doThrow(new RuntimeException("Order not found")).when(waybillService).callbackStatusWaybillFromGHN(request);

        ResponseEntity<ObjectNode> response = waybillController.callbackStatusWaybillFromGHN(request);

        verify(waybillService, times(1)).callbackStatusWaybillFromGHN(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }


    /**TC_WAYBILL_063
     * TCCB09 - Mục tiêu:  trang thai khong dung dinh dang
     * Input: status = "lost"
     * Expected: thông báo không hỗ trợ trạng thái này
     */
    @Test
    @DisplayName("TCCB09 - Trạng thái không hỗ trợ (lost)")
    void testUnknownStatus() {
        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setOrderCode("GHN777888");
        request.setStatus("lost");
        request.setDescription("Thất lạc");

        ResponseEntity<ObjectNode> response = waybillController.callbackStatusWaybillFromGHN(request);

        verify(waybillService, times(1)).callbackStatusWaybillFromGHN(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    //TC_WAYBILL_064
    @Test
    @DisplayName("TCCB010 - COD là số âm => Trả về 400 Bad Request")
    void testNegativeCOD_ShouldReturn400() {
        // Arrange
        GhnCallbackOrderRequest request = new GhnCallbackOrderRequest();
        request.setOrderCode("GHN_NEGATIVE_COD");
        request.setCodAmount(BigDecimal.valueOf(-50000)); // Số âm
        request.setStatus("delivered");
        request.setShopID(1234);
        request.setWeight(1000);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> waybillController.callbackStatusWaybillFromGHN(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("codAmount không được là số âm", exception.getReason());
    }



}
