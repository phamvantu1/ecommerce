package com.electro.controller.order;

import com.electro.service.order.OrderService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {

    private OrderService orderService;
    private OrderController orderController;

    @BeforeEach
    void setUp() {
        orderService = Mockito.mock(OrderService.class);
        orderController = new OrderController(orderService);
    }

    /**
     * TC01 - Mục tiêu: Kiểm tra controller gọi đúng service
     * Input: Mã đơn hàng = "ABC123"
     * Expected Output: Gọi đúng 1 lần phương thức cancelOrder với mã "ABC123"
     * Ghi chú: Đảm bảo controller hoạt động đúng chức năng
     */

    // TCC01
    @Test
    void TC01_cancelOrder_ShouldCallServiceWithCorrectCode() {
        // Arrange
        String orderCode = "ABC123";

        // Act
        orderController.cancelOrder(orderCode);

        // Assert
        verify(orderService, times(1)).cancelOrder(orderCode);
    }

    /**
     * TC02 - Mục tiêu: Kiểm tra HTTP status trả về là OK (200)
     * Input: Mã đơn hàng = "ABC123"
     * Expected Output: ResponseEntity có HttpStatus = OK
     * Ghi chú: Đảm bảo API phản hồi đúng status code
     */
    // TCC02
    @Test
    void TC02_cancelOrder_ShouldReturnHttpStatusOk() {
        // Arrange
        String orderCode = "ABC123";

        // Act
        ResponseEntity<ObjectNode> response = orderController.cancelOrder(orderCode);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * TC03 - Mục tiêu: Kiểm tra response body là JSON rỗng
     * Input: Mã đơn hàng = "ABC123"
     * Expected Output: response body là ObjectNode rỗng -> {}
     * Ghi chú: Vì controller trả về new ObjectNode(JsonNodeFactory.instance)
     */
    // TCC03
    @Test
    void TC03_cancelOrder_ShouldReturnEmptyJsonBody() {
        // Arrange
        String orderCode = "ABC123";

        // Act
        ResponseEntity<ObjectNode> response = orderController.cancelOrder(orderCode);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }



    /**
     * TC01 - Mục tiêu: Kiểm tra xử lý khi mã đơn hàng rỗng
     * Input: ""
     * Expected Output: IllegalArgumentException hoặc custom exception
     */
    // TCC04
    @Test
    @DisplayName("TC04 - cancelOrder với mã đơn hàng rỗng")
    void TC01_cancelOrder_withEmptyCode_shouldThrowException() {
        // Arrange
        String emptyCode = "";

        // Mock service để ném lỗi
        doThrow(new IllegalArgumentException("Order code cannot be empty"))
                .when(orderService).cancelOrder(emptyCode);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderController.cancelOrder(emptyCode);
        });

        assertEquals("Order code cannot be empty", exception.getMessage());
    }


    /**
     * TC02 - Mục tiêu: Kiểm tra xử lý khi mã đơn hàng chứa ký tự đặc biệt
     * Input: "@#%!INVALID"
     * Expected Output: IllegalArgumentException hoặc custom exception
     */
    // TCC05
    @Test
    @DisplayName("TC05 - cancelOrder với mã có ký tự đặc biệt")
    void TC02_cancelOrder_withSpecialChars_shouldThrowException() {
        // Arrange
        String invalidCode = "@#%!INVALID";

        doThrow(new IllegalArgumentException("Invalid order code format"))
                .when(orderService).cancelOrder(invalidCode);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderController.cancelOrder(invalidCode);
        });

        assertEquals("Invalid order code format", exception.getMessage());
    }


    /**
     * TC03 - Mục tiêu: Kiểm tra xử lý khi mã đơn hàng là null
     * Input: null
     * Expected Output: NullPointerException hoặc custom exception
     */
    // TCC06
    @Test
    @DisplayName("TC06 - cancelOrder với mã null")
    void TC03_cancelOrder_withNullCode_shouldThrowException() {
        // Arrange
        String nullCode = null;

        doThrow(new NullPointerException("Order code must not be null"))
                .when(orderService).cancelOrder(nullCode);

        // Act & Assert
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            orderController.cancelOrder(nullCode);
        });

        assertEquals("Order code must not be null", exception.getMessage());
    }


}
