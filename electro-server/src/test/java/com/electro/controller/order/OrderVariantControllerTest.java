package com.electro.controller.order;

import com.electro.dto.order.OrderVariantKeyRequest;
import com.electro.entity.order.OrderVariantKey;
import com.electro.exception.ResourceNotFoundException;
import com.electro.service.inventory.OrderVariantService;
import com.electro.service.order.OrderService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
public class OrderVariantControllerTest {


    private OrderVariantController orderVariantController;
    private OrderVariantService orderVariantService;

    @BeforeEach
    void setUp() {

        orderVariantService = Mockito.mock(OrderVariantService.class);
        orderVariantController = new OrderVariantController(orderVariantService);

    }

    //Kiểm tra xóa thành công đơn variant với mã hợp lệ
    //
    // TC_ORDER_007
    @Test
    void TC01_deleteOrderVariant_ShouldReturnNoContent_WhenValidOrderAndVariantIds() {
        // Arrange
        Long orderId = 1L;
        Long variantId = 2L;
        OrderVariantKey id = new OrderVariantKey(orderId, variantId);

        // Giả lập service xóa thành công
        doNothing().when(orderVariantService).delete(id);

        // Act
        ResponseEntity<Void> response = orderVariantController.deleteOrderVariant(orderId, variantId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(orderVariantService, times(1)).delete(id);  // Kiểm tra service đã được gọi đúng 1 lần
    }


   // TC_ORDER_008
    @Test
    void TC02_deleteOrderVariant_ShouldReturnNotFound_WhenOrderOrVariantNotExist() {
        // Arrange
        Long orderId = 999L;  // Mã đơn hàng không tồn tại
        Long variantId = 999L;  // Mã variant không tồn tại
        OrderVariantKey id = new OrderVariantKey(orderId, variantId);

        // Giả lập service ném exception ResourceNotFoundException nếu không tìm thấy
        doThrow(new ResourceNotFoundException("OrderVariant", "OrderId", orderId.toString()))
                .when(orderVariantService).delete(id);

        // Act
        ResponseEntity<Void> response = orderVariantController.deleteOrderVariant(orderId, variantId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }





    // TC_ORDER_009
    // xoa voi danh sach rong
    @Test
    void TC03_deleteOrderVariants_ShouldReturnBadRequest_WhenEmptyList() {
        // Arrange
        List<OrderVariantKeyRequest> requests = List.of();  // Danh sách rỗng

        // Act
        ResponseEntity<Void> response = orderVariantController.deleteOrderVariants(requests);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }



    // TC_ORDER_010
    // Test Case 1: Xóa thành công các OrderVariant
    // Mã: TC01
    // Mục tiêu: Kiểm tra phương thức deleteOrderVariants khi đầu vào hợp lệ, trả về HTTP NO_CONTENT
    // Input: Một danh sách OrderVariantKeyRequest với các orderId và variantId hợp lệ
    // Expected Output: HTTP status 204 (NO_CONTENT), tức là xóa thành công
    // Ghi chú: Kiểm tra xem phương thức xóa có được gọi đúng với tham số đầu vào hay không
    @Test
    void TC04_deleteOrderVariants_ShouldReturnNoContent_WhenValidOrderVariants() {
        // Arrange
        List<OrderVariantKeyRequest> requests = new ArrayList<>();
        requests.add(new OrderVariantKeyRequest(1L, 2L)); // orderId = 1, variantId = 2
        requests.add(new OrderVariantKeyRequest(1L, 3L)); // orderId = 1, variantId = 3

        List<OrderVariantKey> ids = requests.stream()
                .map(request -> new OrderVariantKey(request.getOrderId(), request.getVariantId()))
                .collect(Collectors.toList());

        // Giả lập service xóa thành công
        doNothing().when(orderVariantService).delete(ids);

        // Act
        ResponseEntity<Void> response = orderVariantController.deleteOrderVariants(requests);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode()); // Kiểm tra trạng thái HTTP trả về
        verify(orderVariantService, times(1)).delete(ids);  // Kiểm tra service đã được gọi đúng 1 lần
    }




    // TC_ORDER_011
    // Test Case 2: Danh sách rỗng, không có OrderVariant để xóa
    // Mã: TC02
    // Mục tiêu: Kiểm tra phương thức deleteOrderVariants khi danh sách đầu vào rỗng
    // Input: Danh sách OrderVariantKeyRequest rỗng
    // Expected Output: HTTP status 204 (NO_CONTENT)
    // Ghi chú: Phương thức phải trả về HTTP status 204 ngay cả khi không có gì để xóa
    @Test
    void TC05_deleteOrderVariants_ShouldReturnNoContent_WhenEmptyList() {
        // Arrange
        List<OrderVariantKeyRequest> requests = new ArrayList<>();

        // Act
        ResponseEntity<Void> response = orderVariantController.deleteOrderVariants(requests);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());  // Kiểm tra trạng thái HTTP trả về
        verify(orderVariantService, times(0)).delete((OrderVariantKey) any());  // Kiểm tra xem phương thức xóa không được gọi
    }


