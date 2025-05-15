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
     * Test ID: CV-CT-001
     * Test case: Xóa một count variant với ID hợp lệ
     * Mục tiêu: Kiểm tra controller trả về status NO_CONTENT và gọi service.delete() với đúng tham số
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
     * Test ID: CV-CT-002
     * Test case: Xóa một count variant với ID không tồn tại
     * Mục tiêu: Kiểm tra controller vẫn trả về status NO_CONTENT và gọi service.delete() với ID không tồn tại
     */
    @Test
    void deleteCountVariant_WithNonExistentId_ShouldReturnNoContent() {
        // Arrange
        Long countId = 999L;
        Long variantId = 999L;
        CountVariantKey key = new CountVariantKey(countId, variantId);

        // Act
        ResponseEntity<Void> response = countVariantController.deleteCountVariant(countId, variantId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(countVariantService, times(1)).delete(key);
    }

    /**
     * Test ID: CV-CT-003
     * Test case: Xóa nhiều count variant với danh sách ID hợp lệ
     * Mục tiêu: Kiểm tra controller trả về status NO_CONTENT và gọi service.delete() với đúng danh sách tham số
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
     * Test ID: CV-CT-004
     * Test case: Xóa nhiều count variant với danh sách rỗng
     * Mục tiêu: Kiểm tra controller trả về status NO_CONTENT và gọi service.delete() với danh sách rỗng
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
     * Test ID: CV-CT-005
     * Test case: Xóa nhiều count variant với danh sách ID không tồn tại
     * Mục tiêu: Kiểm tra controller trả về status NO_CONTENT và gọi service.delete() với danh sách ID không tồn tại
     */
    @Test
    void deleteCountVariants_WithNonExistentIds_ShouldReturnNoContent() {
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
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(countVariantService, times(1)).delete(expectedKeys);
    }
} 