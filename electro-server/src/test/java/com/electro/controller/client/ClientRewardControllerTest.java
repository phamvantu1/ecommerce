package com.electro.controller.client;

import com.electro.constant.AppConstants;
import com.electro.controller.client.ClientRewardController;
import com.electro.dto.client.ClientRewardLogResponse;
import com.electro.dto.client.ClientRewardResponse;
import com.electro.entity.authentication.User;
import com.electro.entity.reward.RewardLog;
import com.electro.mapper.client.ClientRewardLogMapper;
import com.electro.repository.reward.RewardLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientRewardControllerTest {

    @Mock
    private RewardLogRepository rewardLogRepository; // Mock repository để quản lý RewardLog

    @Mock
    private ClientRewardLogMapper clientRewardLogMapper; // Mock mapper để ánh xạ RewardLog sang DTO

    @Mock
    private Authentication authentication; // Mock Authentication để mô phỏng người dùng đăng nhập

    @InjectMocks
    private ClientRewardController clientRewardController; // Inject các mock vào ClientRewardController

    private User user; // Đối tượng User dùng cho các test case
    private RewardLog rewardLog1; // Đối tượng RewardLog thứ nhất
    private RewardLog rewardLog2; // Đối tượng RewardLog thứ hai

    @BeforeEach
    void setUp() {
        // Khởi tạo đối tượng User
        user = new User(); // Tạo mới đối tượng User
        user.setUsername("testuser"); // Gán username

        // Khởi tạo RewardLog thứ nhất
        rewardLog1 = new RewardLog(); // Tạo mới RewardLog
        rewardLog1.setId(1L); // Gán ID
        rewardLog1.setUser(user); // Gán User
        rewardLog1.setScore(50); // Gán điểm thưởng

        // Khởi tạo RewardLog thứ hai
        rewardLog2 = new RewardLog(); // Tạo mới RewardLog
        rewardLog2.setId(2L); // Gán ID
        rewardLog2.setUser(user); // Gán User
        rewardLog2.setScore(50); // Gán điểm thưởng
    }

    // Test case cho getReward - Thành công với danh sách nhật ký và điểm dương
    @Test
    void getReward_WithRewardLogs_ReturnsTotalScoreAndSortedLogs() {
        // Mục đích: Kiểm tra lấy thông tin điểm thưởng thành công với danh sách nhật ký và tổng điểm dương
        // Arrange
        when(authentication.getName()).thenReturn("testuser"); // Mock username từ Authentication
        when(rewardLogRepository.sumScoreByUsername("testuser")).thenReturn(100); // Mock tổng điểm là 100
        when(rewardLogRepository.findByUserUsername("testuser")).thenReturn(List.of(rewardLog1, rewardLog2)); // Mock danh sách RewardLog
        ClientRewardLogResponse response1 = new ClientRewardLogResponse(); // Tạo DTO cho log 1
        response1.setRewardLogScore(50); // Gán điểm cho log 1
        ClientRewardLogResponse response2 = new ClientRewardLogResponse(); // Tạo DTO cho log 2
        response2.setRewardLogScore(50); // Gán điểm cho log 2
        when(clientRewardLogMapper.entityToResponse(anyList())).thenReturn(List.of(response2, response1)); // Mock ánh xạ, sắp xếp ngược theo ID

        // Act
        ResponseEntity<ClientRewardResponse> result = clientRewardController.getReward(authentication); // Gọi phương thức getReward

        // Assert
        verify(authentication).getName(); // Kiểm tra gọi lấy username
        verify(rewardLogRepository).sumScoreByUsername("testuser"); // Kiểm tra gọi tính tổng điểm
        verify(rewardLogRepository).findByUserUsername("testuser"); // Kiểm tra gọi tìm RewardLog
        verify(clientRewardLogMapper).entityToResponse(anyList()); // Kiểm tra gọi ánh xạ
        verify(rewardLogRepository, never()).save(any()); // Kiểm tra không gọi save
        assertEquals(HttpStatus.OK, result.getStatusCode()); // Kiểm tra mã trạng thái HTTP là OK
        assertNotNull(result.getBody()); // Kiểm tra body không null
        assertEquals(100, result.getBody().getRewardTotalScore()); // Kiểm tra tổng điểm là 100
        assertEquals(2, result.getBody().getRewardLogs().size()); // Kiểm tra danh sách log có 2 phần tử
        assertEquals(response2, result.getBody().getRewardLogs().get(0)); // Kiểm tra log ID=2 đứng đầu
        assertEquals(response1, result.getBody().getRewardLogs().get(1)); // Kiểm tra log ID=1 đứng sau
    }

    // Test case cho getReward - Thành công với danh sách nhật ký rỗng và điểm bằng 0
    @Test
    void getReward_NoRewardLogs_ReturnsZeroScoreAndEmptyList() {
        // Mục đích: Kiểm tra lấy thông tin điểm thưởng khi không có nhật ký và tổng điểm là 0
        // Arrange
        when(authentication.getName()).thenReturn("testuser"); // Mock username từ Authentication
        when(rewardLogRepository.sumScoreByUsername("testuser")).thenReturn(0); // Mock tổng điểm là 0
        when(rewardLogRepository.findByUserUsername("testuser")).thenReturn(Collections.emptyList()); // Mock danh sách RewardLog rỗng
        when(clientRewardLogMapper.entityToResponse(anyList())).thenReturn(Collections.emptyList()); // Mock ánh xạ trả về danh sách rỗng

        // Act
        ResponseEntity<ClientRewardResponse> result = clientRewardController.getReward(authentication); // Gọi phương thức getReward

        // Assert
        verify(authentication).getName(); // Kiểm tra gọi lấy username
        verify(rewardLogRepository).sumScoreByUsername("testuser"); // Kiểm tra gọi tính tổng điểm
        verify(rewardLogRepository).findByUserUsername("testuser"); // Kiểm tra gọi tìm RewardLog
        verify(clientRewardLogMapper).entityToResponse(anyList()); // Kiểm tra gọi ánh xạ
        verify(rewardLogRepository, never()).save(any()); // Kiểm tra không gọi save
        assertEquals(HttpStatus.OK, result.getStatusCode()); // Kiểm tra mã trạng thái HTTP là OK
        assertNotNull(result.getBody()); // Kiểm tra body không null
        assertEquals(0, result.getBody().getRewardTotalScore()); // Kiểm tra tổng điểm là 0
        assertTrue(result.getBody().getRewardLogs().isEmpty()); // Kiểm tra danh sách log rỗng
    }

    // Test case cho getReward - Thành công với tổng điểm âm
    @Test
    void getReward_NegativeScore_ReturnsNegativeScoreAndLogs() {
        // Mục đích: Kiểm tra lấy thông tin điểm thưởng khi tổng điểm âm
        // Arrange
        when(authentication.getName()).thenReturn("testuser"); // Mock username từ Authentication
        when(rewardLogRepository.sumScoreByUsername("testuser")).thenReturn(-50); // Mock tổng điểm là -50
        when(rewardLogRepository.findByUserUsername("testuser")).thenReturn(List.of(rewardLog1)); // Mock danh sách RewardLog
        ClientRewardLogResponse response1 = new ClientRewardLogResponse(); // Tạo DTO cho log
        response1.setRewardLogScore(-50); // Gán điểm âm
        when(clientRewardLogMapper.entityToResponse(anyList())).thenReturn(List.of(response1)); // Mock ánh xạ

        // Act
        ResponseEntity<ClientRewardResponse> result = clientRewardController.getReward(authentication); // Gọi phương thức getReward

        // Assert
        verify(authentication).getName(); // Kiểm tra gọi lấy username
        verify(rewardLogRepository).sumScoreByUsername("testuser"); // Kiểm tra gọi tính tổng điểm
        verify(rewardLogRepository).findByUserUsername("testuser"); // Kiểm tra gọi tìm RewardLog
        verify(clientRewardLogMapper).entityToResponse(anyList()); // Kiểm tra gọi ánh xạ
        verify(rewardLogRepository, never()).save(any()); // Kiểm tra không gọi save
        assertEquals(HttpStatus.OK, result.getStatusCode()); // Kiểm tra mã trạng thái HTTP là OK
        assertNotNull(result.getBody()); // Kiểm tra body không null
        assertEquals(-50, result.getBody().getRewardTotalScore()); // Kiểm tra tổng điểm là -50
        assertEquals(1, result.getBody().getRewardLogs().size()); // Kiểm tra danh sách log có 1 phần tử
        assertEquals(-50, result.getBody().getRewardLogs().get(0).getRewardLogScore()); // Kiểm tra điểm của log
    }

    // Test case cho getReward - Authentication không hợp lệ
    @Test
    void getReward_NotAuthenticated_ThrowsException() {
        // Mục đích: Kiểm tra trường hợp Authentication là null hoặc không hợp lệ, ném AuthenticationCredentialsNotFoundException
        // Arrange
        // Không stub authentication.getName() vì chúng ta truyền null trực tiếp

        // Act & Assert
        AuthenticationCredentialsNotFoundException exception = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> clientRewardController.getReward(null)); // Gọi getReward với authentication null
        assertEquals("Authentication is required", exception.getMessage()); // Kiểm tra thông điệp ngoại lệ
        verifyNoInteractions(authentication, rewardLogRepository, clientRewardLogMapper); // Kiểm tra không tương tác với bất kỳ mock nào
    }

    // Test case cho getReward - Lỗi database khi tính tổng điểm
    @Test
    void getReward_SumScoreFails_ThrowsException() {
        // Mục đích: Kiểm tra trường hợp lỗi database khi tính tổng điểm, ném RuntimeException
        // Arrange
        when(authentication.getName()).thenReturn("testuser"); // Mock username từ Authentication
        when(rewardLogRepository.sumScoreByUsername("testuser")).thenThrow(new RuntimeException("DB error")); // Mock sumScoreByUsername ném ngoại lệ

        // Act & Assert
        assertThrows(RuntimeException.class, () -> clientRewardController.getReward(authentication)); // Kiểm tra ném RuntimeException
        verify(authentication).getName(); // Kiểm tra gọi lấy username
        verify(rewardLogRepository).sumScoreByUsername("testuser"); // Kiểm tra gọi tính tổng điểm
        verify(rewardLogRepository, never()).findByUserUsername(anyString()); // Kiểm tra không gọi tìm RewardLog
        verify(clientRewardLogMapper, never()).entityToResponse(anyList()); // Kiểm tra không gọi ánh xạ
        verify(rewardLogRepository, never()).save(any()); // Kiểm tra không gọi save
    }

    // Test case cho getReward - Lỗi database khi lấy danh sách RewardLog
    @Test
    void getReward_FetchLogsFails_ThrowsException() {
        // Mục đích: Kiểm tra trường hợp lỗi database khi lấy RewardLog, ném RuntimeException
        // Arrange
        when(authentication.getName()).thenReturn("testuser"); // Mock username từ Authentication
        when(rewardLogRepository.sumScoreByUsername("testuser")).thenReturn(100); // Mock tổng điểm là 100
        when(rewardLogRepository.findByUserUsername("testuser")).thenThrow(new RuntimeException("DB error")); // Mock findByUserUsername ném ngoại lệ

        // Act & Assert
        assertThrows(RuntimeException.class, () -> clientRewardController.getReward(authentication)); // Kiểm tra ném RuntimeException
        verify(authentication).getName(); // Kiểm tra gọi lấy username
        verify(rewardLogRepository).sumScoreByUsername("testuser"); // Kiểm tra gọi tính tổng điểm
        verify(rewardLogRepository).findByUserUsername("testuser"); // Kiểm tra gọi tìm RewardLog
        verify(clientRewardLogMapper, never()).entityToResponse(anyList()); // Kiểm tra không gọi ánh xạ
        verify(rewardLogRepository, never()).save(any()); // Kiểm tra không gọi save
    }

    // Test case cho getReward - Repository trả về null
    @Test
    void getReward_RepositoryReturnsNull_ThrowsException() {
        // Mục đích: Kiểm tra trường hợp repository trả về null (lỗi bất ngờ), ném NullPointerException
        // Arrange
        when(authentication.getName()).thenReturn("testuser"); // Mock username từ Authentication
        when(rewardLogRepository.sumScoreByUsername("testuser")).thenReturn(0); // Mock tổng điểm là 0
        when(rewardLogRepository.findByUserUsername("testuser")).thenReturn(null); // Mock findByUserUsername trả về null

        // Act & Assert
        assertThrows(NullPointerException.class, () -> clientRewardController.getReward(authentication)); // Kiểm tra ném NullPointerException
        verify(authentication).getName(); // Kiểm tra gọi lấy username
        verify(rewardLogRepository).sumScoreByUsername("testuser"); // Kiểm tra gọi tính tổng điểm
        verify(rewardLogRepository).findByUserUsername("testuser"); // Kiểm tra gọi tìm RewardLog
        verify(clientRewardLogMapper, never()).entityToResponse(anyList()); // Kiểm tra không gọi ánh xạ
        verify(rewardLogRepository, never()).save(any()); // Kiểm tra không gọi save
    }

    // Test case cho getReward - Lỗi ánh xạ
    @Test
    void getReward_MappingError_ThrowsException() {
        // Mục đích: Kiểm tra trường hợp lỗi khi ánh xạ RewardLog sang DTO, ném RuntimeException
        // Arrange
        when(authentication.getName()).thenReturn("testuser"); // Mock username từ Authentication
        when(rewardLogRepository.sumScoreByUsername("testuser")).thenReturn(100); // Mock tổng điểm là 100
        when(rewardLogRepository.findByUserUsername("testuser")).thenReturn(List.of(rewardLog1)); // Mock danh sách RewardLog
        when(clientRewardLogMapper.entityToResponse(anyList())).thenThrow(new RuntimeException("Mapping error")); // Mock ánh xạ ném ngoại lệ

        // Act & Assert
        assertThrows(RuntimeException.class, () -> clientRewardController.getReward(authentication)); // Kiểm tra ném RuntimeException
        verify(authentication).getName(); // Kiểm tra gọi lấy username
        verify(rewardLogRepository).sumScoreByUsername("testuser"); // Kiểm tra gọi tính tổng điểm
        verify(rewardLogRepository).findByUserUsername("testuser"); // Kiểm tra gọi tìm RewardLog
        verify(clientRewardLogMapper).entityToResponse(anyList()); // Kiểm tra gọi ánh xạ
        verify(rewardLogRepository, never()).save(any()); // Kiểm tra không gọi save
    }

    // Test case cho getReward - Nhiều RewardLog, sắp xếp đúng
    @Test
    void getReward_MultipleRewardLogs_ReturnsSortedList() {
        // Mục đích: Kiểm tra lấy thông tin điểm thưởng với nhiều RewardLog, sắp xếp theo ID giảm dần
        // Arrange
        RewardLog log3 = new RewardLog(); // Tạo RewardLog thứ ba
        log3.setId(3L); // Gán ID
        log3.setUser(user); // Gán User
        log3.setScore(50); // Gán điểm thưởng
        when(authentication.getName()).thenReturn("testuser"); // Mock username từ Authentication
        when(rewardLogRepository.sumScoreByUsername("testuser")).thenReturn(150); // Mock tổng điểm là 150
        when(rewardLogRepository.findByUserUsername("testuser")).thenReturn(List.of(rewardLog1, rewardLog2, log3)); // Mock danh sách RewardLog
        ClientRewardLogResponse response1 = new ClientRewardLogResponse(); // Tạo DTO cho log 1
        response1.setRewardLogScore(50); // Gán điểm
        ClientRewardLogResponse response2 = new ClientRewardLogResponse(); // Tạo DTO cho log 2
        response2.setRewardLogScore(50); // Gán điểm
        ClientRewardLogResponse response3 = new ClientRewardLogResponse(); // Tạo DTO cho log 3
        response3.setRewardLogScore(50); // Gán điểm
        when(clientRewardLogMapper.entityToResponse(anyList())).thenReturn(List.of(response3, response2, response1)); // Mock ánh xạ, sắp xếp ngược

        // Act
        ResponseEntity<ClientRewardResponse> result = clientRewardController.getReward(authentication); // Gọi phương thức getReward

        // Assert
        verify(authentication).getName(); // Kiểm tra gọi lấy username
        verify(rewardLogRepository).sumScoreByUsername("testuser"); // Kiểm tra gọi tính tổng điểm
        verify(rewardLogRepository).findByUserUsername("testuser"); // Kiểm tra gọi tìm RewardLog
        verify(clientRewardLogMapper).entityToResponse(anyList()); // Kiểm tra gọi ánh xạ
        verify(rewardLogRepository, never()).save(any()); // Kiểm tra không gọi save
        assertEquals(HttpStatus.OK, result.getStatusCode()); // Kiểm tra mã trạng thái HTTP là OK
        assertNotNull(result.getBody()); // Kiểm tra body không null
        assertEquals(150, result.getBody().getRewardTotalScore()); // Kiểm tra tổng điểm là 150
        assertEquals(3, result.getBody().getRewardLogs().size()); // Kiểm tra danh sách log có 3 phần tử
        assertEquals(response3, result.getBody().getRewardLogs().get(0)); // Kiểm tra log ID=3 đứng đầu
        assertEquals(response2, result.getBody().getRewardLogs().get(1)); // Kiểm tra log ID=2 đứng thứ hai
        assertEquals(response1, result.getBody().getRewardLogs().get(2)); // Kiểm tra log ID=1 đứng cuối
    }
}