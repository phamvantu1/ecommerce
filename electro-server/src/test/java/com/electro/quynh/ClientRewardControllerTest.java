package com.electro.quynh;

import com.electro.constant.AppConstants;
import com.electro.controller.client.ClientRewardController;
import com.electro.dto.client.ClientRewardLogResponse;
import com.electro.dto.client.ClientRewardResponse;
import com.electro.entity.reward.RewardLog;
import com.electro.mapper.client.ClientRewardLogMapper;
import com.electro.repository.reward.RewardLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho ClientRewardController, kiểm tra chức năng lấy thông tin phần thưởng của người dùng.
 * Sử dụng Mockito để giả lập repository và mapper, đảm bảo kiểm tra logic độc lập với DB.
 */
class ClientRewardControllerTest {

    @InjectMocks
    private ClientRewardController controller;

    @Mock
    private RewardLogRepository rewardLogRepository;

    @Mock
    private ClientRewardLogMapper clientRewardLogMapper;

    @Mock
    private Authentication authentication;

    /**
     * Thiết lập trước mỗi test case: khởi tạo mock.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Test lấy thông tin phần thưởng khi có nhật ký phần thưởng.
     * Kịch bản: Người dùng đã đăng nhập, có bản ghi RewardLog, trả về tổng điểm và danh sách sắp xếp đúng.
     * Liên kết mã nguồn: Phương thức getReward (dòng 32-44).
     */
    @Test
    void getReward_WithRewardLogs_ReturnsTotalScoreAndSortedLogs() {
        // Chuẩn bị dữ liệu
        String username = "testuser";
        RewardLog log1 = new RewardLog();
        log1.setId(1L);
        RewardLog log2 = new RewardLog();
        log2.setId(2L);
        List<RewardLog> logs = List.of(log1, log2);
        ClientRewardLogResponse response1 = new ClientRewardLogResponse();
        ClientRewardLogResponse response2 = new ClientRewardLogResponse();
        when(authentication.getName()).thenReturn(username);
        when(rewardLogRepository.sumScoreByUsername(username)).thenReturn(100);
        when(rewardLogRepository.findByUserUsername(username)).thenReturn(logs);
        when(clientRewardLogMapper.entityToResponse(anyList())).thenReturn(List.of(response2, response1)); // Sắp xếp ngược

        // Thực hiện test
        ResponseEntity<ClientRewardResponse> result = controller.getReward(authentication);

        // Kiểm tra kết quả
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(100, result.getBody().getRewardTotalScore());
        assertEquals(2, result.getBody().getRewardLogs().size());
        assertEquals(response2, result.getBody().getRewardLogs().get(0)); // Log có id lớn hơn đứng đầu
        assertEquals(response1, result.getBody().getRewardLogs().get(1));
        // Kiểm tra DB: Đảm bảo gọi repository đúng
        verify(rewardLogRepository).sumScoreByUsername(username);
        verify(rewardLogRepository).findByUserUsername(username);
        verify(rewardLogRepository, never()).save(any());
    }

