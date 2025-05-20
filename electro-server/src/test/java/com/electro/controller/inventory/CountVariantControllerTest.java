package com.electro.controller.inventory;

import com.electro.dto.inventory.CountVariantKeyRequest;
import com.electro.entity.inventory.CountVariantKey;
import com.electro.service.inventory.CountVariantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CountVariantControllerTest {

    @Mock
    private CountVariantService countVariantService;

    @InjectMocks
    private CountVariantController countVariantController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Test ID: 1
     * Test case: Xóa một count variant với ID hợp lệ
     * 
     * Mục tiêu:
     * - Kiểm tra controller trả về status NO_CONTENT khi xóa thành công
     * - Kiểm tra service.delete() được gọi với đúng tham số
     * 
     * Dữ liệu test:
     * - countId = 1L
     * - variantId = 1L
     * 
     * Kết quả mong muốn:
     * - Status code: 204 NO_CONTENT
     * - Service.delete() được gọi 1 lần với key = CountVariantKey(1, 1)
     */
    @Test
    void deleteCountVariant_WithValidId_ShouldDeleteSuccessfully() {
        // Arrange
        Long countId = 1L;
        Long variantId = 1L;
        CountVariantKey key = new CountVariantKey(countId, variantId);

        // Act
        ResponseEntity<Void> response = countVariantController.deleteCountVariant(countId, variantId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(countVariantService, times(1)).delete(key);
    }

    /**
     * Test ID: 2
     * Test case: Xóa một count variant với ID không tồn tại
     * 
     * Mục tiêu:
     * - Kiểm tra controller trả về status NOT_FOUND khi ID không tồn tại
     * - Kiểm tra service.delete() được gọi với ID không tồn tại
     * 
     * Dữ liệu test:
     * - countId = 999L (ID không tồn tại)
     * - variantId = 999L (ID không tồn tại)
     * 
     * Kết quả mong muốn:
     * - Status code: 404 NOT_FOUND
     * - Service.delete() được gọi 1 lần với key = CountVariantKey(999, 999)
     */
    @Test
    void deleteCountVariant_WithNonExistentId_ShouldReturnNotFound() {
        // Arrange
        Long countId = 999L;
        Long variantId = 999L;
        CountVariantKey key = new CountVariantKey(countId, variantId);

        // Act
        ResponseEntity<Void> response = countVariantController.deleteCountVariant(countId, variantId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(countVariantService, times(1)).delete(key);
    }

    /**
     * Test ID: 3
     * Test case: Xóa nhiều count variant với danh sách ID hợp lệ
     * 
     * Mục tiêu:
     * - Kiểm tra controller trả về status NO_CONTENT khi xóa nhiều bản ghi thành công
     * - Kiểm tra service.delete() được gọi với đúng danh sách tham số
     * 
     * Dữ liệu test:
     * - Danh sách 2 cặp ID hợp lệ:
     *   + countId = 1L, variantId = 1L
     *   + countId = 2L, variantId = 2L
     * 
     * Kết quả mong muốn:
     * - Status code: 204 NO_CONTENT
     * - Service.delete() được gọi 1 lần với danh sách 2 key:
     *   + CountVariantKey(1, 1)
     *   + CountVariantKey(2, 2)
     */
    @Test
    void deleteCountVariants_WithValidIds_ShouldDeleteAllSuccessfully() {
        // Arrange
        CountVariantKeyRequest request1 = new CountVariantKeyRequest();
        request1.setCountId(1L);
        request1.setVariantId(1L);

        CountVariantKeyRequest request2 = new CountVariantKeyRequest();
        request2.setCountId(2L);
        request2.setVariantId(2L);

        List<CountVariantKeyRequest> requests = Arrays.asList(request1, request2);

        List<CountVariantKey> expectedKeys = Arrays.asList(
            new CountVariantKey(1L, 1L),
            new CountVariantKey(2L, 2L)
        );

        // Act
        ResponseEntity<Void> response = countVariantController.deleteCountVariants(requests);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(countVariantService, times(1)).delete(expectedKeys);
    }

    /**
     * Test ID: 4
     * Test case: Xóa nhiều count variant với danh sách rỗng
     * 
     * Mục tiêu:
     * - Kiểm tra controller trả về status NO_CONTENT khi gửi danh sách rỗng
     * - Kiểm tra service.delete() được gọi với danh sách rỗng
     * 
     * Dữ liệu test:
     * - Danh sách rỗng
     * 
     * Kết quả mong muốn:
     * - Status code: 204 NO_CONTENT
     * - Service.delete() được gọi 1 lần với danh sách rỗng
     */
    @Test
    void deleteCountVariants_WithEmptyList_ShouldReturnNoContent() {
        // Arrange
        List<CountVariantKeyRequest> requests = List.of();

        // Act
        ResponseEntity<Void> response = countVariantController.deleteCountVariants(requests);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(countVariantService, times(1)).delete(List.of());
    }

    /**
     * Test ID: 5
     * Test case: Xóa nhiều count variant với danh sách ID không tồn tại
     * 
     * Mục tiêu:
     * - Kiểm tra controller trả về status NOT_FOUND khi xóa nhiều ID không tồn tại
     * - Kiểm tra service.delete() được gọi với danh sách ID không tồn tại
     * 
     * Dữ liệu test:
     * - Danh sách 2 cặp ID không tồn tại:
     *   + countId = 999L, variantId = 999L
     *   + countId = 888L, variantId = 888L
     * 
     * Kết quả mong muốn:
     * - Status code: 404 NOT_FOUND
     * - Service.delete() được gọi 1 lần với danh sách 2 key:
     *   + CountVariantKey(999, 999)
     *   + CountVariantKey(888, 888)
     */
    @Test
    void deleteCountVariants_WithNonExistentIds_ShouldReturnNotFound() {
        // Arrange
        CountVariantKeyRequest request1 = new CountVariantKeyRequest();
        request1.setCountId(999L);
        request1.setVariantId(999L);

        CountVariantKeyRequest request2 = new CountVariantKeyRequest();
        request2.setCountId(888L);
        request2.setVariantId(888L);

        List<CountVariantKeyRequest> requests = Arrays.asList(request1, request2);

        List<CountVariantKey> expectedKeys = Arrays.asList(
            new CountVariantKey(999L, 999L),
            new CountVariantKey(888L, 888L)
        );

        // Act
        ResponseEntity<Void> response = countVariantController.deleteCountVariants(requests);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(countVariantService, times(1)).delete(expectedKeys);
    }
} 