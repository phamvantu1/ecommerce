package com.electro.truongpa;

// Import các lớp DTO, entity, repository, mapper, và các thư viện cần thiết
import com.electro.dto.client.*;
import com.electro.entity.authentication.User;
import com.electro.entity.cart.Cart;
import com.electro.entity.cart.CartVariant;
import com.electro.entity.cart.CartVariantKey;
import com.electro.entity.inventory.Docket;
import com.electro.entity.inventory.DocketVariant;
import com.electro.entity.inventory.DocketVariantKey;
import com.electro.mapper.client.ClientCartMapper;
import com.electro.repository.authentication.UserRepository;
import com.electro.repository.cart.CartRepository;
import com.electro.repository.cart.CartVariantRepository;
import com.electro.repository.inventory.DocketVariantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest: Tải toàn bộ context của ứng dụng Spring Boot để chạy test tích hợp
// @AutoConfigureMockMvc: Tự động cấu hình MockMvc để giả lập các HTTP request
// @Transactional: Đảm bảo mỗi test chạy trong một transaction và rollback sau khi hoàn thành
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClientCartControllerTest {

    // @Autowired: Tiêm MockMvc để gửi HTTP request giả lập
    @Autowired
    private MockMvc mockMvc;

    // @Autowired: Tiêm UserRepository để truy vấn user thực tế từ database
    @Autowired
    private UserRepository userRepository;

    // @MockBean: Mock CartRepository để kiểm soát hành vi mà không gọi database thực
    @MockBean
    private CartRepository cartRepository;

    // @MockBean: Mock CartVariantRepository để kiểm soát hành vi xóa cart items
    @MockBean
    private CartVariantRepository cartVariantRepository;

    // @MockBean: Mock ClientCartMapper để kiểm soát ánh xạ giữa DTO và entity
    @MockBean
    private ClientCartMapper clientCartMapper;

    // @MockBean: Mock DocketVariantRepository để kiểm soát dữ liệu tồn kho
    @MockBean
    private DocketVariantRepository docketVariantRepository;

    // @Autowired: Tiêm ObjectMapper để chuyển đổi object thành JSON và ngược lại
    @Autowired
    private ObjectMapper objectMapper;

    // Biến instance để lưu trữ dữ liệu dùng chung cho các test
    private Cart cart; // Entity Cart giả lập
    private ClientCartResponse cartResponse; // DTO response giả lập
    private ClientCartRequest cartRequest; // DTO request giả lập
    private String jwtToken; // JWT token để xác thực request

    // @BeforeEach: Chạy trước mỗi test để thiết lập trạng thái ban đầu
    @BeforeEach
    void setUp() throws Exception {
        // Kiểm tra user "patruong" tồn tại trong database
        Optional<User> userOptional = userRepository.findByUsername("patruong");
        assertTrue(userOptional.isPresent(), "User 'patruong' phải tồn tại trong database");
        User user = userOptional.get();
        // Kiểm tra user có role CUSTOMER
        assertTrue(user.getRoles().stream().anyMatch(role -> "CUSTOMER".equals(role.getCode())),
                "User 'patruong' phải có vai trò CUSTOMER");

        // Khởi tạo Cart entity với dữ liệu giả lập
        cart = new Cart();
        cart.setId(1L); // ID giỏ hàng
        cart.setStatus(1); // Trạng thái giỏ hàng (1 = active)
        cart.setUser(user); // Liên kết với user
        cart.setCartVariants(new HashSet<>()); // Danh sách CartVariant

        // Thêm CartVariant vào giỏ hàng
        CartVariant cartVariant = new CartVariant();
        cartVariant.setCartVariantKey(new CartVariantKey(1L, 1L)); // Key gồm cartId=1, variantId=1
        cartVariant.setQuantity(2); // Số lượng sản phẩm
        cart.getCartVariants().add(cartVariant);

        // Khởi tạo ClientCartResponse (DTO trả về)
        cartResponse = new ClientCartResponse();
        cartResponse.setCartId(1L); // ID giỏ hàng
        cartResponse.setCartItems(new LinkedHashSet<>()); // Danh sách items rỗng

        // Khởi tạo ClientCartRequest (DTO gửi lên)
        cartRequest = new ClientCartRequest();
        cartRequest.setCartId(null); // cartId=null để tạo mới giỏ hàng
        cartRequest.setUserId(user.getId()); // ID của user
        cartRequest.setStatus(1); // Trạng thái giỏ hàng
        cartRequest.setUpdateQuantityType(UpdateQuantityType.OVERRIDE); // Chế độ cập nhật số lượng
        ClientCartVariantRequest variantRequest = new ClientCartVariantRequest();
        variantRequest.setVariantId(1L); // ID variant
        variantRequest.setQuantity(2); // Số lượng
        cartRequest.setCartItems(Set.of(variantRequest)); // Danh sách items

        // Thực hiện login để lấy JWT token
        String loginRequest = "{\"username\":\"patruong\",\"password\":\"123456\"}";
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();

        // Trích xuất JWT token từ response
        String responseContent = loginResult.getResponse().getContentAsString();
        jwtToken = objectMapper.readTree(responseContent).get("token").asText();
    }

    // Tests cho GET /client-api/carts
    // Kiểm tra lấy giỏ hàng thành công
    @Test
    void testGetCart_Success() throws Exception {
        // Arrange: Thiết lập mock
        // Mock cartRepository trả về giỏ hàng
        when(cartRepository.findByUsername("patruong")).thenReturn(Optional.of(cart));
        // Mock mapper trả về DTO response
        when(clientCartMapper.entityToResponse(cart)).thenReturn(cartResponse);

        // Act & Assert: Gửi GET request và kiểm tra
        mockMvc.perform(get("/client-api/carts")
                        .header("Authorization", "Bearer " + jwtToken) // Thêm JWT token
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Kiểm tra mã 200
                .andExpect(jsonPath("$.cartId").value(1L)); // Kiểm tra cartId

        // Verify: Kiểm tra các phương thức mock được gọi
        verify(cartRepository).findByUsername("patruong");
        verify(clientCartMapper).entityToResponse(cart);
    }

    // Kiểm tra lấy giỏ hàng không tìm thấy
    @Test
    void testGetCart_NotFound() throws Exception {
        // Arrange: Mock cartRepository trả về Optional rỗng
        when(cartRepository.findByUsername("patruong")).thenReturn(Optional.empty());

        // Act & Assert: Gửi GET request và kiểm tra
        mockMvc.perform(get("/client-api/carts")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // API trả về 200 với body rỗng
                .andExpect(content().json("{}")); // Kiểm tra body rỗng

        // Verify: Kiểm tra phương thức mock
        verify(cartRepository).findByUsername("patruong");
        verify(clientCartMapper, never()).entityToResponse(any()); // Mapper không được gọi
    }

    // Kiểm tra lấy giỏ hàng khi không có JWT token
    @Test
    void testGetCart_Unauthorized() throws Exception {
        // Act & Assert: Gửi GET request không có token
        mockMvc.perform(get("/client-api/carts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // Kiểm tra mã 401

        // Verify: Không gọi repository hay mapper
        verify(cartRepository, never()).findByUsername(any());
        verify(clientCartMapper, never()).entityToResponse(any());
    }

    // Tests cho POST /client-api/carts
    // Kiểm tra tạo giỏ hàng mới thành công
    @Test
    void testSaveCart_CreateNew_Success() throws Exception {
        // Arrange: Thiết lập mock
        // Mock mapper chuyển request thành entity
        when(clientCartMapper.requestToEntity(cartRequest)).thenReturn(cart);

        // Mock tồn kho đủ để vượt qua kiểm tra
        DocketVariant docketVariant = new DocketVariant();
        docketVariant.setQuantity(10); // Số lượng tồn kho đủ (>= 2)
        DocketVariantKey key = new DocketVariantKey();
        key.setVariantId(1L); // Khớp với variantId trong cartRequest
        key.setDocketId(1L); // Giả lập docketId
        docketVariant.setDocketVariantKey(key);
        Docket docket = new Docket();
        docket.setType(1); // Nhập kho (type=1)
        docket.setStatus(3); // Hoàn thành (status=3 để tính vào canBeSold)
        docketVariant.setDocket(docket);
        when(docketVariantRepository.findByVariantId(1L)).thenReturn(List.of(docketVariant));

        // Mock lưu giỏ hàng và trả về response
        when(cartRepository.save(cart)).thenReturn(cart);
        when(clientCartMapper.entityToResponse(cart)).thenReturn(cartResponse);

        // Act & Assert: Gửi POST request và kiểm tra
        mockMvc.perform(post("/client-api/carts")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk()) // Kiểm tra mã 200
                .andExpect(jsonPath("$.cartId").value(1L)) // Kiểm tra cartId
                .andExpect(jsonPath("$.cartItems").isArray()); // Kiểm tra cartItems là mảng

        // Verify: Kiểm tra các phương thức mock được gọi
        verify(clientCartMapper).requestToEntity(cartRequest);
        verify(docketVariantRepository).findByVariantId(1L);
        verify(cartRepository).save(cart);
        verify(clientCartMapper).entityToResponse(cart);
    }

    // Kiểm tra cập nhật giỏ hàng hiện có thành công
    @Test
    void testSaveCart_UpdateExisting_Success() throws Exception {
        // Arrange: Thiết lập mock
        cartRequest.setCartId(1L); // cartId=1 để cập nhật
        // Mock tìm giỏ hàng theo ID
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        // Mock cập nhật giỏ hàng từ request
        when(clientCartMapper.partialUpdate(cart, cartRequest)).thenReturn(cart);

        // Mock tồn kho đủ
        DocketVariant docketVariant = new DocketVariant();
        docketVariant.setQuantity(10);
        DocketVariantKey key = new DocketVariantKey();
        key.setVariantId(1L);
        key.setDocketId(1L);
        docketVariant.setDocketVariantKey(key);
        Docket docket = new Docket();
        docket.setType(1);
        docket.setStatus(3);
        docketVariant.setDocket(docket);
        when(docketVariantRepository.findByVariantId(1L)).thenReturn(List.of(docketVariant));

        // Mock lưu giỏ hàng và trả về response
        when(cartRepository.save(cart)).thenReturn(cart);
        when(clientCartMapper.entityToResponse(cart)).thenReturn(cartResponse);

        // Act & Assert: Gửi POST request và kiểm tra
        mockMvc.perform(post("/client-api/carts")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1L));

        // Verify: Kiểm tra các phương thức mock được gọi
        verify(cartRepository).findById(1L);
        verify(clientCartMapper).partialUpdate(cart, cartRequest);
        verify(docketVariantRepository).findByVariantId(1L);
        verify(cartRepository).save(cart);
        verify(clientCartMapper).entityToResponse(cart);
    }

    // Kiểm tra cập nhật giỏ hàng không tồn tại
    @Test
    void testSaveCart_CartNotFound_ThrowsException() throws Exception {
        // Arrange: Mock giỏ hàng không tồn tại
        cartRequest.setCartId(999L);
        when(cartRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert: Gửi POST request và kiểm tra
        mockMvc.perform(post("/client-api/carts")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isNotFound()) // Kiểm tra mã 404
                .andExpect(jsonPath("$.message").value("Cart not found with id: '999'")); // Kiểm tra thông báo

        // Verify: Kiểm tra các phương thức mock
        verify(cartRepository).findById(999L);
        verify(clientCartMapper, never()).partialUpdate(any(), any());
        verify(cartRepository, never()).save(any());
    }

    // Kiểm tra tạo giỏ hàng khi vượt quá tồn kho
    @Test
    void testSaveCart_InventoryExceeds_ThrowsException() throws Exception {
        // Arrange: Mock giỏ hàng và thiếu tồn kho
        when(clientCartMapper.requestToEntity(cartRequest)).thenReturn(cart);
        // Mock không có dữ liệu tồn kho
        when(docketVariantRepository.findByVariantId(1L)).thenReturn(Collections.emptyList());

        // Act & Assert: Gửi POST request và kiểm tra
        mockMvc.perform(post("/client-api/carts")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().is5xxServerError()) // Kiểm tra mã 5xx
                .andExpect(jsonPath("$.message").value("Variant quantity cannot greater than variant inventory")); // Kiểm tra thông báo

        // Verify: Kiểm tra các phương thức mock
        verify(clientCartMapper).requestToEntity(cartRequest);
        verify(docketVariantRepository).findByVariantId(1L);
        verify(cartRepository, never()).save(any());
    }

    // Kiểm tra tạo giỏ hàng khi không có JWT token
    @Test
    void testSaveCart_Unauthorized() throws Exception {
        // Act & Assert: Gửi POST request không có token
        mockMvc.perform(post("/client-api/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isUnauthorized()); // Kiểm tra mã 401

        // Verify: Không gọi mapper hay repository
        verify(clientCartMapper, never()).requestToEntity(any());
        verify(cartRepository, never()).save(any());
    }

    // Tests cho DELETE /client-api/carts
    // Kiểm tra xóa items trong giỏ hàng thành công
    @Test
    void testDeleteCartItems_Success() throws Exception {
        // Arrange: Thiết lập request xóa
        ClientCartVariantKeyRequest keyRequest = new ClientCartVariantKeyRequest();
        keyRequest.setCartId(1L);
        keyRequest.setVariantId(1L);
        List<ClientCartVariantKeyRequest> idRequests = List.of(keyRequest);
        // Mock hành vi xóa
        doNothing().when(cartVariantRepository).deleteAllById(anyList());

        // Act & Assert: Gửi DELETE request và kiểm tra
        mockMvc.perform(delete("/client-api/carts")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(idRequests)))
                .andExpect(status().isNoContent()); // Kiểm tra mã 204

        // Verify: Kiểm tra phương thức xóa được gọi
        verify(cartVariantRepository).deleteAllById(anyList());
    }

    // Kiểm tra xóa items khi không có JWT token
    @Test
    void testDeleteCartItems_Unauthorized() throws Exception {
        // Arrange: Thiết lập request xóa
        ClientCartVariantKeyRequest keyRequest = new ClientCartVariantKeyRequest();
        keyRequest.setCartId(1L);
        keyRequest.setVariantId(1L);
        List<ClientCartVariantKeyRequest> idRequests = List.of(keyRequest);

        // Act & Assert: Gửi DELETE request không có token
        mockMvc.perform(delete("/client-api/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(idRequests)))
                .andExpect(status().isUnauthorized()); // Kiểm tra mã 401

        // Verify: Không gọi phương thức xóa
        verify(cartVariantRepository, never()).deleteAllById(anyList());
    }

    @Test
    void testSaveCart_CreateNew_MultipleVariants_Success() throws Exception {
        // Arrange: Tạo cartRequest với nhiều CartVariant
        ClientCartVariantRequest variantRequest1 = new ClientCartVariantRequest();
        variantRequest1.setVariantId(1L); // Variant 1 (ví dụ: iPhone 14, 128GB, Đen)
        variantRequest1.setQuantity(2);
        ClientCartVariantRequest variantRequest2 = new ClientCartVariantRequest();
        variantRequest2.setVariantId(2L); // Variant 2 (ví dụ: iPhone 14, 256GB, Trắng)
        variantRequest2.setQuantity(3);
        cartRequest.setCartItems(Set.of(variantRequest1, variantRequest2));

        // Mock Cart với nhiều CartVariant
        Cart cartWithMultipleVariants = new Cart();
        cartWithMultipleVariants.setId(1L);
        cartWithMultipleVariants.setStatus(1);
        cartWithMultipleVariants.setUser(userRepository.findByUsername("patruong").get());
        cartWithMultipleVariants.setCartVariants(new HashSet<>());
        CartVariant cartVariant1 = new CartVariant();
        cartVariant1.setCartVariantKey(new CartVariantKey(1L, 1L));
        cartVariant1.setQuantity(2);
        CartVariant cartVariant2 = new CartVariant();
        cartVariant2.setCartVariantKey(new CartVariantKey(1L, 2L));
        cartVariant2.setQuantity(3);
        cartWithMultipleVariants.getCartVariants().addAll(Set.of(cartVariant1, cartVariant2));

        // Mock mapper chuyển request thành entity
        when(clientCartMapper.requestToEntity(cartRequest)).thenReturn(cartWithMultipleVariants);

        // Mock tồn kho cho cả hai variant
        DocketVariant docketVariant1 = new DocketVariant();
        docketVariant1.setQuantity(10); // Tồn kho đủ cho variantId=1
        DocketVariantKey key1 = new DocketVariantKey();
        key1.setVariantId(1L);
        key1.setDocketId(1L);
        docketVariant1.setDocketVariantKey(key1);
        Docket docket1 = new Docket();
        docket1.setType(1); // Nhập kho
        docket1.setStatus(3); // Hoàn thành
        docketVariant1.setDocket(docket1);

        DocketVariant docketVariant2 = new DocketVariant();
        docketVariant2.setQuantity(10); // Tồn kho đủ cho variantId=2
        DocketVariantKey key2 = new DocketVariantKey();
        key2.setVariantId(2L);
        key2.setDocketId(2L);
        docketVariant2.setDocketVariantKey(key2);
        Docket docket2 = new Docket();
        docket2.setType(1);
        docket2.setStatus(3);
        docketVariant2.setDocket(docket2);

        when(docketVariantRepository.findByVariantId(1L)).thenReturn(List.of(docketVariant1));
        when(docketVariantRepository.findByVariantId(2L)).thenReturn(List.of(docketVariant2));

        // Mock lưu giỏ hàng và trả về response
        when(cartRepository.save(cartWithMultipleVariants)).thenReturn(cartWithMultipleVariants);
        when(clientCartMapper.entityToResponse(cartWithMultipleVariants)).thenReturn(cartResponse);

        // Act & Assert: Gửi POST request
        mockMvc.perform(post("/client-api/carts")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1L))
                .andExpect(jsonPath("$.cartItems").isArray()); // Kiểm tra cartItems là mảng

        // Verify: Kiểm tra các phương thức mock được gọi
        verify(clientCartMapper).requestToEntity(cartRequest);
        verify(docketVariantRepository).findByVariantId(1L);
        verify(docketVariantRepository).findByVariantId(2L);
        verify(cartRepository).save(cartWithMultipleVariants);
        verify(clientCartMapper).entityToResponse(cartWithMultipleVariants);
    }

    @Test
    void testSaveCart_InvalidQuantity_ThrowsException() throws Exception {
        // Arrange: Tạo cartRequest với quantity không hợp lệ
        ClientCartVariantRequest variantRequest = new ClientCartVariantRequest();
        variantRequest.setVariantId(1L);
        variantRequest.setQuantity(0); // Hoặc -1 để kiểm tra số âm
        cartRequest.setCartItems(Set.of(variantRequest));

        // Mock mapper (giả sử API vẫn gọi mapper trước khi validate)
        when(clientCartMapper.requestToEntity(cartRequest)).thenReturn(cart);

        // Act & Assert: Gửi POST request
        mockMvc.perform(post("/client-api/carts")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Quantity must be positive"));

        // Verify: Kiểm tra các phương thức mock
        verify(clientCartMapper).requestToEntity(cartRequest);
        verify(docketVariantRepository, never()).findByVariantId(any()); // Không kiểm tra tồn kho
        verify(cartRepository, never()).save(any()); // Không lưu giỏ hàng
    }

    @Test
    void testSaveCart_VariantNotFound_ThrowsException() throws Exception {
        // Arrange: Tạo cartRequest với variantId không tồn tại
        ClientCartVariantRequest variantRequest = new ClientCartVariantRequest();
        variantRequest.setVariantId(999L); // Variant không tồn tại
        variantRequest.setQuantity(2);
        cartRequest.setCartItems(Set.of(variantRequest));

        // Mock mapper
        when(clientCartMapper.requestToEntity(cartRequest)).thenReturn(cart);

        // Mock variant không tồn tại
        when(docketVariantRepository.findByVariantId(999L)).thenReturn(Collections.emptyList());

        // Act & Assert: Gửi POST request
        mockMvc.perform(post("/client-api/carts")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Variant with ID 999 not found"));

        // Verify: Kiểm tra các phương thức mock
        verify(clientCartMapper).requestToEntity(cartRequest);
        verify(docketVariantRepository).findByVariantId(999L);
        verify(cartRepository, never()).save(any());
    }

}