    // TC_ORDER_012
    // Test Case 3: Danh sách chứa OrderVariant không hợp lệ (orderId hoặc variantId null)
    // Mã: TC03
    // Mục tiêu: Kiểm tra phương thức deleteOrderVariants khi một phần tử trong danh sách có orderId hoặc variantId null
    // Input: Danh sách chứa OrderVariantKeyRequest với orderId hoặc variantId null
    // Expected Output: HTTP status 400 (BAD_REQUEST), vì đầu vào không hợp lệ
    // Ghi chú: Kiểm tra xem hệ thống xử lý đầu vào không hợp lệ như thế nào
    @Test
    void TC06_deleteOrderVariants_ShouldReturnBadRequest_WhenInvalidOrderVariant() {
        // Arrange
        List<OrderVariantKeyRequest> requests = new ArrayList<>();
        requests.add(new OrderVariantKeyRequest(null, 2L)); // orderId null
        requests.add(new OrderVariantKeyRequest(1L, null)); // variantId null

        // Act
        ResponseEntity<Void> response = orderVariantController.deleteOrderVariants(requests);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()); // Kiểm tra trạng thái HTTP trả về
        verify(orderVariantService, times(0)).delete((OrderVariantKey) any());  // Kiểm tra xem phương thức xóa không được gọi
    }


    // TC_ORDER_013
    // Test Case 4: Xóa OrderVariant thất bại do lỗi từ Service
    // Mã: TC04
    // Mục tiêu: Kiểm tra trường hợp xóa OrderVariant thất bại trong service (ví dụ: lỗi hệ thống)
    // Input: Một danh sách OrderVariantKeyRequest hợp lệ
    // Expected Output: HTTP status 500 (INTERNAL_SERVER_ERROR) nếu có lỗi xảy ra trong service
    // Ghi chú: Kiểm tra trường hợp lỗi trong khi thực hiện xóa
    @Test
    void TC07_deleteOrderVariants_ShouldReturnInternalServerError_WhenServiceFails() {
        // Arrange
        List<OrderVariantKeyRequest> requests = new ArrayList<>();
        requests.add(new OrderVariantKeyRequest(1L, 2L)); // orderId = 1, variantId = 2
        requests.add(new OrderVariantKeyRequest(1L, 3L)); // orderId = 1, variantId = 3

        List<OrderVariantKey> ids = requests.stream()
                .map(request -> new OrderVariantKey(request.getOrderId(), request.getVariantId()))
                .collect(Collectors.toList());

        // Giả lập service ném lỗi
        doThrow(new RuntimeException("Error during deletion")).when(orderVariantService).delete(ids);

        // Act
        ResponseEntity<Void> response = orderVariantController.deleteOrderVariants(requests);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()); // Kiểm tra trạng thái HTTP trả về
        verify(orderVariantService, times(1)).delete(ids);  // Kiểm tra service đã được gọi đúng 1 lần
    }





}
