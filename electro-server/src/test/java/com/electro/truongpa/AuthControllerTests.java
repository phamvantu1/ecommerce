package com.electro.truongpa;

import com.electro.dto.authentication.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit test cho chức năng đăng nhập của AuthController
 * Kiểm tra các tình huống đúng - sai - thiếu dữ liệu - bất thường
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTests {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Trường hợp thành công:
     * Username + password hợp lệ
     * Phản hồi phải trả về HTTP 200 và đầy đủ trường JSON
     */
    @Test
    public void testLoginSuccess() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("patruong");
        loginRequest.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login success!"))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    /**
     * Mật khẩu sai:
     * Dự kiến trả về HTTP 401 Unauthorized
     */
    @Test
    public void testLoginWrongPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("patruong");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Bad credentials"));
    }

    /**
     * Username không tồn tại:
     * Dự kiến trả về HTTP 401 Unauthorized
     */
    @Test
    public void testLoginUserNotFound() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistentuser");
        loginRequest.setPassword("any");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Bad credentials"));
    }

    /**
     * Thiếu username trong request
     */
    @Test
    public void testLoginMissingUsername() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Thiếu password trong request
     */
    @Test
    public void testLoginMissingPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("patruong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Cả username và password đều rỗng
     */
    @Test
    public void testLoginEmptyCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("");
        loginRequest.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Username chứa ký tự đặc biệt
     * Tùy vào logic server có thể cho phép hoặc từ chối (ví dụ: regex validate)
     */
    @Test
    public void testLoginUsernameWithSpecialChars() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("user!@#");
        loginRequest.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()) // có thể là BadRequest nếu có validation rõ ràng
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Token trả về sai format (giả định test phía frontend hoặc sau decode)
     * (chỉ test khi bạn muốn mô phỏng decode/token validation ở client)
     * Nếu chỉ test đăng nhập thì có thể bỏ
     */
    @Test
    public void testLoginWithMockedInvalidTokenFormat() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("patruong");
        loginRequest.setPassword("123456");

        // Nếu server trả về token sai format, client có thể decode bị lỗi
        // Nhưng phía backend rất hiếm khi test như vậy trừ khi bạn tự kiểm chứng sau token
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(org.hamcrest.Matchers.containsString("."))); // token phải có dấu chấm
    }
}
