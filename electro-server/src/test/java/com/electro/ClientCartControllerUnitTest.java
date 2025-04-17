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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetCart_WhenCartExists_ShouldReturnCartResponse() {
        String username = "testuser";
        Cart cart = new Cart(); // your Cart mock
        ClientCartResponse responseDto = new ClientCartResponse();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode expectedNode = mapper.createObjectNode().put("mock", "value");

        when(authentication.getName()).thenReturn(username);
        when(cartRepository.findByUsername(username)).thenReturn(Optional.of(cart));
        when(clientCartMapper.entityToResponse(cart)).thenReturn(responseDto);
        when(new ObjectMapper().convertValue(responseDto, ObjectNode.class)).thenReturn(expectedNode);

        ResponseEntity<ObjectNode> response = clientCartController.getCart(authentication);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    void testGetCart_WhenCartDoesNotExist_ShouldReturnEmptyObject() {
        String username = "nouser";

        when(authentication.getName()).thenReturn(username);
        when(cartRepository.findByUsername(username)).thenReturn(Optional.empty());

        ResponseEntity<ObjectNode> response = clientCartController.getCart(authentication);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testSaveCart_WhenCartIdIsNull_ShouldCreateNewCart() {
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(null);

        Cart cartEntity = new Cart();
        ClientCartResponse responseDto = new ClientCartResponse();

        when(clientCartMapper.requestToEntity(request)).thenReturn(cartEntity);
        when(cartRepository.save(cartEntity)).thenReturn(cartEntity);
        when(clientCartMapper.entityToResponse(cartEntity)).thenReturn(responseDto);

        ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(responseDto, response.getBody());
    }

    @Test
    void testSaveCart_WhenCartIdExistsAndFound_ShouldUpdateCart() {
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(1L);

        Cart existingCart = new Cart();
        Cart updatedCart = new Cart();
        ClientCartResponse responseDto = new ClientCartResponse();

        when(cartRepository.findById(1L)).thenReturn(Optional.of(existingCart));
        when(clientCartMapper.partialUpdate(existingCart, request)).thenReturn(updatedCart);
        when(cartRepository.save(updatedCart)).thenReturn(updatedCart);
        when(clientCartMapper.entityToResponse(updatedCart)).thenReturn(responseDto);

        ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(responseDto, response.getBody());
    }

    @Test
    void testSaveCart_WhenCartIdExistsAndNotFound_ShouldThrowException() {
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(99L);

        when(cartRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clientCartController.saveCart(request));
    }

    @Test
    void testDeleteCartItems_ShouldDeleteVariantsAndReturnNoContent() {
        ClientCartVariantKeyRequest keyRequest = new ClientCartVariantKeyRequest();
        keyRequest.setCartId(1L);
        keyRequest.setVariantId(2L);
        List<ClientCartVariantKeyRequest> requestList = List.of(keyRequest);

        ResponseEntity<Void> response = clientCartController.deleteCartItems(requestList);

        verify(cartVariantRepository).deleteAllById(anyList());
        assertEquals(204, response.getStatusCodeValue());
    }
    @Test
    void testSaveCart_WhenQuantityExceedsInventory_ShouldThrowRuntimeException() {
        // Mock request
        ClientCartRequest request = new ClientCartRequest();
        request.setCartId(null); // Simulate creating a new cart

        // Mock cart and cart variants
        Cart cartEntity = new Cart();
        CartVariant cartVariant = new CartVariant();
        cartVariant.setQuantity(10); // Quantity exceeds inventory
        CartVariantKey cartVariantKey = new CartVariantKey();
        cartVariantKey.setVariantId(1L);
        cartVariant.setCartVariantKey(cartVariantKey);
        cartEntity.setCartVariants(Set.of(cartVariant));

        // Mock inventory calculation
        List<DocketVariant> mockTransactions = new ArrayList<>(); // Ensure non-null list
        when(clientCartMapper.requestToEntity(request)).thenReturn(cartEntity);
        when(docketVariantRepository.findByVariantId(1L)).thenReturn(mockTransactions);
        when(InventoryUtils.calculateInventoryIndices(mockTransactions))
                .thenReturn(Map.of("canBeSold", 5));

        // Assert exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> clientCartController.saveCart(request));
        assertEquals("Variant quantity cannot greater than variant inventory", exception.getMessage());

        // Verify interactions
        verify(docketVariantRepository).findByVariantId(1L);

    }
}
