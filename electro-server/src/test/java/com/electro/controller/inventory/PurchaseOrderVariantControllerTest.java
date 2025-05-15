package com.electro.controller.inventory;

import com.electro.dto.inventory.PurchaseOrderVariantKeyRequest;
import com.electro.entity.inventory.PurchaseOrderVariantKey;
import com.electro.service.inventory.PurchaseOrderVariantService;
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

class PurchaseOrderVariantControllerTest {

    @Mock
    private PurchaseOrderVariantService purchaseOrderVariantService;

    @InjectMocks
    private PurchaseOrderVariantController purchaseOrderVariantController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Test ID: POV-CT-001
     * Test case: Xóa một purchase order variant với ID hợp lệ
     * Mục tiêu: Kiểm tra controller trả về status NO_CONTENT và gọi service.delete() với đúng tham số
     */
    @Test
    void deletePurchaseOrderVariant_WithValidId_ShouldDeleteSuccessfully() {
        // Arrange
        Long purchaseOrderId = 1L;
        Long variantId = 1L;
        PurchaseOrderVariantKey key = new PurchaseOrderVariantKey(purchaseOrderId, variantId);

        // Act
        ResponseEntity<Void> response = purchaseOrderVariantController.deletePurchaseOrderVariant(purchaseOrderId, variantId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(purchaseOrderVariantService, times(1)).delete(key);
    }

    /**
     * Test ID: POV-CT-002
     * Test case: Xóa một purchase order variant với ID không tồn tại
     * Mục tiêu: Kiểm tra controller vẫn trả về status NO_CONTENT và gọi service.delete() với ID không tồn tại
     */
    @Test
    void deletePurchaseOrderVariant_WithNonExistentId_ShouldReturnNoContent() {
        // Arrange
        Long purchaseOrderId = 999L;
        Long variantId = 999L;
        PurchaseOrderVariantKey key = new PurchaseOrderVariantKey(purchaseOrderId, variantId);

        // Act
        ResponseEntity<Void> response = purchaseOrderVariantController.deletePurchaseOrderVariant(purchaseOrderId, variantId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(purchaseOrderVariantService, times(1)).delete(key);
    }

    /**
     * Test ID: POV-CT-003
     * Test case: Xóa nhiều purchase order variant với danh sách ID hợp lệ
     * Mục tiêu: Kiểm tra controller trả về status NO_CONTENT và gọi service.delete() với đúng danh sách tham số
     */
    @Test
    void deletePurchaseOrderVariants_WithValidIds_ShouldDeleteAllSuccessfully() {
        // Arrange
        PurchaseOrderVariantKeyRequest request1 = new PurchaseOrderVariantKeyRequest();
        request1.setPurchaseOrderId(1L);
        request1.setVariantId(1L);

        PurchaseOrderVariantKeyRequest request2 = new PurchaseOrderVariantKeyRequest();
        request2.setPurchaseOrderId(2L);
        request2.setVariantId(2L);

        List<PurchaseOrderVariantKeyRequest> requests = Arrays.asList(request1, request2);

        List<PurchaseOrderVariantKey> expectedKeys = Arrays.asList(
            new PurchaseOrderVariantKey(1L, 1L),
            new PurchaseOrderVariantKey(2L, 2L)
        );

        // Act
        ResponseEntity<Void> response = purchaseOrderVariantController.deletePurchaseOrderVariants(requests);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(purchaseOrderVariantService, times(1)).delete(expectedKeys);
    }

    /**
     * Test ID: POV-CT-004
     * Test case: Xóa nhiều purchase order variant với danh sách rỗng
     * Mục tiêu: Kiểm tra controller trả về status NO_CONTENT và gọi service.delete() với danh sách rỗng
     */
    @Test
    void deletePurchaseOrderVariants_WithEmptyList_ShouldReturnNoContent() {
        // Arrange
        List<PurchaseOrderVariantKeyRequest> requests = List.of();

        // Act
        ResponseEntity<Void> response = purchaseOrderVariantController.deletePurchaseOrderVariants(requests);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(purchaseOrderVariantService, times(1)).delete(List.of());
    }

    /**
     * Test ID: POV-CT-005
     * Test case: Xóa nhiều purchase order variant với danh sách ID không tồn tại
     * Mục tiêu: Kiểm tra controller trả về status NO_CONTENT và gọi service.delete() với danh sách ID không tồn tại
     */
    @Test
    void deletePurchaseOrderVariants_WithNonExistentIds_ShouldReturnNoContent() {
        // Arrange
        PurchaseOrderVariantKeyRequest request1 = new PurchaseOrderVariantKeyRequest();
        request1.setPurchaseOrderId(999L);
        request1.setVariantId(999L);

        PurchaseOrderVariantKeyRequest request2 = new PurchaseOrderVariantKeyRequest();
        request2.setPurchaseOrderId(888L);
        request2.setVariantId(888L);

        List<PurchaseOrderVariantKeyRequest> requests = Arrays.asList(request1, request2);

        List<PurchaseOrderVariantKey> expectedKeys = Arrays.asList(
            new PurchaseOrderVariantKey(999L, 999L),
            new PurchaseOrderVariantKey(888L, 888L)
        );

        // Act
        ResponseEntity<Void> response = purchaseOrderVariantController.deletePurchaseOrderVariants(requests);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(purchaseOrderVariantService, times(1)).delete(expectedKeys);
    }
} 