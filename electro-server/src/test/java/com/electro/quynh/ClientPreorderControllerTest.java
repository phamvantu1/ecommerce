package com.electro.quynh;

import com.electro.controller.client.ClientPreorderController;
import com.electro.dto.client.ClientPreorderRequest;
import com.electro.dto.client.ClientPreorderResponse;
import com.electro.entity.authentication.User;
import com.electro.entity.client.Preorder;
import com.electro.entity.product.Product;
import com.electro.mapper.client.ClientPreorderMapper;
import com.electro.repository.client.PreorderRepository;
import com.electro.exception.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientPreorderControllerTest {

    private PreorderRepository preorderRepository;
    private ClientPreorderMapper mapper;
    private ClientPreorderController controller;

    @BeforeEach
    void setUp() {
        // Khởi tạo mock và controller
        preorderRepository = mock(PreorderRepository.class);
        mapper = mock(ClientPreorderMapper.class);
        controller = new ClientPreorderController(preorderRepository, mapper);
    }

    // ✅ Test tạo preorder khi đã tồn tại và status = 1 (không cho tạo lại)
    @Test
    void testCreatePreorder_Testcase1() throws Exception{
        ClientPreorderRequest request = new ClientPreorderRequest();
        request.setUserId(1L);
        request.setProductId(2L);

        Preorder existing = new Preorder();
        existing.setStatus(1); // đã tồn tại và đã active

        when(preorderRepository.findByUser_IdAndProduct_Id(1L, 2L))
                .thenReturn(Optional.of(existing));

        assertThrows(Exception.class, () -> controller.createPreorder(request));
    }

    // ✅ Test tạo preorder khi đã tồn tại nhưng status != 1 (cập nhật lại)
    @Test
    void testCreatePreorder_Testcase2() throws Exception{
        ClientPreorderRequest request = new ClientPreorderRequest();
        request.setUserId(1L);
        request.setProductId(2L);

        Preorder existing = new Preorder();
        existing.setStatus(0); // status khác 1

        Preorder updated = new Preorder();
        updated.setStatus(1);

        when(preorderRepository.findByUser_IdAndProduct_Id(1L, 2L))
                .thenReturn(Optional.of(existing));
        when(preorderRepository.save(any(Preorder.class))).thenReturn(updated);
        when(mapper.entityToResponse(any(Preorder.class))).thenReturn(new ClientPreorderResponse());

        ResponseEntity<ClientPreorderResponse> response = controller.createPreorder(request);

        // Kiểm tra kết quả trả về OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ✅ Test tạo preorder mới khi chưa tồn tại
    @Test
    void testCreatePreorder_Testcase3() throws Exception{
        ClientPreorderRequest request = new ClientPreorderRequest();
        request.setUserId(1L);
        request.setProductId(2L);
        request.setStatus(1);

        // Tạo entity mock cho user và product
        User user = new User();
        user.setId(1L);
        Product product = new Product();
        product.setId(2L);

        Preorder newEntity = new Preorder();
        newEntity.setUser(user);
        newEntity.setProduct(product);
        newEntity.setStatus(1);

        Preorder savedEntity = new Preorder();
        savedEntity.setId(1L);
        savedEntity.setUser(user);
        savedEntity.setProduct(product);
        savedEntity.setStatus(1);
        savedEntity.setUpdatedAt(Instant.now());

        when(preorderRepository.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.empty());
        when(mapper.requestToEntity(request)).thenReturn(newEntity);
        when(preorderRepository.save(newEntity)).thenReturn(newEntity);
        when(mapper.entityToResponse(newEntity)).thenReturn(new ClientPreorderResponse());

        ResponseEntity<ClientPreorderResponse> response = controller.createPreorder(request);

        // Kiểm tra status CREATED
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // ✅ Test cập nhật preorder khi tồn tại
    @Test
    void testUpdatePreorder_Testcase1() {
        ClientPreorderRequest request = new ClientPreorderRequest();
        request.setUserId(1L);
        request.setProductId(2L);
        request.setStatus(0);

        Preorder existing = new Preorder();
        Preorder updated = new Preorder();
        ClientPreorderResponse response = new ClientPreorderResponse();

        when(preorderRepository.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.of(existing));
        when(mapper.partialUpdate(existing, request)).thenReturn(updated);
        when(preorderRepository.save(updated)).thenReturn(updated);
        when(mapper.entityToResponse(updated)).thenReturn(response);

        ResponseEntity<ClientPreorderResponse> result = controller.updatePreorder(request);

        // Kiểm tra trả về thành công và đúng response
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    // ✅ Test cập nhật preorder khi không tồn tại
    @Test
    void testUpdatePreorder_Testcase2() {
        ClientPreorderRequest request = new ClientPreorderRequest();
        request.setUserId(1L);
        request.setProductId(2L);

        when(preorderRepository.findByUser_IdAndProduct_Id(1L, 2L)).thenReturn(Optional.empty());

        // Mong muốn ném ra exception khi không tìm thấy preorder
        assertThrows(ResourceNotFoundException.class, () -> controller.updatePreorder(request));
    }

    // ✅ Test xóa preorder theo danh sách ID
    @Test
    void testDeletePreorders_Testcase1() {
        List<Long> ids = List.of(1L, 2L, 3L);
        doNothing().when(preorderRepository).deleteAllById(ids);

        ResponseEntity<Void> response = controller.deletePreorders(ids);

        // Trả về NO_CONTENT khi xóa thành công
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(preorderRepository).deleteAllById(ids);
    }
}