    /**
     * Test lấy thông tin phần thưởng khi không có nhật ký phần thưởng.
     * Kịch bản: Người dùng đã đăng nhập, không có RewardLog, trả về tổng điểm 0 và danh sách rỗng.
     * Liên kết mã nguồn: Phương thức getReward (dòng 32-44).
     */
    @Test
    void getReward_NoRewardLogs_ReturnsZeroScoreAndEmptyList() {
        // Chuẩn bị dữ liệu
        String username = "testuser";
        when(authentication.getName()).thenReturn(username);
        when(rewardLogRepository.sumScoreByUsername(username)).thenReturn(0);
        when(rewardLogRepository.findByUserUsername(username)).thenReturn(Collections.emptyList());
        when(clientRewardLogMapper.entityToResponse(anyList())).thenReturn(Collections.emptyList());

        // Thực hiện test
        ResponseEntity<ClientRewardResponse> result = controller.getReward(authentication);

        // Kiểm tra kết quả
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getRewardTotalScore());
        assertTrue(result.getBody().getRewardLogs().isEmpty());
        // Kiểm tra DB: Đảm bảo gọi repository đúng
        verify(rewardLogRepository).sumScoreByUsername(username);
        verify(rewardLogRepository).findByUserUsername(username);
        verify(rewardLogRepository, never()).save(any());
    }

    /**
     * Test lấy thông tin phần thưởng khi chưa đăng nhập.
     * Kịch bản: Authentication là null, ném AuthenticationCredentialsNotFoundException.
     * Liên kết mã nguồn: Phương thức getReward (dòng 32, trước khi lấy username).
     */
    @Test
    void getReward_NotAuthenticated_ThrowsException() {
        // Chuẩn bị dữ liệu
        when(authentication.getName()).thenThrow(new AuthenticationCredentialsNotFoundException("Not authenticated"));

        // Thực hiện test
        AuthenticationCredentialsNotFoundException exception = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> controller.getReward(authentication));
        assertEquals("Not authenticated", exception.getMessage());
        // Kiểm tra DB: Đảm bảo không gọi repository
        verify(rewardLogRepository, never()).sumScoreByUsername(any());
        verify(rewardLogRepository, never()).findByUserUsername(any());
        verify(rewardLogRepository, never()).save(any());
    }

    /**
     * Test lấy thông tin phần thưởng khi lỗi tính tổng điểm.
     * Kịch bản: Lỗi DB khi gọi sumScoreByUsername, ném RuntimeException.
     * Liên kết mã nguồn: Phương thức getReward (dòng 34).
     */
    @Test
    void getReward_SumScoreFails_ThrowsException() {
        // Chuẩn bị dữ liệu
        String username = "testuser";
        when(authentication.getName()).thenReturn(username);
        when(rewardLogRepository.sumScoreByUsername(username)).thenThrow(new RuntimeException("DB error"));

        // Thực hiện test
        assertThrows(RuntimeException.class, () -> controller.getReward(authentication));
        // Kiểm tra DB: Đảm bảo chỉ gọi sumScoreByUsername, không gọi findByUserUsername
        verify(rewardLogRepository).sumScoreByUsername(username);
        verify(rewardLogRepository, never()).findByUserUsername(any());
        verify(rewardLogRepository, never()).save(any());
    }

    /**
     * Test lấy thông tin phần thưởng khi lỗi lấy danh sách RewardLog.
     * Kịch bản: Lỗi DB khi gọi findByUserUsername, ném RuntimeException.
     * Liên kết mã nguồn: Phương thức getReward (dòng 36-39).
     */
    @Test
    void getReward_FetchLogsFails_ThrowsException() {
        // Chuẩn bị dữ liệu
        String username = "testuser";
        when(authentication.getName()).thenReturn(username);
        when(rewardLogRepository.sumScoreByUsername(username)).thenReturn(100);
        when(rewardLogRepository.findByUserUsername(username)).thenThrow(new RuntimeException("DB error"));

        // Thực hiện test
        assertThrows(RuntimeException.class, () -> controller.getReward(authentication));
        // Kiểm tra DB: Đảm bảo gọi cả sumScoreByUsername và findByUserUsername
        verify(rewardLogRepository).sumScoreByUsername(username);
        verify(rewardLogRepository).findByUserUsername(username);
        verify(rewardLogRepository, never()).save(any());
    }

    /**
     * Test lấy thông tin phần thưởng với nhiều bản ghi RewardLog.
     * Kịch bản: Có nhiều RewardLog, đảm bảo sắp xếp đúng thứ tự id giảm dần.
     * Liên kết mã nguồn: Phương thức getReward (dòng 36-39, sắp xếp).
     */
    @Test
    void getReward_MultipleRewardLogs_ReturnsSortedList() {
        // Chuẩn bị dữ liệu
        String username = "testuser";
        RewardLog log1 = new RewardLog();
        log1.setId(1L);
        RewardLog log2 = new RewardLog();
        log2.setId(2L);
        RewardLog log3 = new RewardLog();
        log3.setId(3L);
        List<RewardLog> logs = List.of(log1, log2, log3); // Đầu vào không sắp xếp
        ClientRewardLogResponse response1 = new ClientRewardLogResponse();
        ClientRewardLogResponse response2 = new ClientRewardLogResponse();
        ClientRewardLogResponse response3 = new ClientRewardLogResponse();
        when(authentication.getName()).thenReturn(username);
        when(rewardLogRepository.sumScoreByUsername(username)).thenReturn(300);
        when(rewardLogRepository.findByUserUsername(username)).thenReturn(logs);
        when(clientRewardLogMapper.entityToResponse(anyList())).thenReturn(List.of(response3, response2, response1)); // Sắp xếp ngược

        // Thực hiện test
        ResponseEntity<ClientRewardResponse> result = controller.getReward(authentication);

        // Kiểm tra kết quả
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(300, result.getBody().getRewardTotalScore());
        assertEquals(3, result.getBody().getRewardLogs().size());
        assertEquals(response3, result.getBody().getRewardLogs().get(0)); // id=3
        assertEquals(response2, result.getBody().getRewardLogs().get(1)); // id=2
        assertEquals(response1, result.getBody().getRewardLogs().get(2)); // id=1
        // Kiểm tra DB: Đảm bảo gọi repository đúng
        verify(rewardLogRepository).sumScoreByUsername(username);
        verify(rewardLogRepository).findByUserUsername(username);
        verify(rewardLogRepository, never()).save(any());
    }
}