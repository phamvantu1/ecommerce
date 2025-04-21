package com.electro.controller.client;

import com.electro.constant.AppConstants;
import com.electro.dto.ListResponse;
import com.electro.dto.client.ClientOrderDetailResponse;
import com.electro.dto.client.ClientSimpleOrderResponse;
import com.electro.entity.order.Order;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.client.ClientOrderMapper;
import com.electro.repository.order.OrderRepository;
import com.electro.service.order.OrderService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientOrderControllerTest {

    @InjectMocks
    private ClientOrderController controller;



    @Mock
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ClientOrderMapper clientOrderMapper;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }



    // Test lấy danh sách đơn hàng thành công khi không có filter
// Input: username = "john_doe", page = 1, size = 10, sort = "createdAt,desc", filter = null
// Expected: HTTP 200, trả về 1 đơn hàng được ánh xạ thành công sang response

    //TCG01
    @Test
    void testGetAllOrders_SuccessWithoutFilter() {
        // Arrange
        String username = "john_doe";
        when(authentication.getName()).thenReturn(username);

        Order mockOrder = new Order(); // Tùy chỉnh nếu có constructor
        ClientSimpleOrderResponse mockResponse = new ClientSimpleOrderResponse(); // Tùy chỉnh nếu cần

        Page<Order> page = new PageImpl<>(List.of(mockOrder));
        when(orderRepository.findAllByUsername(eq(username), anyString(), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        when(clientOrderMapper.entityToResponse(mockOrder)).thenReturn(mockResponse);

        // Act
        ResponseEntity<ListResponse<ClientSimpleOrderResponse>> response = controller.getAllOrders(
                authentication, 1, 10, "createdAt,desc", null
        );

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }


    // Test lấy danh sách đơn hàng thành công khi có filter hợp lệ
// Input: username = "john_doe", page = 1, size = 10, sort = "createdAt,desc", filter = "status:DELIVERED"
// Expected: HTTP 200, trả về 1 đơn hàng đã lọc theo filter
    //TCG02
    @Test
    void testGetAllOrders_SuccessWithFilter() {
        String username = "john_doe";
        String filter = "status:DELIVERED";
        when(authentication.getName()).thenReturn(username);

        Order mockOrder = new Order();
        ClientSimpleOrderResponse mockResponse = new ClientSimpleOrderResponse();

        Page<Order> page = new PageImpl<>(List.of(mockOrder));
        when(orderRepository.findAllByUsername(eq(username), anyString(), eq(filter), any(PageRequest.class)))
                .thenReturn(page);

        when(clientOrderMapper.entityToResponse(mockOrder)).thenReturn(mockResponse);

        ResponseEntity<ListResponse<ClientSimpleOrderResponse>> response = controller.getAllOrders(
                authentication, 1, 10, "createdAt,desc", filter
        );

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }


    // Test lấy danh sách đơn hàng thành công khi có filter hợp lệ
// Input: username = "john_doe", page = 1, size = 10, sort = "createdAt,desc", filter = "status:DELIVERED"
// Expected: HTTP 200, trả về 1 đơn hàng đã lọc theo filter
    //TCG03
    @Test
    void testGetAllOrders_EmptyOrderList() {
        String username = "john_doe";
        when(authentication.getName()).thenReturn(username);

        Page<Order> page = new PageImpl<>(List.of()); // Trống
        when(orderRepository.findAllByUsername(eq(username), anyString(), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        ResponseEntity<ListResponse<ClientSimpleOrderResponse>> response = controller.getAllOrders(
                authentication, 1, 10, "createdAt,desc", null
        );

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
    }


    // Test trường hợp truyền vào giá trị page < 0 (không hợp lệ)
// Input: page = -1
// Expected: HTTP 400 Bad Request
    //TCG04
    @Test
    void testGetAllOrders_WithNegativePage() {
        String username = "john_doe";
        when(authentication.getName()).thenReturn(username);
        Page<Order> page = new PageImpl<>(List.of()); // Trống
        when(orderRepository.findAllByUsername(eq(username), anyString(), isNull(), any(PageRequest.class)))
                .thenReturn(page);


        // Trường hợp page < 0
        ResponseEntity<ListResponse<ClientSimpleOrderResponse>> response = controller.getAllOrders(
                authentication, -1, 10, "createdAt,desc", null
        );

        assertThat(response.getStatusCodeValue()).isEqualTo(400); // Bad Request
    }


    // Test trường hợp truyền vào giá trị size < 0 (không hợp lệ)
// Input: size = -10
// Expected: HTTP 400 Bad Request
    //TCG05
    @Test
    void testGetAllOrders_WithNegativeSize() {
        String username = "john_doe";
        when(authentication.getName()).thenReturn(username);

        // Trường hợp size < 0
        ResponseEntity<ListResponse<ClientSimpleOrderResponse>> response = controller.getAllOrders(
                authentication, 1, -10, "createdAt,desc", null
        );

        assertThat(response.getStatusCodeValue()).isEqualTo(400); // Bad Request
    }



    // Test với size rất lớn (giá trị lớn hơn giới hạn bình thường, ví dụ 1000)
// Input: size = 1000
// Expected: HTTP 200, xử lý thành công ()
    //TCG06
    @Test
    void testGetAllOrders_WithLargeSize() {
        String username = "john_doe";
        when(authentication.getName()).thenReturn(username);
        Page<Order> page = new PageImpl<>(List.of()); // Trống
        when(orderRepository.findAllByUsername(eq(username), anyString(), isNull(), any(PageRequest.class)))
                .thenReturn(page);


        // Trường hợp size quá lớn
        ResponseEntity<ListResponse<ClientSimpleOrderResponse>> response = controller.getAllOrders(
                authentication, 1, 1000, "createdAt,desc", null
        );

        assertThat(response.getStatusCodeValue()).isEqualTo(200); // Success

    }


    // Test khi truyền vào sort không hợp lệ (không đúng định dạng "field,direction")
// Input: sort = "invalidSort"
// Expected: HTTP 400 Bad Request
    //TCG07
    @Test
    void testGetAllOrders_WithInvalidSort() {
        String username = "john_doe";
        when(authentication.getName()).thenReturn(username);
        Page<Order> page = new PageImpl<>(List.of()); // Trống
        when(orderRepository.findAllByUsername(eq(username), anyString(), isNull(), any(PageRequest.class)))
                .thenReturn(page);


        // Trường hợp sort không hợp lệ
        ResponseEntity<ListResponse<ClientSimpleOrderResponse>> response = controller.getAllOrders(
                authentication, 1, 10, "invalidSort", null
        );

        assertThat(response.getStatusCodeValue()).isEqualTo(400); // Bad Request
    }


    // Test khi truyền vào filter không hợp lệ (ví dụ không đúng định dạng hoặc không được hỗ trợ)
// Input: filter = "invalidFilter"
// Expected: HTTP 400 Bad Request
// *Lưu ý*: Logic kiểm tra tính hợp lệ của filter nằm trong controller (không cần mock repository)
    //TCG08
    @Test
    void testGetAllOrders_WithInvalidFilter() {
        String username = "john_doe";
        when(authentication.getName()).thenReturn(username);

        // Trường hợp filter không hợp lệ
        String invalidFilter = "invalidFilter";

        ResponseEntity<ListResponse<ClientSimpleOrderResponse>> response = controller.getAllOrders(
                authentication, 1, 10, "createdAt,desc", invalidFilter
        );

        assertThat(response.getStatusCodeValue()).isEqualTo(400); // Bad Request
    }


    // Test khi không có đơn hàng nào phù hợp với filter được truyền vào
// Input: filter = "status:DELIVERED", dữ liệu trả về là danh sách trống
// Expected: HTTP 200, trả về danh sách rỗng
    //TCG09
    @Test
    void testGetAllOrders_EmptyOrderList_WithFilter() {
        String username = "john_doe";
        String filter = "status:DELIVERED";
        when(authentication.getName()).thenReturn(username);

        Page<Order> page = new PageImpl<>(List.of()); // Trống khi có filter
        when(orderRepository.findAllByUsername(eq(username), anyString(), eq(filter), any(PageRequest.class)))
                .thenReturn(page);

        ResponseEntity<ListResponse<ClientSimpleOrderResponse>> response = controller.getAllOrders(
                authentication, 1, 10, "createdAt,desc", filter
        );

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
    }


    // TCD01 - Mục tiêu: Trả về chi tiết đơn hàng khi mã hợp lệ
    // Input: code = "ORDER123"
    // Expected Output: HTTP 200 OK + ResponseEntity chứa thông tin chi tiết đơn hàng
    // Ghi chú: Đây là trường hợp thành công
    @Test
    void testGetOrder_Success() {
        // Arrange
        String code = "ORDER123";
        Order mockOrder = new Order();
        ClientOrderDetailResponse mockResponse = new ClientOrderDetailResponse();

        when(orderRepository.findByCode(code)).thenReturn(Optional.of(mockOrder));
        when(clientOrderMapper.entityToDetailResponse(mockOrder)).thenReturn(mockResponse);

        // Act
        ResponseEntity<ClientOrderDetailResponse> response = controller.getOrder(code);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(mockResponse);
    }

    // TCD02 - Mục tiêu: Ném ResourceNotFoundException khi mã không tồn tại
    // Input: code = "INVALID123"
    // Expected Output: Ném ResourceNotFoundException
    // Ghi chú: Trường hợp lỗi khi mã không tồn tại trong DB
    @Test
    void testGetOrder_NotFound() {
        // Arrange
        String code = "INVALID123";
        when(orderRepository.findByCode(code)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> controller.getOrder(code))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found") // hoặc chứa mã code nếu cần
                .hasMessageContaining(code);
    }



    // huy don hang ton tai .
    // thanh cong
    // TCH01
    @Test
    void testCancelOrder_Success() {
        // Arrange
        String code = "ORDER123";

        // Giả lập không ném exception khi gọi cancelOrder()
        doNothing().when(orderService).cancelOrder(code);

        // Act
        ResponseEntity<ObjectNode> response = controller.cancelOrder(code);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Đảm bảo status code là 200
        assertNotNull(response.getBody()); // Đảm bảo body không null
        assertTrue(response.getBody().isObject()); // Body là kiểu ObjectNode
    }

    // huy don hang khong ton tai
    // khong thanh cong
    // TCH02
    @Test
    void testCancelOrder_NotFound() {
        // Arrange
        String code = "INVALID123";
        doThrow(new ResourceNotFoundException("Order", "code", code))
                .when(orderService).cancelOrder(code);

        // Act & Assert
        assertThatThrownBy(() -> controller.cancelOrder(code))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order")
                .hasMessageContaining(code);
    }


}
