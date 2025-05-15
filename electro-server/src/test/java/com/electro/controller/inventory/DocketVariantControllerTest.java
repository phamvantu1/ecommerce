package com.electro.controller.inventory;

import com.electro.dto.inventory.DocketVariantKeyRequest;
import com.electro.entity.inventory.DocketVariantKey;
import com.electro.service.inventory.DocketVariantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class DocketVariantControllerTest {

    @Mock
    private DocketVariantService docketVariantService;

    @InjectMocks
    private DocketVariantController docketVariantController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Test ID: DV-CT-001
     * Test case: Xóa một docket variant với ID hợp lệ
     * Mục tiêu: Kiểm tra controller trả về status NO_CONTENT và gọi service.delete() với đúng tham số
     */
    @Test
    void deleteDocketVariant_WithValidId_ShouldDeleteSuccessfully() {
        // Arrange
        Long docketId = 1L;
        Long variantId = 1L;
        DocketVariantKey key = new DocketVariantKey(docketId, variantId);

        // Act
        ResponseEntity<Void> response = docketVariantController.deleteDocketVariant(docketId, variantId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(docketVariantService, times(1)).delete(key);
    }

    /**
     * Test ID: DV-CT-002
     * Test case: Xóa một docket variant với ID không tồn tại
     * Mục tiêu: Kiểm tra controller vẫn trả về status NO_CONTENT và gọi service.delete() với ID không tồn tại
     */
    @Test
    void deleteDocketVariant_WithNonExistentId_ShouldReturnNoContent() {
        // Arrange
        Long docketId = 999L;
        Long variantId = 999L;
        DocketVariantKey key = new DocketVariantKey(docketId, variantId);

        // Act
        ResponseEntity<Void> response = docketVariantController.deleteDocketVariant(docketId, variantId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(docketVariantService, times(1)).delete(key);
    }

    /**
     * Test ID: DV-CT-003
     * Test case: Xóa nhiều docket variant với danh sách ID hợp lệ
     * Mục tiêu: Kiểm tra controller trả về status NO_CONTENT và gọi service.delete() với đúng danh sách tham số
     */
    @Test
    void deleteDocketVariants_WithValidIds_ShouldDeleteAllSuccessfully() {
        // Arrange
        DocketVariantKeyRequest request1 = new DocketVariantKeyRequest();
        request1.setDocketId(1L);
        request1.setVariantId(1L);

        DocketVariantKeyRequest request2 = new DocketVariantKeyRequest();
        request2.setDocketId(2L);
        request2.setVariantId(2L);

        List<DocketVariantKeyRequest> requests = Arrays.asList(request1, request2);

        // Act
        ResponseEntity<Void> response = docketVariantController.deleteDocketVariants(requests);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(docketVariantService, times(1)).delete(anyList());
    }

    /**
     * Test ID: DV-CT-004
     * Test case: Xóa nhiều docket variant với danh sách rỗng
     * Mục tiêu: Kiểm tra controller trả về status NO_CONTENT và gọi service.delete() với danh sách rỗng
     */
    @Test
    void deleteDocketVariants_WithEmptyList_ShouldReturnNoContent() {
        // Arrange
        List<DocketVariantKeyRequest> requests = Collections.emptyList();

        // Act
        ResponseEntity<Void> response = docketVariantController.deleteDocketVariants(requests);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(docketVariantService, times(1)).delete(Collections.emptyList());
    }

    /**
     * Test ID: DV-CT-005
     * Test case: Xóa nhiều docket variant với danh sách ID không tồn tại
     * Mục tiêu: Kiểm tra controller trả về status NO_CONTENT và gọi service.delete() với danh sách ID không tồn tại
     */
    @Test
    void deleteDocketVariants_WithNonExistentIds_ShouldReturnNoContent() {
        // Arrange
        DocketVariantKeyRequest request1 = new DocketVariantKeyRequest();
        request1.setDocketId(999L);
        request1.setVariantId(999L);

        DocketVariantKeyRequest request2 = new DocketVariantKeyRequest();
        request2.setDocketId(888L);
        request2.setVariantId(888L);

        List<DocketVariantKeyRequest> requests = Arrays.asList(request1, request2);

        // Act
        ResponseEntity<Void> response = docketVariantController.deleteDocketVariants(requests);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(docketVariantService, times(1)).delete(anyList());
    }

    /**
     * Test ID: DV-CT-006
     * Test case: Xóa nhiều docket variant với danh sách null
     * Mục tiêu: Kiểm tra controller xử lý đúng khi nhận danh sách null
     */
    @Test
    void deleteDocketVariants_WithNullList_ShouldReturnNoContent() {
        // Act
        ResponseEntity<Void> response = docketVariantController.deleteDocketVariants(null);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(docketVariantService, times(1)).delete(Collections.emptyList());
    }

    /**
     * Test ID: DV-CT-007
     * Test case: Xóa nhiều docket variant với danh sách chứa null
     * Mục tiêu: Kiểm tra controller xử lý đúng khi nhận danh sách chứa phần tử null
     */
    @Test
    void deleteDocketVariants_WithListContainingNull_ShouldReturnNoContent() {
        // Arrange
        List<DocketVariantKeyRequest> requests = Arrays.asList(
            new DocketVariantKeyRequest(),
            null,
            new DocketVariantKeyRequest()
        );

        // Act
        ResponseEntity<Void> response = docketVariantController.deleteDocketVariants(requests);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(docketVariantService, times(1)).delete(anyList());
    }
} 