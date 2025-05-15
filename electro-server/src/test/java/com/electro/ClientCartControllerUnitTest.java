package com.electro;

import com.electro.controller.client.ClientCartController;
import com.electro.dto.client.*;
import com.electro.entity.cart.*;
import com.electro.entity.inventory.DocketVariant;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.client.ClientCartMapper;
import com.electro.repository.cart.CartRepository;
import com.electro.repository.cart.CartVariantRepository;

import com.electro.repository.inventory.DocketVariantRepository;
import com.electro.utils.InventoryUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientCartControllerUnitTest {

    @InjectMocks
    private ClientCartController clientCartController;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartVariantRepository cartVariantRepository;

    @Mock
    private ClientCartMapper clientCartMapper;
    @Mock
    private DocketVariantRepository docketVariantRepository;
    @Mock
    private Authentication authentication;
    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    //testcase 1: lấy giỏ hàng của người dùng
    @Test
    void testGetCart_WhenUserHasCart_ShouldReturnCartResponse() {
        // Chuẩn bị dữ liệu giả lập (Arrange)
        String username = "testuser"; // Tên người dùng giả lập
        Cart mockCart = new Cart(); // Tạo đối tượng Cart giả lập
        mockCart.setId(1L); // Giả lập id của giỏ hàng
        mockCart.setStatus(1); // Giả lập trạng thái giỏ hàng

        // Tạo đối tượng ClientCartResponse giả lập để trả về cho người dùng
        ClientCartResponse mockResponseDto = new ClientCartResponse();
        mockResponseDto.setCartId(1L); // Giả lập id giỏ hàng trong response
        mockResponseDto.setCartItems(null); // Giả lập giỏ hàng không có item nào

        // Tạo đối tượng ObjectNode mong muốn để so sánh kết quả trả về
        ObjectNode expectedResponse = new ObjectMapper().createObjectNode();
        expectedResponse.put("cartId", 1L); // Thêm id giỏ hàng vào ObjectNode
        expectedResponse.putNull("cartItems"); // Thêm trường "cartItems" với giá trị null

        // Giả lập các hành vi của các phương thức cần thiết
        when(authentication.getName()).thenReturn(username); // Giả lập tên người dùng từ authentication
        when(cartRepository.findByUsername(username)).thenReturn(Optional.of(mockCart)); // Giả lập việc tìm giỏ hàng theo username
        when(clientCartMapper.entityToResponse(mockCart)).thenReturn(mockResponseDto); // Giả lập việc chuyển đổi Cart entity thành ClientCartResponse
        when(objectMapper.createObjectNode()).thenReturn(expectedResponse); // Giả lập tạo ObjectNode từ ObjectMapper
        when(objectMapper.convertValue(mockResponseDto, ObjectNode.class)).thenReturn(expectedResponse); // Giả lập việc chuyển đổi ClientCartResponse thành ObjectNode

        // Thực thi: Gọi phương thức getCart để lấy giỏ hàng
        ResponseEntity<ObjectNode> response = clientCartController.getCart(authentication);

        // Kiểm tra kết quả trả về
        assertEquals(200, response.getStatusCodeValue()); // Kiểm tra mã trạng thái HTTP trả về là 200 (thành công)
        assertNotNull(response.getBody()); // Kiểm tra thân của response không null
        assertEquals(expectedResponse, response.getBody()); // Kiểm tra thân response có bằng với expectedResponse không
    }

    // Test case 2: Kiểm tra phương thức getCart trả về đối tượng rỗng khi người dùng không có giỏ hàng
    @Test
    void testGetCart_WhenCartDoesNotExist_ShouldReturnEmptyObject() {
        // Arrange: Giả lập tên người dùng không tồn tại
        String username = "nouser"; // Tên người dùng giả định

        // Giả lập phương thức lấy tên người dùng từ đối tượng authentication
        when(authentication.getName()).thenReturn(username); // Khi getName() được gọi, trả về "nouser"

        // Giả lập phương thức tìm kiếm giỏ hàng từ cartRepository, trả về Optional.empty() khi không tìm thấy giỏ hàng
        when(cartRepository.findByUsername(username)).thenReturn(Optional.empty()); // Không tìm thấy giỏ hàng cho người dùng

        // Act: Gọi phương thức getCart của clientCartController để lấy giỏ hàng
        ResponseEntity<ObjectNode> response = clientCartController.getCart(authentication);

        // Assert: Kiểm tra kết quả trả về
        assertEquals(200, response.getStatusCodeValue()); // Kiểm tra mã trạng thái HTTP là 200 (OK)
        assertNotNull(response.getBody()); // Đảm bảo body không phải là null
        assertTrue(response.getBody().isEmpty()); // Kiểm tra body là một đối tượng rỗng (không có thông tin giỏ hàng)
    }

    // Test case 3: Kiểm tra phương thức mapper entityToResponse và chuyển đổi DTO sang ObjectNode trả về đúng dữ liệu mong đợi
    @Test
    void testMapperAndConvertToObjectNode_ShouldReturnCorrectObjectNode() {
        // Arrange: Tạo dữ liệu mock cho giỏ hàng và đối tượng phản hồi
        Cart mockCart = new Cart();
        mockCart.setId(1L); // Đặt ID giỏ hàng là 1
        mockCart.setStatus(1); // Đặt trạng thái giỏ hàng là 1

        // Tạo DTO giả định cho phản hồi
        ClientCartResponse mockResponseDto = new ClientCartResponse();
        mockResponseDto.setCartId(1L); // Đặt ID giỏ hàng trong DTO là 1
        mockResponseDto.setCartItems(null); // Đặt giỏ hàng không có sản phẩm (null)

        // Tạo đối tượng ObjectNode mong đợi sau khi chuyển đổi
        ObjectNode expectedObjectNode = new ObjectMapper().createObjectNode();
        expectedObjectNode.put("cartId", 1L); // Đặt giá trị "cartId" là 1
        expectedObjectNode.putNull("cartItems"); // Đặt "cartItems" là null

        // Mock các phương thức của clientCartMapper và objectMapper
        when(clientCartMapper.entityToResponse(mockCart)).thenReturn(mockResponseDto); // Khi gọi entityToResponse, trả về mockResponseDto
        when(objectMapper.convertValue(mockResponseDto, ObjectNode.class)).thenReturn(expectedObjectNode); // Khi convert, trả về expectedObjectNode

        // Act: Gọi phương thức mapper và chuyển đổi object
        ClientCartResponse actualResponseDto = clientCartMapper.entityToResponse(mockCart); // Chuyển đổi giỏ hàng thành DTO
        ObjectNode actualObjectNode = objectMapper.convertValue(actualResponseDto, ObjectNode.class); // Chuyển đổi DTO thành ObjectNode

        // Assert: Kiểm tra các kết quả
        assertNotNull(actualResponseDto); // Đảm bảo rằng response DTO không null
        assertEquals(mockResponseDto, actualResponseDto); // Kiểm tra DTO trả về đúng như mock
        assertNotNull(actualObjectNode); // Đảm bảo ObjectNode không null
        assertEquals(expectedObjectNode, actualObjectNode); // Kiểm tra ObjectNode trả về đúng như mong đợi
    }

    // Test case 4: Kiểm tra phương thức saveCart tạo mới giỏ hàng khi cartId trong request là null
    @Test
    void testSaveCart_WhenCartIdIsNull_ShouldCreateNewCart() {
        // Arrange: Tạo request với cartId = null, giả lập yêu cầu tạo giỏ hàng mới
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(null); // Cart ID là null, chứng tỏ yêu cầu tạo giỏ hàng mới

        // Tạo đối tượng Cart và ClientCartResponse mock
        Cart cartEntity = new Cart(); // Giỏ hàng mới chưa có thông tin
        ClientCartResponse responseDto = new ClientCartResponse(); // DTO cho phản hồi

        // Mock các phương thức của clientCartMapper và cartRepository
        when(clientCartMapper.requestToEntity(request)).thenReturn(cartEntity); // Khi gọi requestToEntity, trả về cartEntity
        when(cartRepository.save(cartEntity)).thenReturn(cartEntity); // Khi lưu cartEntity, trả về chính nó
        when(clientCartMapper.entityToResponse(cartEntity)).thenReturn(responseDto); // Khi chuyển đổi entity sang response, trả về responseDto

        // Act: Gọi phương thức saveCart
        ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(request); // Gọi controller để lưu giỏ hàng

        // Assert: Kiểm tra kết quả trả về
        assertEquals(200, response.getStatusCodeValue()); // Kiểm tra mã trạng thái HTTP là 200 OK
        assertEquals(responseDto, response.getBody()); // Kiểm tra phản hồi có đúng như DTO mong đợi
    }

    // Test case 5: Kiểm tra phương thức saveCart cập nhật giỏ hàng khi cartId tồn tại và tìm thấy giỏ hàng tương ứng trong database
    @Test
    void testSaveCart_WhenCartIdExistsAndFound_ShouldUpdateCart() {
        // Arrange: Tạo request có cartId != null, nghĩa là người dùng muốn cập nhật giỏ hàng đã tồn tại
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(1L); // cartId đã tồn tại

        // Tạo mock cho giỏ hàng hiện tại trong database và giỏ hàng sau cập nhật
        Cart existingCart = new Cart(); // Cart hiện tại (tìm thấy trong DB)
        Cart updatedCart = new Cart();  // Cart sau khi mapper cập nhật từ request
        ClientCartResponse responseDto = new ClientCartResponse(); // DTO phản hồi trả về cho client

        // Mock hành vi các phụ thuộc:
        when(cartRepository.findById(1L)).thenReturn(Optional.of(existingCart)); // Giả lập tìm thấy cart
        when(clientCartMapper.partialUpdate(existingCart, request)).thenReturn(updatedCart); // Cập nhật cart
        when(cartRepository.save(updatedCart)).thenReturn(updatedCart); // Giả lập lưu cart cập nhật
        when(clientCartMapper.entityToResponse(updatedCart)).thenReturn(responseDto); // Mapper chuyển sang DTO

        // Act: Gọi phương thức saveCart để cập nhật giỏ hàng
        ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(request);

        // Assert: Kiểm tra kết quả phản hồi
        assertEquals(200, response.getStatusCodeValue()); // Kiểm tra mã HTTP 200 OK
        assertEquals(responseDto, response.getBody()); // Kiểm tra nội dung phản hồi chính xác
    }

    // Test case 6: Kiểm tra phương thức saveCart ném ResourceNotFoundException khi cartId tồn tại nhưng không tìm thấy giỏ hàng trong database
    @Test
    void testSaveCart_WhenCartIdExistsAndNotFound_ShouldThrowException() {
        // Arrange: Tạo request với cartId = 99L (giả lập cart không tồn tại trong DB)
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(99L); // cartId không tồn tại

        // Giả lập khi tìm kiếm cart trong DB thì không thấy
        when(cartRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert: Gọi saveCart và kỳ vọng sẽ ném ra ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () -> clientCartController.saveCart(request));
    }

    // Test case 7: Kiểm tra phương thức saveCart lưu giỏ hàng thành công khi số lượng sản phẩm hợp lệ (không vượt quá tồn kho)
    @Test
    void testSaveCart_WhenQuantityIsValid_ShouldSaveCart() {
        // Arrange: Chuẩn bị dữ liệu đầu vào là request có cartId = null (tạo mới giỏ hàng)
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(null);

        // Tạo giỏ hàng giả lập chứa một sản phẩm (CartVariant) với số lượng là 5
        Cart cartEntity = new Cart();
        CartVariant cartVariant = new CartVariant();
        cartVariant.setQuantity(5); // Số lượng sản phẩm đặt mua
        CartVariantKey cartVariantKey = new CartVariantKey();
        cartVariantKey.setVariantId(1L); // ID của biến thể sản phẩm
        cartVariant.setCartVariantKey(cartVariantKey);
        cartEntity.setCartVariants(Set.of(cartVariant));

        // Tạo đối tượng phản hồi giả lập sau khi lưu thành công
        ClientCartResponse responseDto = new ClientCartResponse();

        // Giả lập không có giao dịch tồn kho ban đầu (sẽ xử lý logic tồn kho sau)
        List<DocketVariant> mockTransactions = new ArrayList<>();

        // Giả lập mapper chuyển từ request -> entity
        when(clientCartMapper.requestToEntity(request)).thenReturn(cartEntity);

        // Giả lập repository truy vấn các giao dịch tồn kho theo variantId
        when(docketVariantRepository.findByVariantId(1L)).thenReturn(mockTransactions);

        // Mock static method InventoryUtils để trả về số lượng tồn kho = 10 (lớn hơn số lượng đặt)
        try (MockedStatic<InventoryUtils> mockedStatic = mockStatic(InventoryUtils.class)) {
            mockedStatic.when(() -> InventoryUtils.calculateInventoryIndices(mockTransactions))
                    .thenReturn(Map.of("canBeSold", 10)); // Tồn kho còn 10 sản phẩm

            // Giả lập lưu cart thành công
            when(cartRepository.save(cartEntity)).thenReturn(cartEntity);

            // Giả lập mapper chuyển từ entity -> DTO phản hồi
            when(clientCartMapper.entityToResponse(cartEntity)).thenReturn(responseDto);

            // Act: Gọi controller để lưu cart
            ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(request);

            // Assert: Kết quả phản hồi đúng và thành công
            assertEquals(200, response.getStatusCodeValue());         // HTTP OK
            assertEquals(responseDto, response.getBody());            // DTO phản hồi đúng

            // Verify: Kiểm tra các phương thức mock đã được gọi đúng
            verify(docketVariantRepository).findByVariantId(1L);
            verify(cartRepository).save(cartEntity);
        }
    }

    // Test case 8: Kiểm tra phương thức saveCart ném RuntimeException khi số lượng sản phẩm đặt vượt quá tồn kho
    @Test
    void testSaveCart_WhenQuantityExceedsInventory_ShouldThrowRuntimeException() {
        // Arrange: Giả lập request tạo mới giỏ hàng (cartId = null)
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(null);

        // Tạo một Cart chứa một CartVariant có số lượng vượt quá tồn kho
        Cart cartEntity = new Cart();
        CartVariant cartVariant = new CartVariant();
        cartVariant.setQuantity(15); // Số lượng đặt mua vượt quá tồn kho
        CartVariantKey cartVariantKey = new CartVariantKey();
        cartVariantKey.setVariantId(1L); // ID biến thể
        cartVariant.setCartVariantKey(cartVariantKey);
        cartEntity.setCartVariants(Set.of(cartVariant)); // Thêm vào Cart

        // Giả lập không có giao dịch tồn kho ban đầu (sẽ xử lý logic tồn kho)
        List<DocketVariant> mockTransactions = new ArrayList<>();

        // Giả lập mapper chuyển request sang entity
        when(clientCartMapper.requestToEntity(request)).thenReturn(cartEntity);

        // Giả lập repository trả về danh sách giao dịch tồn kho
        when(docketVariantRepository.findByVariantId(1L)).thenReturn(mockTransactions);

        // Mock static InventoryUtils để trả về tồn kho là 10
        try (MockedStatic<InventoryUtils> mockedStatic = mockStatic(InventoryUtils.class)) {
            mockedStatic.when(() -> InventoryUtils.calculateInventoryIndices(mockTransactions))
                    .thenReturn(Map.of("canBeSold", 10)); // Chỉ có 10 sản phẩm tồn kho

            // Act & Assert: Gọi hàm và kỳ vọng ném RuntimeException
            RuntimeException exception = assertThrows(RuntimeException.class, () -> clientCartController.saveCart(request));

            // Kiểm tra thông điệp lỗi đúng như mong đợi
            assertEquals("Variant quantity cannot greater than variant inventory", exception.getMessage());
        }
    }

    // Test case 9: Kiểm tra phương thức saveCart ném RuntimeException khi tồn kho hoặc số lượng sản phẩm âm (không hợp lệ)
    @Test
    void testSaveCart_WhenInventoryOrQuantityIsNegative_ShouldThrowRuntimeException() {
        // Arrange: Tạo một request giả lập để thêm mới giỏ hàng (cartId = null)
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(null); // Tạo mới cart

        // Tạo một Cart chứa một CartVariant với số lượng âm
        Cart cartEntity = new Cart();
        CartVariant cartVariant = new CartVariant();
        cartVariant.setQuantity(-5); // Số lượng âm (không hợp lệ)
        CartVariantKey cartVariantKey = new CartVariantKey();
        cartVariantKey.setVariantId(1L); // Biến thể sản phẩm
        cartVariant.setCartVariantKey(cartVariantKey);
        cartEntity.setCartVariants(Set.of(cartVariant)); // Thêm vào giỏ hàng

        // Không có giao dịch tồn kho
        List<DocketVariant> mockTransactions = new ArrayList<>();

        // Giả lập hành vi mapper và repository
        when(clientCartMapper.requestToEntity(request)).thenReturn(cartEntity);
        when(docketVariantRepository.findByVariantId(1L)).thenReturn(mockTransactions);

        // Mock phương thức tĩnh tính tồn kho trả về tồn kho âm
        try (MockedStatic<InventoryUtils> mockedStatic = mockStatic(InventoryUtils.class)) {
            mockedStatic.when(() -> InventoryUtils.calculateInventoryIndices(mockTransactions))
                    .thenReturn(Map.of("canBeSold", -1)); // Tồn kho âm (không hợp lệ)

            // Act & Assert: Gọi controller và mong đợi RuntimeException xảy ra
            RuntimeException exception = assertThrows(RuntimeException.class, () -> clientCartController.saveCart(request));

            // Đảm bảo message lỗi đúng như mong đợi
            assertEquals("Invalid inventory or quantity value", exception.getMessage());

            // Kiểm tra đã gọi đúng repository liên quan đến variant
            verify(docketVariantRepository).findByVariantId(1L);
        }
    }

    // Test case 10: Kiểm tra phương thức deleteCartItems xoá các cart variant đúng và trả về mã trạng thái 200 cùng thông báo thành công
    @Test
    void testDeleteCartItems_ShouldDeleteVariantsAndReturnContent() {
        // Arrange: Tạo một request giả lập với cartId và variantId
        ClientCartVariantKeyRequest keyRequest = new ClientCartVariantKeyRequest();
        keyRequest.setCartId(1L);
        keyRequest.setVariantId(2L);

        // Danh sách request chứa key cần xoá
        List<ClientCartVariantKeyRequest> requestList = List.of(keyRequest);

        // Act: Gọi hàm xoá cart variant từ controller
        ResponseEntity<Void> response = clientCartController.deleteCartItems(requestList);

        // Assert: Kiểm tra repository deleteAllById đã được gọi với danh sách bất kỳ
        verify(cartVariantRepository).deleteAllById(anyList());

        // Kiểm tra response trả về mã 200
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Delete cart items successfully", response.getBody());
    }

    // Test case 11: Kiểm tra phương thức deleteCartItems khi nhận dữ liệu không hợp lệ (cartId và variantId null) sẽ ném RuntimeException và không gọi repository deleteAllById
    @Test
    void testDeleteCartItems_WithInvalidCartItems_ShouldThrowRuntimeException() {
        // Arrange: Tạo request không hợp lệ với cartId và variantId đều null
        ClientCartVariantKeyRequest invalidKeyRequest = new ClientCartVariantKeyRequest();
        invalidKeyRequest.setCartId(null); // cartId không hợp lệ
        invalidKeyRequest.setVariantId(null); // variantId không hợp lệ

        List<ClientCartVariantKeyRequest> invalidRequestList = List.of(invalidKeyRequest);

        // Act & Assert: Kiểm tra rằng khi gọi controller với request không hợp lệ thì ném RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> clientCartController.deleteCartItems(invalidRequestList));
        assertEquals("Invalid cart item keys provided", exception.getMessage()); // So sánh thông báo lỗi

        // Verify: Đảm bảo rằng repository không được gọi khi dữ liệu không hợp lệ
        verify(cartVariantRepository, never()).deleteAllById(anyList());
    }

}
