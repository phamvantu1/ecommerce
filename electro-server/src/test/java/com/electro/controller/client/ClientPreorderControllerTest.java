package com.electro.controller.client;

import com.electro.constant.AppConstants;
import com.electro.controller.client.ClientPreorderController;
import com.electro.dto.ListResponse;
import com.electro.dto.client.ClientPreorderRequest;
import com.electro.dto.client.ClientPreorderResponse;
import com.electro.dto.client.ClientListedProductResponse;
import com.electro.entity.client.Preorder;
import com.electro.entity.authentication.User;
import com.electro.entity.address.Address;
import com.electro.entity.address.Ward;
import com.electro.entity.address.District;
import com.electro.entity.address.Province;
import com.electro.entity.product.Product;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.client.ClientPreorderMapper;
import com.electro.repository.client.PreorderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientPreorderControllerTest {

    @Mock
    private PreorderRepository preorderRepository; // Mock repository để quản lý Preorder

    @Mock
    private ClientPreorderMapper clientPreorderMapper; // Mock mapper để ánh xạ giữa entity và DTO

    @Mock
    private Authentication authentication; // Mock Authentication để mô phỏng người dùng đăng nhập

    @InjectMocks
    private ClientPreorderController clientPreorderController; // Inject các mock vào ClientPreorderController

    private ClientPreorderRequest request; // Đối tượng ClientPreorderRequest dùng cho các test case
    private Preorder preorder; // Đối tượng Preorder dùng cho các test case
    private ClientPreorderResponse response; // Đối tượng ClientPreorderResponse dùng cho các test case
    private User user; // Đối tượng User dùng cho các test case
    private Product product; // Đối tượng Product dùng cho các test case

    @BeforeEach
    void setUp() {
        // Khởi tạo đối tượng User để sử dụng trong các test case
        user = new User(); // Tạo mới đối tượng User
        user.setId(1L); // Gán ID
        user.setUsername("testuser"); // Gán username
        user.setFullname("Test User"); // Gán fullname
        user.setPhone("1234567890"); // Gán số điện thoại

        // Thiết lập địa chỉ cho User
        Address address = new Address(); // Tạo mới đối tượng Address
        address.setLine("123 Street"); // Gán địa chỉ cụ thể
        Ward ward = new Ward(); // Tạo mới đối tượng Ward
        ward.setName("Ward A"); // Gán tên ward
        District district = new District(); // Tạo mới đối tượng District
        district.setName("District B"); // Gán tên district
        Province province = new Province(); // Tạo mới đối tượng Province
        province.setName("Province C"); // Gán tên province
        address.setWard(ward); // Gán ward vào address
        address.setDistrict(district); // Gán district vào address
        address.setProvince(province); // Gán province vào address
        user.setAddress(address); // Gán address vào user

        // Khởi tạo đối tượng Product
        product = new Product(); // Tạo mới đối tượng Product
        product.setId(2L); // Gán ID để khớp với request
        product.setCode("PROD123"); // Gán mã sản phẩm

        // Khởi tạo dữ liệu mẫu cho ClientPreorderRequest
        request = new ClientPreorderRequest(); // Tạo mới request
        request.setUserId(1L); // Gán userId
        request.setProductId(2L); // Gán productId

        // Khởi tạo dữ liệu mẫu cho Preorder
        preorder = new Preorder(); // Tạo mới Preorder
        preorder.setId(1L); // Gán ID
        preorder.setUser(user); // Gán User
        preorder.setProduct(product); // Gán Product
        preorder.setStatus(1); // Gán status (đang hoạt động)
        preorder.setCreatedAt(Instant.now()); // Gán thời gian tạo
        preorder.setUpdatedAt(Instant.now()); // Gán thời gian cập nhật

        // Khởi tạo dữ liệu mẫu cho ClientPreorderResponse
        response = new ClientPreorderResponse(); // Tạo mới response
        response.setPreorderId(1L); // Gán ID cho pre-order
        response.setPreorderCreatedAt(preorder.getCreatedAt()); // Gán thời gian tạo
        response.setPreorderUpdatedAt(preorder.getUpdatedAt()); // Gán thời gian cập nhật
        response.setPreorderStatus(1); // Gán status
        ClientListedProductResponse productResponse = new ClientListedProductResponse(); // Tạo mới product response
        productResponse.setProductId(2L); // Gán productId
        response.setPreorderProduct(productResponse); // Gán product vào response
    }

    // Test case cho getAllPreorders - Danh sách rỗng
    @Test
    void getAllPreorders_Testcase1() {
        // Mục đích: Kiểm tra lấy danh sách Preorder khi không có dữ liệu
        // Arrange
        when(authentication.getName()).thenReturn("testuser"); // Mock username từ Authentication
        Page<Preorder> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0); // Tạo Page rỗng
        when(preorderRepository.findAllByUsername(eq("testuser"), eq("updatedAt,desc"), isNull(), any(PageRequest.class)))
                .thenReturn(emptyPage); // Mock repository trả về Page rỗng

        // Act
        ResponseEntity<ListResponse<ClientPreorderResponse>> result = clientPreorderController.getAllPreorders(
                authentication, 1, 10, "updatedAt,desc", null); // Gọi phương thức getAllPreorders

        // Assert
        verify(authentication).getName(); // Kiểm tra gọi lấy username
        verify(preorderRepository).findAllByUsername(eq("testuser"), eq("updatedAt,desc"), isNull(), any(PageRequest.class)); // Kiểm tra gọi repository
        verify(clientPreorderMapper, never()).entityToResponse(any()); // Kiểm tra không gọi ánh xạ
        assertEquals(HttpStatus.OK, result.getStatusCode()); // Kiểm tra mã trạng thái HTTP là OK
        ListResponse<ClientPreorderResponse> body = result.getBody(); // Lấy body của response
        assertNotNull(body); // Kiểm tra body không null
        assertTrue(body.getContent().isEmpty()); // Kiểm tra danh sách rỗng
    }

    // Test case cho getAllPreorders - Danh sách có dữ liệu với filter
    @Test
    void getAllPreorders_Testcase2() {
        // Mục đích: Kiểm tra lấy danh sách Preorder với dữ liệu và filter
        // Arrange
        when(authentication.getName()).thenReturn("testuser"); // Mock username từ Authentication
        List<Preorder> preorders = List.of(preorder); // Tạo danh sách Preorder
        Page<Preorder> page = new PageImpl<>(preorders, PageRequest.of(0, 10), 1); // Tạo Page chứa 1 Preorder
        when(preorderRepository.findAllByUsername(eq("testuser"), eq("updatedAt,desc"), eq("status:1"), any(PageRequest.class)))
                .thenReturn(page); // Mock repository trả về Page
        when(clientPreorderMapper.entityToResponse(preorder)).thenReturn(response); // Mock ánh xạ Preorder sang response

        // Act
        ResponseEntity<ListResponse<ClientPreorderResponse>> result = clientPreorderController.getAllPreorders(
                authentication, 1, 10, "updatedAt,desc", "status:1"); // Gọi phương thức getAllPreorders với filter

        // Assert
        verify(authentication).getName(); // Kiểm tra gọi lấy username
        verify(preorderRepository).findAllByUsername(eq("testuser"), eq("updatedAt,desc"), eq("status:1"), any(PageRequest.class)); // Kiểm tra gọi repository
        verify(clientPreorderMapper).entityToResponse(preorder); // Kiểm tra gọi ánh xạ
        assertEquals(HttpStatus.OK, result.getStatusCode()); // Kiểm tra mã trạng thái HTTP là OK
        ListResponse<ClientPreorderResponse> body = result.getBody(); // Lấy body của response
        assertNotNull(body); // Kiểm tra body không null
        assertEquals(1, body.getContent().size()); // Kiểm tra danh sách có 1 phần tử
        assertEquals(response, body.getContent().get(0)); // Kiểm tra phần tử đầu tiên
        assertEquals(1, body.getPage()); // Kiểm tra tổng số phần tử
    }

    // Test case cho getAllPreorders - Authentication không hợp lệ
    @Test
    void getAllPreorders_Testcase3() {
        // Mục đích: Kiểm tra trường hợp Authentication là null
        // Arrange
        // Không cần stub vì mã dừng ngay khi authentication là null

        // Act & Assert
        AuthenticationCredentialsNotFoundException exception = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> clientPreorderController.getAllPreorders(null, 1, 10, "updatedAt,desc", null)); // Gọi getAllPreorders với authentication null
        assertEquals("Authentication is required", exception.getMessage()); // Kiểm tra thông điệp ngoại lệ
        verifyNoInteractions(authentication, preorderRepository, clientPreorderMapper); // Kiểm tra không tương tác với bất kỳ mock nào
    }

    // Test case cho getAllPreorders - Lỗi database
    @Test
    void getAllPreorders_Testcase4() {
        // Mục đích: Kiểm tra trường hợp lỗi database khi lấy danh sách Preorder
        // Arrange
        when(authentication.getName()).thenReturn("testuser"); // Mock username từ Authentication
        when(preorderRepository.findAllByUsername(eq("testuser"), eq("updatedAt,desc"), isNull(), any(PageRequest.class)))
                .thenThrow(new RuntimeException("DB error")); // Mock repository ném ngoại lệ

        // Act & Assert
        assertThrows(RuntimeException.class, () -> clientPreorderController.getAllPreorders(
                authentication, 1, 10, "updatedAt,desc", null)); // Kiểm tra ném RuntimeException
        verify(authentication).getName(); // Kiểm tra gọi lấy username
        verify(preorderRepository).findAllByUsername(eq("testuser"), eq("updatedAt,desc"), isNull(), any(PageRequest.class)); // Kiểm tra gọi repository
        verify(clientPreorderMapper, never()).entityToResponse(any()); // Kiểm tra không gọi ánh xạ
    }

    // Test case cho createPreorder - Tạo mới Preorder
    @Test
    void createPreorder_Testcase1() throws Exception {
        // Mục đích: Kiểm tra tạo mới Preorder khi không tồn tại trước đó
        // Arrange
        when(preorderRepository.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.empty()); // Mock không tìm thấy Preorder
        when(clientPreorderMapper.requestToEntity(request)).thenReturn(preorder); // Mock ánh xạ request sang entity
        when(preorderRepository.save(preorder)).thenReturn(preorder); // Mock lưu Preorder
        when(clientPreorderMapper.entityToResponse(preorder)).thenReturn(response); // Mock ánh xạ entity sang response

        // Act
        ResponseEntity<ClientPreorderResponse> result = clientPreorderController.createPreorder(request); // Gọi phương thức createPreorder

        // Assert
        verify(preorderRepository).findByUser_IdAndProduct_Id(1L, 2L); // Kiểm tra gọi tìm Preorder
        verify(clientPreorderMapper).requestToEntity(request); // Kiểm tra gọi ánh xạ request sang entity
        verify(preorderRepository).save(preorder); // Kiểm tra gọi lưu Preorder
        verify(clientPreorderMapper).entityToResponse(preorder); // Kiểm tra gọi ánh xạ sang response
        assertEquals(HttpStatus.CREATED, result.getStatusCode()); // Kiểm tra mã trạng thái HTTP là CREATED
        assertEquals(response, result.getBody()); // Kiểm tra body chứa response đúng
    }

    // Test case cho createPreorder - Preorder tồn tại với status = 1 (ném ngoại lệ)
    @Test
    void createPreorder_Testcase2() {
        // Mục đích: Kiểm tra trường hợp Preorder đã tồn tại và đang hoạt động
        // Arrange
        preorder.setStatus(1); // Đặt status là 1 (đang hoạt động)
        when(preorderRepository.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.of(preorder)); // Mock tìm thấy Preorder

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> clientPreorderController.createPreorder(request)); // Kiểm tra ném Exception
        assertEquals("Duplicated preorder", exception.getMessage()); // Kiểm tra thông điệp ngoại lệ
        verify(preorderRepository).findByUser_IdAndProduct_Id(1L, 2L); // Kiểm tra gọi tìm Preorder
        verify(preorderRepository, never()).save(any()); // Kiểm tra không gọi lưu
        verify(clientPreorderMapper, never()).requestToEntity(any()); // Kiểm tra không gọi ánh xạ request sang entity
        verify(clientPreorderMapper, never()).entityToResponse(any()); // Kiểm tra không gọi ánh xạ sang response
    }

    // Test case cho createPreorder - Preorder tồn tại với status != 1 (cập nhật Preorder)
    @Test
    void createPreorder_Testcase3() throws Exception {
        // Mục đích: Kiểm tra kích hoạt lại Preorder bị hủy (status != 1)
        // Arrange
        preorder.setStatus(0); // Đặt status là 0 (bị hủy)
        when(preorderRepository.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.of(preorder)); // Mock tìm thấy Preorder
        when(preorderRepository.save(preorder)).thenReturn(preorder); // Mock lưu Preorder
        when(clientPreorderMapper.entityToResponse(preorder)).thenReturn(response); // Mock ánh xạ entity sang response

        // Act
        ResponseEntity<ClientPreorderResponse> result = clientPreorderController.createPreorder(request); // Gọi phương thức createPreorder

        // Assert
        verify(preorderRepository).findByUser_IdAndProduct_Id(1L, 2L); // Kiểm tra gọi tìm Preorder
        verify(preorderRepository).save(preorder); // Kiểm tra gọi lưu Preorder
        verify(clientPreorderMapper).entityToResponse(preorder); // Kiểm tra gọi ánh xạ sang response
        verify(clientPreorderMapper, never()).requestToEntity(any()); // Kiểm tra không gọi ánh xạ request sang entity
        assertEquals(HttpStatus.OK, result.getStatusCode()); // Kiểm tra mã trạng thái HTTP là OK
        assertEquals(response, result.getBody()); // Kiểm tra body chứa response đúng
        assertEquals(1, preorder.getStatus()); // Kiểm tra status được cập nhật thành 1
    }

    // Test case cho createPreorder - Lỗi database khi lưu
    @Test
    void createPreorder_Testcase4() {
        // Mục đích: Kiểm tra trường hợp lỗi database khi lưu Preorder
        // Arrange
        when(preorderRepository.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.empty()); // Mock không tìm thấy Preorder
        when(clientPreorderMapper.requestToEntity(request)).thenReturn(preorder); // Mock ánh xạ request sang entity
        when(preorderRepository.save(preorder)).thenThrow(new RuntimeException("DB error")); // Mock lưu Preorder ném ngoại lệ

        // Act & Assert
        assertThrows(RuntimeException.class, () -> clientPreorderController.createPreorder(request)); // Kiểm tra ném RuntimeException
        verify(preorderRepository).findByUser_IdAndProduct_Id(1L, 2L); // Kiểm tra gọi tìm Preorder
        verify(clientPreorderMapper).requestToEntity(request); // Kiểm tra gọi ánh xạ request sang entity
        verify(preorderRepository).save(preorder); // Kiểm tra gọi lưu Preorder
        verify(clientPreorderMapper, never()).entityToResponse(any()); // Kiểm tra không gọi ánh xạ sang response
    }

    // Test case cho createPreorder - Lỗi ánh xạ request sang entity
    @Test
    void createPreorder_Testcase5() {
        // Mục đích: Kiểm tra trường hợp lỗi ánh xạ từ request sang entity
        // Arrange
        when(preorderRepository.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.empty()); // Mock không tìm thấy Preorder
        when(clientPreorderMapper.requestToEntity(request)).thenThrow(new RuntimeException("Mapping error")); // Mock ánh xạ ném ngoại lệ

        // Act & Assert
        assertThrows(RuntimeException.class, () -> clientPreorderController.createPreorder(request)); // Kiểm tra ném RuntimeException
        verify(preorderRepository).findByUser_IdAndProduct_Id(1L, 2L); // Kiểm tra gọi tìm Preorder
        verify(clientPreorderMapper).requestToEntity(request); // Kiểm tra gọi ánh xạ request sang entity
        verify(preorderRepository, never()).save(any()); // Kiểm tra không gọi lưu
        verify(clientPreorderMapper, never()).entityToResponse(any()); // Kiểm tra không gọi ánh xạ sang response
    }

    // Test case cho updatePreorder - Cập nhật Preorder thành công
    @Test
    void updatePreorder_Testcase1() {
        // Mục đích: Kiểm tra cập nhật Preorder thành công
        // Arrange
        when(preorderRepository.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.of(preorder)); // Mock tìm thấy Preorder
        when(clientPreorderMapper.partialUpdate(preorder, request)).thenReturn(preorder); // Mock cập nhật Preorder
        when(preorderRepository.save(preorder)).thenReturn(preorder); // Mock lưu Preorder
        when(clientPreorderMapper.entityToResponse(preorder)).thenReturn(response); // Mock ánh xạ entity sang response

        // Act
        ResponseEntity<ClientPreorderResponse> result = clientPreorderController.updatePreorder(request); // Gọi phương thức updatePreorder

        // Assert
        verify(preorderRepository).findByUser_IdAndProduct_Id(1L, 2L); // Kiểm tra gọi tìm Preorder
        verify(clientPreorderMapper).partialUpdate(preorder, request); // Kiểm tra gọi cập nhật Preorder
        verify(preorderRepository).save(preorder); // Kiểm tra gọi lưu Preorder
        verify(clientPreorderMapper).entityToResponse(preorder); // Kiểm tra gọi ánh xạ sang response
        assertEquals(HttpStatus.OK, result.getStatusCode()); // Kiểm tra mã trạng thái HTTP là OK
        assertEquals(response, result.getBody()); // Kiểm tra body chứa response đúng
    }

    // Test case cho updatePreorder - Preorder không tồn tại
    @Test
    void updatePreorder_Testcase2() {
        // Mục đích: Kiểm tra trường hợp không tìm thấy Preorder để cập nhật
        // Arrange
        when(preorderRepository.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.empty()); // Mock không tìm thấy Preorder

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> clientPreorderController.updatePreorder(request)); // Kiểm tra ném ResourceNotFoundException
        assertTrue(exception.getMessage().contains("PREORDER")); // Kiểm tra thông điệp ngoại lệ chứa resource name
        verify(preorderRepository).findByUser_IdAndProduct_Id(1L, 2L); // Kiểm tra gọi tìm Preorder
        verify(clientPreorderMapper, never()).partialUpdate(any(), any()); // Kiểm tra không gọi cập nhật
        verify(preorderRepository, never()).save(any()); // Kiểm tra không gọi lưu
        verify(clientPreorderMapper, never()).entityToResponse(any()); // Kiểm tra không gọi ánh xạ sang response
    }

    // Test case cho updatePreorder - Lỗi database khi lưu
    @Test
    void updatePreorder_Testcase3() {
        // Mục đích: Kiểm tra trường hợp lỗi database khi lưu Preorder
        // Arrange
        when(preorderRepository.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.of(preorder)); // Mock tìm thấy Preorder
        when(clientPreorderMapper.partialUpdate(preorder, request)).thenReturn(preorder); // Mock cập nhật Preorder
        when(preorderRepository.save(preorder)).thenThrow(new RuntimeException("DB error")); // Mock lưu Preorder ném ngoại lệ

        // Act & Assert
        assertThrows(RuntimeException.class, () -> clientPreorderController.updatePreorder(request)); // Kiểm tra ném RuntimeException
        verify(preorderRepository).findByUser_IdAndProduct_Id(1L, 2L); // Kiểm tra gọi tìm Preorder
        verify(clientPreorderMapper).partialUpdate(preorder, request); // Kiểm tra gọi cập nhật Preorder
        verify(preorderRepository).save(preorder); // Kiểm tra gọi lưu Preorder
        verify(clientPreorderMapper, never()).entityToResponse(any()); // Kiểm tra không gọi ánh xạ sang response
    }

    // Test case cho updatePreorder - Lỗi ánh xạ partialUpdate
    @Test
    void updatePreorder_Testcase4() {
        // Mục đích: Kiểm tra trường hợp lỗi ánh xạ khi cập nhật Preorder
        // Arrange
        when(preorderRepository.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.of(preorder)); // Mock tìm thấy Preorder
        when(clientPreorderMapper.partialUpdate(preorder, request)).thenThrow(new RuntimeException("Mapping error")); // Mock ánh xạ ném ngoại lệ

        // Act & Assert
        assertThrows(RuntimeException.class, () -> clientPreorderController.updatePreorder(request)); // Kiểm tra ném RuntimeException
        verify(preorderRepository).findByUser_IdAndProduct_Id(1L, 2L); // Kiểm tra gọi tìm Preorder
        verify(clientPreorderMapper).partialUpdate(preorder, request); // Kiểm tra gọi cập nhật Preorder
        verify(preorderRepository, never()).save(any()); // Kiểm tra không gọi lưu
        verify(clientPreorderMapper, never()).entityToResponse(any()); // Kiểm tra không gọi ánh xạ sang response
    }

    // Test case cho deletePreorders - Xóa danh sách ID hợp lệ
    @Test
    void deletePreorders_Testcase1() {
        // Mục đích: Kiểm tra xóa danh sách Preorder với ID hợp lệ
        // Arrange
        List<Long> ids = List.of(1L, 2L); // Tạo danh sách ID
        doNothing().when(preorderRepository).deleteAllById(ids); // Mock xóa Preorder

        // Act
        ResponseEntity<Void> result = clientPreorderController.deletePreorders(ids); // Gọi phương thức deletePreorders

        // Assert
        verify(preorderRepository).deleteAllById(ids); // Kiểm tra gọi xóa Preorder
        verifyNoInteractions(clientPreorderMapper); // Kiểm tra không gọi mapper
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode()); // Kiểm tra mã trạng thái HTTP là NO_CONTENT
        assertNull(result.getBody()); // Kiểm tra body là null
    }

    // Test case cho deletePreorders - Xóa danh sách ID rỗng
    @Test
    void deletePreorders_Testcase2() {
        // Mục đích: Kiểm tra xóa khi danh sách ID rỗng
        // Arrange
        List<Long> ids = Collections.emptyList(); // Tạo danh sách ID rỗng
        doNothing().when(preorderRepository).deleteAllById(ids); // Mock xóa Preorder

        // Act
        ResponseEntity<Void> result = clientPreorderController.deletePreorders(ids); // Gọi phương thức deletePreorders

        // Assert
        verify(preorderRepository).deleteAllById(ids); // Kiểm tra gọi xóa Preorder
        verifyNoInteractions(clientPreorderMapper); // Kiểm tra không gọi mapper
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode()); // Kiểm tra mã trạng thái HTTP là NO_CONTENT
        assertNull(result.getBody()); // Kiểm tra body là null
    }

    // Test case cho deletePreorders - Lỗi database khi xóa
    @Test
    void deletePreorders_Testcase3() {
        // Mục đích: Kiểm tra trường hợp lỗi database khi xóa Preorder
        // Arrange
        List<Long> ids = List.of(1L); // Tạo danh sách ID
        doThrow(new RuntimeException("DB error")).when(preorderRepository).deleteAllById(ids); // Mock xóa Preorder ném ngoại lệ

        // Act & Assert
        assertThrows(RuntimeException.class, () -> clientPreorderController.deletePreorders(ids)); // Kiểm tra ném RuntimeException
        verify(preorderRepository).deleteAllById(ids); // Kiểm tra gọi xóa Preorder
        verifyNoInteractions(clientPreorderMapper); // Kiểm tra không gọi mapper
    }
}