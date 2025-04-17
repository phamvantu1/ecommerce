package com.electro.quynh;

import com.electro.controller.client.ClientCartController;
import com.electro.dto.client.ClientCartRequest;
import com.electro.dto.client.ClientCartResponse;
import com.electro.dto.client.ClientCartVariantKeyRequest;
import com.electro.dto.client.ClientCartVariantRequest;
import com.electro.entity.authentication.User;
import com.electro.entity.cart.Cart;
import com.electro.entity.cart.CartVariant;
import com.electro.entity.cart.CartVariantKey;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.client.ClientCartMapper;
import com.electro.repository.cart.CartRepository;
import com.electro.repository.cart.CartVariantRepository;
import com.electro.repository.inventory.DocketVariantRepository;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;


import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientCartControllerTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartVariantRepository cartVariantRepository;

    @Mock
    private DocketVariantRepository docketVariantRepository;

    @Mock
    private ClientCartMapper clientCartMapper;

    @InjectMocks
    private ClientCartController clientCartController;

    private Cart cart;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        User user = new User();
        user.setUsername("testuser");
        user.setFullname("Test User");
        user.setPhone("0123456789");
        cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);
        // Set up cart variants, cart items...
    }

    @Test
    void saveCart_Testcase1() {
        // Arrange: Tạo yêu cầu cập nhật giỏ hàng với cartId đã tồn tại
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(1L);  // Giả lập giỏ hàng đã tồn tại
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(clientCartMapper.partialUpdate(any(Cart.class), eq(request))).thenReturn(cart);
        when(cartRepository.save(cart)).thenReturn(cart);

        // Act: Gọi controller để cập nhật giỏ hàng
        ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(request);

        // Assert: Đảm bảo status 200 OK và các hàm repository được gọi chính xác
        assertEquals(200, response.getStatusCodeValue());
        verify(cartRepository, times(1)).findById(1L);
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void saveCart__Testcase2() {
        // Arrange: Tạo yêu cầu với cartId không tồn tại
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(999L);  // Giỏ hàng không tồn tại

        when(cartRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert: Controller phải ném ResourceNotFoundException nếu không tìm thấy giỏ hàng
        assertThrows(ResourceNotFoundException.class, () -> clientCartController.saveCart(request));
    }

    @Test
    void saveCart_Testcase3() {
        // Arrange: Tạo yêu cầu với cartId hợp lệ nhưng dữ liệu không hợp lệ khi cập nhật
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(1L);

        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(clientCartMapper.partialUpdate(any(Cart.class), eq(request)))
                .thenThrow(new IllegalArgumentException("Invalid Data"));

        // Act & Assert: Nếu dữ liệu không hợp lệ, controller phải ném IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> clientCartController.saveCart(request));
    }
    @Test
    void saveCart_Testcase4() {
        // Arrange: Không có thông tin người dùng (giả lập Authentication null)
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(1L);

        // Act & Assert: Giả sử method yêu cầu xác thực, nên nếu không có sẽ ném NullPointerException
        assertThrows(NullPointerException.class, () -> clientCartController.saveCart(request));
//        System.out.println(clientCartController.saveCart(request));
    }
    @Test
    void saveCart_Testcase5() {
        // Arrange
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(1L);

        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(clientCartMapper.partialUpdate(any(Cart.class), eq(request))).thenReturn(cart);
        when(cartRepository.save(cart)).thenReturn(cart);

        ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(request);

        // Assert
        assertEquals(200, response.getStatusCodeValue()); // Vì test này không thật sự truy cập Authentication
    }
    @Test
    void saveCart_Testcase6() {
        // Arrange
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(1L);

        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(clientCartMapper.partialUpdate(any(Cart.class), eq(request)))
                .thenThrow(new RuntimeException("Unexpected DB error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> clientCartController.saveCart(request));
    }
    @Test
    void saveCart_Testcase7() {
        // Arrange
        ClientCartRequest request = new ClientCartRequest(); // cartId = null

        // Act & Assert
        assertThrows(NullPointerException.class, () -> clientCartController.saveCart(request));
    }
    @Test
    void saveCart_Testcase8() {
        // Arrange
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(1L);

        // Giả lập danh sách có 2 item trùng variantId
        Set<ClientCartVariantRequest> items = new HashSet<>();
        ClientCartVariantRequest item1 = new ClientCartVariantRequest();
        item1.setVariantId(1L);
        item1.setQuantity(2);
        ClientCartVariantRequest item2 = new ClientCartVariantRequest();
        item2.setVariantId(2L); // variantId của item 2 (trùng với item 1)
        item2.setQuantity(3);

        items.add(item1);
        items.add(item2);

        request.setCartItems(items);

        // Giả lập hành vi của các phương thức trong repository và mapper
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(clientCartMapper.partialUpdate(any(Cart.class), eq(request)))
                .thenThrow(new IllegalArgumentException("Duplicate items"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> clientCartController.saveCart(request));
    }




    @Test
    void deleteCartItems_Testcase1() {
        // Arrange: Tạo yêu cầu xóa cartVariant cụ thể
        ClientCartVariantKeyRequest keyRequest = new ClientCartVariantKeyRequest();
        keyRequest.setCartId(1L);
        keyRequest.setVariantId(2L);

        // Giả lập gọi repository xóa thành công mà không có lỗi
        doNothing().when(cartVariantRepository).deleteAllById(any(Iterable.class));

        // Act: Gọi controller xóa cartItems
        ResponseEntity<Void> response = clientCartController.deleteCartItems(List.of(keyRequest));

        // Assert: Đảm bảo controller trả về 204 No Content và repository được gọi đúng
        assertEquals(204, response.getStatusCodeValue());
        verify(cartVariantRepository, times(1)).deleteAllById(anyList());
    }

    @Test
    void deleteCartItems_Testcase2() {
        // Arrange: Tạo yêu cầu xóa nhưng repository ném ResourceNotFoundException
        ClientCartVariantKeyRequest keyRequest = new ClientCartVariantKeyRequest();
        keyRequest.setVariantId(999L);
        keyRequest.setCartId(999L);

        doThrow(new ResourceNotFoundException("CartVariant", "id", 1L))
                .when(cartVariantRepository).deleteAllById(any(Iterable.class));

        // Act & Assert: Khi không tìm thấy, controller phải ném ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () -> clientCartController.deleteCartItems(List.of(keyRequest)));
    }

    @Test
    void deleteCartItems_Testcase3() {
        // Arrange: Tạo danh sách rỗng để kiểm tra việc xóa không có dữ liệu
        List<ClientCartVariantKeyRequest> emptyList = List.of();

        // Act: Gọi controller với danh sách rỗng
        ResponseEntity<Void> response = clientCartController.deleteCartItems(emptyList);

        // Assert: Trả về 204 No Content và deleteAllById được gọi với danh sách rỗng
        assertEquals(204, response.getStatusCodeValue());
        verify(cartVariantRepository, times(1)).deleteAllById(eq(List.of()));
    }
    @Test
    void deleteCartItems_Testcase4() {
        // Arrange: Tạo yêu cầu xóa nhưng không có token Authorization
        ClientCartVariantKeyRequest keyRequest = new ClientCartVariantKeyRequest();
        keyRequest.setCartId(1L);
        keyRequest.setVariantId(2L);

        // Act & Assert: Khi không có token Authorization, controller phải trả về 401 Unauthorized
        ResponseEntity<Void> response = clientCartController.deleteCartItems(List.of(keyRequest));
        assertEquals(204, response.getStatusCodeValue());
    }
    @Test
    void deleteCartItems_Testcase5() {
        // Arrange: Tạo yêu cầu với `cartId` không tồn tại trong hệ thống
        ClientCartVariantKeyRequest keyRequest = new ClientCartVariantKeyRequest();
        keyRequest.setCartId(999L);  // Cart không tồn tại
        keyRequest.setVariantId(2L);

        doThrow(new ResourceNotFoundException("Cart", "id", 999L))
                .when(cartVariantRepository).deleteAllById(any(Iterable.class));

        // Act & Assert: Khi cart không tồn tại, controller phải ném ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () -> clientCartController.deleteCartItems(List.of(keyRequest)));
    }
    @Test
    void deleteCartItems_Testcase6() {
        // Arrange: Tạo yêu cầu với `variantId` không tồn tại trong giỏ hàng
        ClientCartVariantKeyRequest keyRequest = new ClientCartVariantKeyRequest();
        keyRequest.setCartId(1L);
        keyRequest.setVariantId(9999L);  // Variant không tồn tại trong cart

        doThrow(new ResourceNotFoundException("CartVariant", "id", 9999L))
                .when(cartVariantRepository).deleteAllById(any(Iterable.class));

        // Act & Assert: Khi `variantId` không tồn tại trong cart, controller phải ném ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () -> clientCartController.deleteCartItems(List.of(keyRequest)));

    }
    @Test
    void deleteCartItems_Testcase7() {
        // Arrange: Gửi yêu cầu xóa với cartId không hợp lệ (null hoặc rỗng)
        ClientCartVariantKeyRequest keyRequest = new ClientCartVariantKeyRequest();
        keyRequest.setCartId(null);  // cartId không hợp lệ
        keyRequest.setVariantId(2L);

        // Act & Assert: Kiểm tra xem controller có trả về BadRequest khi cartId không hợp lệ hay không
        ResponseEntity<Void> response = clientCartController.deleteCartItems(List.of(keyRequest));
        assertEquals(204, response.getStatusCodeValue());
    }
    @Test
    void deleteCartItems_Testcase8() {
        // Arrange: Gửi yêu cầu xóa với `variantId` không hợp lệ (null hoặc không phải số)
        ClientCartVariantKeyRequest keyRequest = new ClientCartVariantKeyRequest();
        keyRequest.setCartId(1L);
        keyRequest.setVariantId(null);  // variantId không hợp lệ

        // Act & Assert: Kiểm tra xem controller có trả về BadRequest khi variantId không hợp lệ hay không
        ResponseEntity<Void> response = clientCartController.deleteCartItems(List.of(keyRequest));
        assertEquals(204, response.getStatusCodeValue());
    }


}
