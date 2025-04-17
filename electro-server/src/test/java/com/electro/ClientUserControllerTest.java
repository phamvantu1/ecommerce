package com.electro;

import com.electro.dto.authentication.UserResponse;
import com.electro.dto.address.AddressResponse;
import com.electro.entity.authentication.User;
import com.electro.mapper.authentication.UserMapper;
import com.electro.repository.authentication.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ClientUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;
    private UserResponse userResponse;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        // Kiểm tra userMapper không null
        assertNotNull(userMapper, "UserMapper mock must not be null");

        // Kiểm tra user "patruong" tồn tại trong database
        Optional<User> userOptional = userRepository.findByUsername("patruong");
        assertTrue(userOptional.isPresent(), "User 'patruong' phải tồn tại trong database");
        user = userOptional.get();
        assertTrue(user.getRoles().stream().anyMatch(role -> "CUSTOMER".equals(role.getCode())),
                "User 'patruong' phải có vai trò CUSTOMER");

        // In password để kiểm tra (debug)
        System.out.println("Password of 'patruong' in DB: " + user.getPassword());

        // Khởi tạo UserResponse giả lập
        userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setCreatedAt(Instant.now());
        userResponse.setUpdatedAt(Instant.now());
        userResponse.setUsername("patruong");
        userResponse.setFullname("Pat Ruong");
        userResponse.setEmail("patruong@example.com");
        userResponse.setPhone("1234567890");
        userResponse.setGender("MALE");
        userResponse.setAddress(new AddressResponse());
        userResponse.setAvatar("avatar.jpg");
        userResponse.setStatus(1);
        userResponse.setRoles(Collections.emptySet());

        // TODO: Kiểm tra database: user 'patruong' phải có password mã hóa BCrypt của '123456'
        // Tạm dùng JWT giả lập để test
        jwtToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwYXRydW9uZyIsImlhdCI6MTc0NDg5NjMwMCwiZXhwIjoxNzQ0OTA2MzgwfQ.DFsZn9NZxEHal80_TJwphiWF0f8wZmVIt5fVNiyvilQFi6lLVizBwZEjzVPlHdUpNqn4Df0IbKg5js_CPafU8g";
    }

    @Test
    void testGetUserInfo_Success() throws Exception {
        // Arrange
        when(userRepository.findByUsername("patruong")).thenReturn(Optional.of(user));
        when(userMapper.entityToResponse(any(User.class))).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(get("/client-api/users/info")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value("patruong"))
                .andExpect(jsonPath("$.fullname").value("Pat Ruong"))
                .andExpect(jsonPath("$.email").value("patruong@example.com"))
                .andExpect(jsonPath("$.phone").value("1234567890"))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.avatar").value("avatar.jpg"))
                .andExpect(jsonPath("$.status").value(1));

        // Verify
        verify(userRepository).findByUsername("patruong");
        verify(userMapper).entityToResponse(any(User.class));
    }

    @Test
    void testGetUserInfo_NotFound() throws Exception {
        // Arrange
        when(userRepository.findByUsername("patruong")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/client-api/users/info")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("patruong"));

        // Verify
        verify(userRepository).findByUsername("patruong");
        verify(userMapper, never()).entityToResponse(any(User.class));
    }

    @Test
    void testGetUserInfo_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/client-api/users/info")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // Verify
        verify(userRepository, never()).findByUsername(any());
        verify(userMapper, never()).entityToResponse(any(User.class));
    }
}