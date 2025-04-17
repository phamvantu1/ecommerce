package com.electro;

import com.electro.controller.client.ClientUserController;
import com.electro.dto.authentication.UserResponse;
import com.electro.dto.client.ClientEmailSettingUserRequest;
import com.electro.dto.client.ClientPasswordSettingUserRequest;
import com.electro.dto.client.ClientPersonalSettingUserRequest;
import com.electro.dto.client.ClientPhoneSettingUserRequest;
import com.electro.entity.authentication.User;
import com.electro.mapper.authentication.UserMapper;
import com.electro.repository.authentication.UserRepository;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientUserControllerUnitTest {

    @InjectMocks
    private ClientUserController clientUserController;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetUserInfo_ShouldReturnUserInfo() {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập repository trả về User
        User user = new User();
        user.setUsername("testUsername");
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername("testUsername");

        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(user));
        when(userMapper.entityToResponse(user)).thenReturn(userResponse);

        // Gọi phương thức và kiểm tra kết quả
        ResponseEntity<UserResponse> response = clientUserController.getUserInfo(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testUsername", response.getBody().getUsername());
    }

    @Test
    void testUpdatePersonalSetting_ShouldUpdateUserInfo() {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập dữ liệu đầu vào
        ClientPersonalSettingUserRequest userRequest = new ClientPersonalSettingUserRequest();
        userRequest.setFullname("NewFirstName");

        // Giả lập repository trả về User
        User existingUser = new User();
        existingUser.setUsername("testUsername");
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername("testUsername");

        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(existingUser));
        when(userMapper.partialUpdate(existingUser, userRequest)).thenReturn(existingUser);
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.entityToResponse(existingUser)).thenReturn(userResponse);

        // Gọi phương thức và kiểm tra kết quả
        ResponseEntity<UserResponse> response = clientUserController.updatePersonalSetting(authentication, userRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testUsername", response.getBody().getUsername());
    }

    @Test
    void testUpdatePhoneSetting_ShouldUpdateUserPhone() {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập dữ liệu đầu vào
        ClientPhoneSettingUserRequest userRequest = new ClientPhoneSettingUserRequest();
        userRequest.setPhone("1234567890");

        // Giả lập repository trả về User
        User existingUser = new User();
        existingUser.setUsername("testUsername");
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername("testUsername");

        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(existingUser));
        when(userMapper.partialUpdate(existingUser, userRequest)).thenReturn(existingUser);
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.entityToResponse(existingUser)).thenReturn(userResponse);

        // Gọi phương thức và kiểm tra kết quả
        ResponseEntity<UserResponse> response = clientUserController.updatePhoneSetting(authentication, userRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testUsername", response.getBody().getUsername());
    }

    @Test
    void testUpdateEmailSetting_ShouldUpdateUserEmail() {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập dữ liệu đầu vào
        ClientEmailSettingUserRequest userRequest = new ClientEmailSettingUserRequest();
        userRequest.setEmail("test@example.com");

        // Giả lập repository trả về User
        User existingUser = new User();
        existingUser.setUsername("testUsername");
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername("testUsername");

        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(existingUser));
        when(userMapper.partialUpdate(existingUser, userRequest)).thenReturn(existingUser);
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.entityToResponse(existingUser)).thenReturn(userResponse);

        // Gọi phương thức và kiểm tra kết quả
        ResponseEntity<UserResponse> response = clientUserController.updateEmailSetting(authentication, userRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testUsername", response.getBody().getUsername());
    }

    @Test
    void testUpdatePasswordSetting_ShouldUpdateUserPassword() throws Exception {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập dữ liệu đầu vào
        ClientPasswordSettingUserRequest userRequest = new ClientPasswordSettingUserRequest();
        userRequest.setOldPassword("oldPassword");
        userRequest.setNewPassword("newPassword");

        // Giả lập repository trả về User
        User existingUser = new User();
        existingUser.setUsername("testUsername");
        existingUser.setPassword("oldPassword");

        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(userRequest.getOldPassword(), existingUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(userRequest.getNewPassword())).thenReturn("encodedNewPassword");
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        // Gọi phương thức và kiểm tra kết quả
        ResponseEntity<ObjectNode> response = clientUserController.updatePasswordSetting(authentication, userRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isObject());
    }

    @Test
    void testUpdatePasswordSetting_ShouldThrowExceptionWhenOldPasswordDoesNotMatch() throws Exception {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập dữ liệu đầu vào
        ClientPasswordSettingUserRequest userRequest = new ClientPasswordSettingUserRequest();
        userRequest.setOldPassword("wrongOldPassword");
        userRequest.setNewPassword("newPassword");

        // Giả lập repository trả về User
        User existingUser = new User();
        existingUser.setUsername("testUsername");
        existingUser.setPassword("oldPassword");

        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(userRequest.getOldPassword(), existingUser.getPassword())).thenReturn(false);

        // Gọi phương thức và kiểm tra kết quả
        Exception exception = assertThrows(Exception.class, () -> {
            clientUserController.updatePasswordSetting(authentication, userRequest);
        });

        assertEquals("Wrong old password", exception.getMessage());
    }

    @Test
    void testGetUserInfo_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập không tìm thấy User trong DB
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.empty());

        // Gọi phương thức và kiểm tra exception
        assertThrows(UsernameNotFoundException.class, () -> {
            clientUserController.getUserInfo(authentication);
        });
    }

    @Test
    void testUpdatePersonalSetting_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập dữ liệu đầu vào
        ClientPersonalSettingUserRequest userRequest = new ClientPersonalSettingUserRequest();
        userRequest.setFullname("NewFirstName");

        // Giả lập không tìm thấy User trong DB
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.empty());

        // Gọi phương thức và kiểm tra exception
        assertThrows(UsernameNotFoundException.class, () -> {
            clientUserController.updatePersonalSetting(authentication, userRequest);
        });
    }

    @Test
    void testUpdatePhoneSetting_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập dữ liệu đầu vào
        ClientPhoneSettingUserRequest userRequest = new ClientPhoneSettingUserRequest();
        userRequest.setPhone("1234567890");

        // Giả lập không tìm thấy User trong DB
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.empty());

        // Gọi phương thức và kiểm tra exception
        assertThrows(UsernameNotFoundException.class, () -> {
            clientUserController.updatePhoneSetting(authentication, userRequest);
        });
    }

    @Test
    void testUpdateEmailSetting_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập dữ liệu đầu vào
        ClientEmailSettingUserRequest userRequest = new ClientEmailSettingUserRequest();
        userRequest.setEmail("test@example.com");

        // Giả lập không tìm thấy User trong DB
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.empty());

        // Gọi phương thức và kiểm tra exception
        assertThrows(UsernameNotFoundException.class, () -> {
            clientUserController.updateEmailSetting(authentication, userRequest);
        });
    }

    @Test
    void testUpdatePasswordSetting_ShouldThrowException_WhenOldPasswordIsWrong() throws Exception {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập dữ liệu đầu vào
        ClientPasswordSettingUserRequest userRequest = new ClientPasswordSettingUserRequest();
        userRequest.setOldPassword("wrongOldPassword");
        userRequest.setNewPassword("newPassword");

        // Giả lập repository trả về User
        User existingUser = new User();
        existingUser.setUsername("testUsername");
        existingUser.setPassword("oldPassword");

        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(userRequest.getOldPassword(), existingUser.getPassword())).thenReturn(false);

        // Gọi phương thức và kiểm tra exception
        Exception exception = assertThrows(Exception.class, () -> {
            clientUserController.updatePasswordSetting(authentication, userRequest);
        });

        assertEquals("Wrong old password", exception.getMessage());
    }

    @Test
    void testUpdatePasswordSetting_ShouldThrowUsernameNotFoundException_WhenUserNotFound() throws Exception {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập dữ liệu đầu vào
        ClientPasswordSettingUserRequest userRequest = new ClientPasswordSettingUserRequest();
        userRequest.setOldPassword("oldPassword");
        userRequest.setNewPassword("newPassword");

        // Giả lập không tìm thấy User trong DB
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.empty());

        // Gọi phương thức và kiểm tra exception
        assertThrows(UsernameNotFoundException.class, () -> {
            clientUserController.updatePasswordSetting(authentication, userRequest);
        });
    }

    @Test
    void testUpdatePasswordSetting_ShouldReturnEmptyJson_WhenPasswordUpdatedSuccessfully() throws Exception {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập dữ liệu đầu vào
        ClientPasswordSettingUserRequest userRequest = new ClientPasswordSettingUserRequest();
        userRequest.setOldPassword("oldPassword");
        userRequest.setNewPassword("newPassword");

        // Giả lập repository trả về User
        User existingUser = new User();
        existingUser.setUsername("testUsername");
        existingUser.setPassword("oldPassword");

        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(userRequest.getOldPassword(), existingUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(userRequest.getNewPassword())).thenReturn("encodedNewPassword");
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        // Gọi phương thức và kiểm tra kết quả
        ResponseEntity<ObjectNode> response = clientUserController.updatePasswordSetting(authentication, userRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isObject());
    }
}

