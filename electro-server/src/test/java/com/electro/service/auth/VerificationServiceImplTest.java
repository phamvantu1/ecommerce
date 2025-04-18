package com.electro.service.auth;

import com.electro.constant.AppConstants;
import com.electro.dto.authentication.RegistrationRequest;
import com.electro.dto.authentication.ResetPasswordRequest;
import com.electro.dto.authentication.UserRequest;
import com.electro.dto.address.AddressRequest;
import com.electro.entity.authentication.Role;
import com.electro.entity.authentication.User;
import com.electro.entity.authentication.Verification;
import com.electro.entity.authentication.VerificationType;
import com.electro.entity.customer.Customer;
import com.electro.entity.customer.CustomerGroup;
import com.electro.entity.customer.CustomerResource;
import com.electro.entity.customer.CustomerStatus;
import com.electro.exception.ExpiredTokenException;
import com.electro.exception.VerificationException;
import com.electro.mapper.authentication.UserMapper;
import com.electro.repository.authentication.UserRepository;
import com.electro.repository.authentication.VerificationRepository;
import com.electro.repository.customer.CustomerRepository;
import com.electro.service.email.EmailSenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Lớp test cho VerificationServiceImpl
 * Mục tiêu: Kiểm tra các chức năng xác thực người dùng như đăng ký, xác nhận đăng ký, đổi email, quên mật khẩu, đặt lại mật khẩu
 */
@ExtendWith(MockitoExtension.class)
public class VerificationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationRepository verificationRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private VerificationServiceImpl verificationService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<Verification> verificationCaptor;

    @Captor
    private ArgumentCaptor<Customer> customerCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> emailAttributesCaptor;

    private UserRequest userRequest;
    private User user;
    private Verification verification;
    private RegistrationRequest registrationRequest;
    private ResetPasswordRequest resetPasswordRequest;

    /**
     * Thiết lập dữ liệu test trước mỗi test case
     */
    @BeforeEach
    void setUp() {
        // Tạo dữ liệu test cho UserRequest
        userRequest = new UserRequest();
        userRequest.setUsername("testuser");
        userRequest.setPassword("password123");
        userRequest.setFullname("Test User");
        userRequest.setEmail("test@example.com");
        userRequest.setPhone("0123456789");
        userRequest.setGender("M");
        
        AddressRequest addressRequest = new AddressRequest();
        addressRequest.setLine("123 Test Street");
        addressRequest.setProvinceId(1L);
        addressRequest.setDistrictId(2L);
        addressRequest.setWardId(3L);
        userRequest.setAddress(addressRequest);
        
        // Tạo dữ liệu test cho User
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encodedPassword");
        user.setFullname("Test User");
        user.setEmail("test@example.com");
        user.setPhone("0123456789");
        user.setGender("M");
        user.setStatus(2); // Trạng thái chưa xác thực
        
        // Tạo dữ liệu test cho Verification
        verification = new Verification();
        verification.setId(1L);
        verification.setUser(user);
        verification.setToken("123456");
        verification.setExpiredAt(Instant.now().plus(5, java.time.temporal.ChronoUnit.MINUTES));
        verification.setType(VerificationType.REGISTRATION);
        
        // Tạo dữ liệu test cho RegistrationRequest
        registrationRequest = new RegistrationRequest();
        registrationRequest.setUserId(1L);
        registrationRequest.setToken("123456");
        
        // Tạo dữ liệu test cho ResetPasswordRequest
        resetPasswordRequest = new ResetPasswordRequest();
        resetPasswordRequest.setEmail("test@example.com");
        resetPasswordRequest.setToken("resetToken123");
        resetPasswordRequest.setPassword("newPassword123");
    }

    /**
     * Test case: Tạo token xác thực thành công
     * Mục tiêu: Kiểm tra quy trình tạo token xác thực khi đăng ký tài khoản mới
     * - Kiểm tra tên đăng nhập và email chưa tồn tại
     * - Kiểm tra tạo người dùng với trạng thái chưa xác thực
     * - Kiểm tra tạo token xác thực
     * - Kiểm tra gửi email chứa token
     * Expected output: Trả về userId của người dùng mới tạo
     */
    @Test
    void generateTokenVerify_Success() {
        // Chuẩn bị
        when(userRepository.existsUserByUsername(anyString())).thenReturn(false);
        when(userRepository.existsUserByEmail(anyString())).thenReturn(false);
        when(userMapper.requestToEntity(any(UserRequest.class))).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(verificationRepository.save(any(Verification.class))).thenAnswer(invocation -> {
            Verification savedVerification = invocation.getArgument(0);
            return savedVerification;
        });
        doNothing().when(emailSenderService).sendVerificationToken(anyString(), any(Map.class));
        
        // Thực hiện
        Long userId = verificationService.generateTokenVerify(userRequest);
        
        // Kiểm tra
        assertEquals(1L, userId);
        
        // Xác minh
        verify(userRepository).existsUserByUsername("testuser");
        verify(userRepository).existsUserByEmail("test@example.com");
        verify(userMapper).requestToEntity(userRequest);
        verify(userRepository).save(userCaptor.capture());
        verify(verificationRepository).save(verificationCaptor.capture());
        verify(emailSenderService).sendVerificationToken(
                eq("test@example.com"), emailAttributesCaptor.capture());
        
        // Kiểm tra User được lưu
        User savedUser = userCaptor.getValue();
        assertEquals(2, savedUser.getStatus());
        assertEquals(1, savedUser.getRoles().size());
        
        // Kiểm tra Verification được lưu
        Verification savedVerification = verificationCaptor.getValue();
        assertEquals(user, savedVerification.getUser());
        assertEquals(VerificationType.REGISTRATION, savedVerification.getType());
        assertTrue(savedVerification.getExpiredAt().isAfter(Instant.now()));
        
        // Kiểm tra token được tạo ra
        String token = savedVerification.getToken();
        assertTrue(Pattern.matches("^\\d{4}$", token), "Token phải là 4 chữ số");
        
        // Kiểm tra email được gửi
        Map<String, Object> emailAttributes = emailAttributesCaptor.getValue();
        assertEquals(token, emailAttributes.get("token"), "Token trong email phải giống token được lưu");
        assertTrue(((String) emailAttributes.get("link")).contains("/signup?userId=1"));
    }

    /**
     * Test case: Tên đăng nhập đã tồn tại
     * Mục tiêu: Kiểm tra hệ thống từ chối đăng ký khi tên đăng nhập đã tồn tại
     * - Kiểm tra ném ra ngoại lệ VerificationException
     * - Kiểm tra thông báo lỗi chính xác
     * - Kiểm tra không có thao tác nào được thực hiện (không lưu user, không tạo token, không gửi email)
     * Expected output: Ném ra VerificationException với thông báo "Username is existing"
     */
    @Test
    void generateTokenVerify_UsernameExists_ThrowsException() {
        // Chuẩn bị
        when(userRepository.existsUserByUsername(anyString())).thenReturn(true);
        
        // Thực hiện & Kiểm tra
        VerificationException exception = assertThrows(VerificationException.class, 
            () -> verificationService.generateTokenVerify(userRequest));
        
        assertEquals("Username is existing", exception.getMessage());
        
        // Xác minh
        verify(userRepository).existsUserByUsername("testuser");
        verify(userRepository, never()).existsUserByEmail(anyString());
        verify(userMapper, never()).requestToEntity(any(UserRequest.class));
        verify(userRepository, never()).save(any(User.class));
        verify(verificationRepository, never()).save(any(Verification.class));
        verify(emailSenderService, never()).sendVerificationToken(anyString(), any(Map.class));
    }

    /**
     * Test case: Email đã tồn tại
     * Mục tiêu: Kiểm tra hệ thống từ chối đăng ký khi email đã tồn tại
     * - Kiểm tra ném ra ngoại lệ VerificationException
     * - Kiểm tra thông báo lỗi chính xác
     * - Kiểm tra không có thao tác nào được thực hiện (không lưu user, không tạo token, không gửi email)
     * Expected output: Ném ra VerificationException với thông báo "Email is existing"
     */
    @Test
    void generateTokenVerify_EmailExists_ThrowsException() {
        // Chuẩn bị
        when(userRepository.existsUserByUsername(anyString())).thenReturn(false);
        when(userRepository.existsUserByEmail(anyString())).thenReturn(true);
        
        // Thực hiện & Kiểm tra
        VerificationException exception = assertThrows(VerificationException.class, 
            () -> verificationService.generateTokenVerify(userRequest));
        
        assertEquals("Email is existing", exception.getMessage());
        
        // Xác minh
        verify(userRepository).existsUserByUsername("testuser");
        verify(userRepository).existsUserByEmail("test@example.com");
        verify(userMapper, never()).requestToEntity(any(UserRequest.class));
        verify(userRepository, never()).save(any(User.class));
        verify(verificationRepository, never()).save(any(Verification.class));
        verify(emailSenderService, never()).sendVerificationToken(anyString(), any(Map.class));
    }

    /**
     * Test case: Kiểm tra định dạng token
     * Mục tiêu: Kiểm tra token được tạo ra có đúng định dạng 4 chữ số
     * - Kiểm tra token là chuỗi 4 chữ số
     * - Kiểm tra token trong email attributes giống với token được lưu
     * Expected output: Token là chuỗi 4 chữ số và token trong email giống với token được lưu
     */
    @Test
    void generateTokenVerify_TokenFormat() {
        // Chuẩn bị
        when(userRepository.existsUserByUsername(anyString())).thenReturn(false);
        when(userRepository.existsUserByEmail(anyString())).thenReturn(false);
        when(userMapper.requestToEntity(any(UserRequest.class))).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(verificationRepository.save(any(Verification.class))).thenAnswer(invocation -> {
            Verification savedVerification = invocation.getArgument(0);
            return savedVerification;
        });
        doNothing().when(emailSenderService).sendVerificationToken(anyString(), any(Map.class));
        
        // Thực hiện
        verificationService.generateTokenVerify(userRequest);
        
        // Xác minh
        verify(verificationRepository).save(verificationCaptor.capture());
        verify(emailSenderService).sendVerificationToken(
                eq("test@example.com"), emailAttributesCaptor.capture());
        
        // Kiểm tra định dạng token
        Verification savedVerification = verificationCaptor.getValue();
        String token = savedVerification.getToken();
        
        // Kiểm tra token có đúng định dạng 4 chữ số
        assertTrue(Pattern.matches("^\\d{4}$", token), "Token phải là 4 chữ số");
        
        // Kiểm tra token trong email attributes
        Map<String, Object> emailAttributes = emailAttributesCaptor.getValue();
        assertEquals(token, emailAttributes.get("token"),
                "Token trong email phải giống token được lưu");
    }

    /**
     * Test case: Xác nhận đăng ký thành công
     * Mục tiêu: Kiểm tra quy trình xác nhận đăng ký thành công
     * - Kiểm tra tìm verification theo userId
     * - Kiểm tra cập nhật trạng thái người dùng thành đã xác thực
     * - Kiểm tra xóa verification sau khi xác nhận
     * - Kiểm tra tạo customer mới
     * Expected output: Không có lỗi, người dùng được cập nhật trạng thái đã xác thực và customer mới được tạo
     */
    @Test
    void confirmRegistration_Success() {
        // Chuẩn bị
        when(verificationRepository.findByUserId(anyLong())).thenReturn(Optional.of(verification));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(customerRepository.save(any(Customer.class))).thenReturn(new Customer());
        
        // Thực hiện
        verificationService.confirmRegistration(registrationRequest);
        
        // Kiểm tra & Xác minh
        verify(verificationRepository).findByUserId(1L);
        verify(userRepository).save(userCaptor.capture());
        verify(verificationRepository).delete(verification);
        verify(customerRepository).save(customerCaptor.capture());
        
        // Kiểm tra User được cập nhật
        User updatedUser = userCaptor.getValue();
        assertEquals(1, updatedUser.getStatus()); // Đã xác thực
        
        // Kiểm tra Customer được tạo
        Customer createdCustomer = customerCaptor.getValue();
        assertEquals(user, createdCustomer.getUser());
        assertEquals(1L, createdCustomer.getCustomerGroup().getId());
        assertEquals(1L, createdCustomer.getCustomerStatus().getId());
        assertEquals(1L, createdCustomer.getCustomerResource().getId());
    }

    /**
     * Test case: Người dùng không tồn tại khi xác nhận đăng ký
     * Mục tiêu: Kiểm tra hệ thống từ chối xác nhận đăng ký khi người dùng không tồn tại
     * - Kiểm tra ném ra ngoại lệ VerificationException
     * - Kiểm tra thông báo lỗi chính xác
     * - Kiểm tra không có thao tác nào được thực hiện (không cập nhật user, không xóa verification, không tạo customer)
     * Expected output: Ném ra VerificationException với thông báo "User does not exist"
     */
    @Test
    void confirmRegistration_UserNotFound_ThrowsException() {
        // Chuẩn bị
        when(verificationRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        
        // Thực hiện & Kiểm tra
        VerificationException exception = assertThrows(VerificationException.class, 
            () -> verificationService.confirmRegistration(registrationRequest));
        
        assertEquals("User does not exist", exception.getMessage());
        
        // Xác minh
        verify(verificationRepository).findByUserId(1L);
        verify(userRepository, never()).save(any(User.class));
        verify(verificationRepository, never()).delete(any(Verification.class));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    /**
     * Test case: Token không hợp lệ khi xác nhận đăng ký
     * Mục tiêu: Kiểm tra hệ thống từ chối xác nhận đăng ký khi token không hợp lệ
     * - Kiểm tra ném ra ngoại lệ VerificationException
     * - Kiểm tra thông báo lỗi chính xác
     * - Kiểm tra không có thao tác nào được thực hiện (không cập nhật user, không xóa verification, không tạo customer)
     * Expected output: Ném ra VerificationException với thông báo "Invalid token"
     */
    @Test
    void confirmRegistration_InvalidToken_ThrowsException() {
        // Chuẩn bị
        registrationRequest.setToken("invalid_token");
        when(verificationRepository.findByUserId(anyLong())).thenReturn(Optional.of(verification));
        
        // Thực hiện & Kiểm tra
        VerificationException exception = assertThrows(VerificationException.class, 
            () -> verificationService.confirmRegistration(registrationRequest));
        
        assertEquals("Invalid token", exception.getMessage());
        
        // Xác minh
        verify(verificationRepository).findByUserId(1L);
        verify(userRepository, never()).save(any(User.class));
        verify(verificationRepository, never()).delete(any(Verification.class));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    /**
     * Test case: Token hết hạn khi xác nhận đăng ký
     * Mục tiêu: Kiểm tra hệ thống xử lý khi token hết hạn
     * - Kiểm tra ném ra ngoại lệ ExpiredTokenException
     * - Kiểm tra thông báo lỗi chính xác
     * - Kiểm tra tạo token mới
     * - Kiểm tra gửi email chứa token mới
     * Expected output: Ném ra ExpiredTokenException với thông báo "Token is expired, please check your email to get new token!"
     */
    @Test
    void confirmRegistration_ExpiredToken_ThrowsException() {
        // Chuẩn bị
        verification.setExpiredAt(Instant.now().minus(1,
                java.time.temporal.ChronoUnit.MINUTES));
        when(verificationRepository.findByUserId(anyLong())).thenReturn(Optional.of(verification));
        when(verificationRepository.save(any(Verification.class))).thenReturn(verification);
        doNothing().when(emailSenderService).sendVerificationToken(anyString(), any(Map.class));
        
        // Thực hiện & Kiểm tra
        ExpiredTokenException exception = assertThrows(ExpiredTokenException.class, 
            () -> verificationService.confirmRegistration(registrationRequest));
        
        assertEquals("Token is expired, please check your email to get new token!",
                exception.getMessage());
        
        // Xác minh
        verify(verificationRepository).findByUserId(1L);
        verify(verificationRepository).save(verificationCaptor.capture());
        verify(emailSenderService).sendVerificationToken(eq("test@example.com"),
                emailAttributesCaptor.capture());
        
        // Kiểm tra token mới được tạo
        Verification updatedVerification = verificationCaptor.getValue();
        assertNotEquals("123456", updatedVerification.getToken());
        assertTrue(updatedVerification.getExpiredAt().isAfter(Instant.now()));
        
        // Kiểm tra email được gửi
        Map<String, Object> emailAttributes = emailAttributesCaptor.getValue();
        assertNotEquals("123456", emailAttributes.get("token"));
        assertTrue(((String) emailAttributes.get("link")).contains("/signup?userId=1"));
    }
    
    /**
     * Test case: Gửi lại token đăng ký thành công
     * Mục tiêu: Kiểm tra quy trình gửi lại token đăng ký
     * - Kiểm tra tìm verification theo userId
     * - Kiểm tra tạo token mới
     * - Kiểm tra gửi email chứa token mới
     * Expected output: Không có lỗi, token mới được tạo và email được gửi
     */
    @Test
    void resendRegistrationToken_Success() {
        // Chuẩn bị
        when(verificationRepository.findByUserId(anyLong())).thenReturn(Optional.of(verification));
        when(verificationRepository.save(any(Verification.class))).thenAnswer(invocation -> {
            Verification savedVerification = invocation.getArgument(0);
            return savedVerification;
        });
        doNothing().when(emailSenderService).sendVerificationToken(anyString(), any(Map.class));
        
        // Thực hiện
        verificationService.resendRegistrationToken(1L);
        
        // Xác minh
        verify(verificationRepository).findByUserId(1L);
        verify(verificationRepository).save(verificationCaptor.capture());
        verify(emailSenderService).sendVerificationToken(eq("test@example.com"),
                emailAttributesCaptor.capture());
        
        // Kiểm tra token mới được tạo
        Verification updatedVerification = verificationCaptor.getValue();
        assertNotEquals("123456", updatedVerification.getToken());
        assertTrue(updatedVerification.getExpiredAt().isAfter(Instant.now()));
        
        // Kiểm tra định dạng token
        String token = updatedVerification.getToken();
        assertTrue(Pattern.matches("^\\d{4}$", token), "Token phải là 4 chữ số");
        
        // Kiểm tra email được gửi
        Map<String, Object> emailAttributes = emailAttributesCaptor.getValue();
        assertEquals(token, emailAttributes.get("token"),
                "Token trong email phải giống token được lưu");
        assertTrue(((String) emailAttributes.get("link")).contains("/signup?userId=1"));
    }
    
    /**
     * Test case: Người dùng không tồn tại khi gửi lại token đăng ký
     * Mục tiêu: Kiểm tra hệ thống từ chối gửi lại token khi người dùng không tồn tại
     * - Kiểm tra ném ra ngoại lệ VerificationException
     * - Kiểm tra thông báo lỗi chính xác
     * - Kiểm tra không có thao tác nào được thực hiện (không lưu verification, không gửi email)
     * Expected output: Ném ra VerificationException với thông báo "User ID is invalid. Please try again!"
     */
    @Test
    void resendRegistrationToken_UserNotFound_ThrowsException() {
        // Chuẩn bị
        when(verificationRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        
        // Thực hiện & Kiểm tra
        VerificationException exception = assertThrows(VerificationException.class, 
            () -> verificationService.resendRegistrationToken(1L));
        
        assertEquals("User ID is invalid. Please try again!", exception.getMessage());
        
        // Xác minh
        verify(verificationRepository).findByUserId(1L);
        verify(verificationRepository, never()).save(any(Verification.class));
        verify(emailSenderService, never()).sendVerificationToken(anyString(), any(Map.class));
    }
    
    /**
     * Test case: Đổi email đăng ký thành công
     * Mục tiêu: Kiểm tra quy trình đổi email đăng ký
     * - Kiểm tra tìm verification theo userId
     * - Kiểm tra email mới chưa tồn tại
     * - Kiểm tra cập nhật email mới cho người dùng
     * - Kiểm tra tạo token mới
     * - Kiểm tra gửi email chứa token mới đến địa chỉ email mới
     * Expected output: Không có lỗi, email mới được cập nhật, token mới được tạo và email được gửi
     */
    @Test
    void changeRegistrationEmail_Success() {
        // Chuẩn bị
        String newEmail = "newemail@example.com";
        when(verificationRepository.findByUserId(anyLong())).thenReturn(Optional.of(verification));
        when(userRepository.existsUserByEmail(newEmail)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(verificationRepository.save(any(Verification.class))).thenAnswer(invocation -> {
            Verification savedVerification = invocation.getArgument(0);
            return savedVerification;
        });
        doNothing().when(emailSenderService).sendVerificationToken(anyString(), any(Map.class));
        
        // Thực hiện
        verificationService.changeRegistrationEmail(1L, newEmail);
        
        // Xác minh
        verify(verificationRepository).findByUserId(1L);
        verify(userRepository).existsUserByEmail(newEmail);
        verify(userRepository).save(userCaptor.capture());
        verify(verificationRepository).save(verificationCaptor.capture());
        verify(emailSenderService).sendVerificationToken(eq(newEmail),
                emailAttributesCaptor.capture());
        
        // Kiểm tra User được cập nhật
        User updatedUser = userCaptor.getValue();
        assertEquals(newEmail, updatedUser.getEmail());
        
        // Kiểm tra Verification được cập nhật
        Verification updatedVerification = verificationCaptor.getValue();
        assertNotEquals("123456", updatedVerification.getToken());
        assertTrue(updatedVerification.getExpiredAt().isAfter(Instant.now()));
        
        // Kiểm tra định dạng token
        String token = updatedVerification.getToken();
        assertTrue(Pattern.matches("^\\d{4}$", token), "Token phải là 4 chữ số");
        
        // Kiểm tra email được gửi
        Map<String, Object> emailAttributes = emailAttributesCaptor.getValue();
        assertEquals(token, emailAttributes.get("token"),
                "Token trong email phải giống token được lưu");
        assertTrue(((String) emailAttributes.get("link")).contains("/signup?userId=1"));
    }
    
    /**
     * Test case: Người dùng không tồn tại khi đổi email đăng ký
     * Mục tiêu: Kiểm tra hệ thống từ chối đổi email khi người dùng không tồn tại
     * - Kiểm tra ném ra ngoại lệ VerificationException
     * - Kiểm tra thông báo lỗi chính xác
     * - Kiểm tra không có thao tác nào được thực hiện (không kiểm tra email, không cập nhật user, không lưu verification, không gửi email)
     * Expected output: Ném ra VerificationException với thông báo "User does not exist"
     */
    @Test
    void changeRegistrationEmail_UserNotFound_ThrowsException() {
        // Chuẩn bị
        String newEmail = "newemail@example.com";
        when(verificationRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        
        // Thực hiện & Kiểm tra
        VerificationException exception = assertThrows(VerificationException.class, 
            () -> verificationService.changeRegistrationEmail(1L, newEmail));
        
        assertEquals("User does not exist", exception.getMessage());
        
        // Xác minh
        verify(verificationRepository).findByUserId(1L);
        verify(userRepository, never()).existsUserByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(verificationRepository, never()).save(any(Verification.class));
        verify(emailSenderService, never()).sendVerificationToken(anyString(), any(Map.class));
    }
    
    /**
     * Test case: Email mới đã tồn tại khi đổi email đăng ký
     * Mục tiêu: Kiểm tra hệ thống từ chối đổi email khi email mới đã tồn tại
     * - Kiểm tra ném ra ngoại lệ VerificationException
     * - Kiểm tra thông báo lỗi chính xác
     * - Kiểm tra không có thao tác nào được thực hiện (không cập nhật user, không lưu verification, không gửi email)
     * Expected output: Ném ra VerificationException với thông báo "Email is existing"
     */
    @Test
    void changeRegistrationEmail_EmailExists_ThrowsException() {
        // Chuẩn bị
        String newEmail = "existing@example.com";
        when(verificationRepository.findByUserId(anyLong())).thenReturn(Optional.of(verification));
        when(userRepository.existsUserByEmail(newEmail)).thenReturn(true);
        
        // Thực hiện & Kiểm tra
        VerificationException exception = assertThrows(VerificationException.class, 
            () -> verificationService.changeRegistrationEmail(1L, newEmail));
        
        assertEquals("Email is existing", exception.getMessage());
        
        // Xác minh
        verify(verificationRepository).findByUserId(1L);
        verify(userRepository).existsUserByEmail(newEmail);
        verify(userRepository, never()).save(any(User.class));
        verify(verificationRepository, never()).save(any(Verification.class));
        verify(emailSenderService, never()).sendVerificationToken(anyString(), any(Map.class));
    }
    
    /**
     * Test case: Định dạng email không hợp lệ khi đổi email đăng ký
     * Mục tiêu: Kiểm tra hệ thống từ chối đổi email khi định dạng email không hợp lệ
     * - Kiểm tra ném ra ngoại lệ VerificationException
     * - Kiểm tra thông báo lỗi chính xác
     * - Kiểm tra không có thao tác nào được thực hiện (không kiểm tra email, không cập nhật user, không lưu verification, không gửi email)
     * Expected output: Ném ra VerificationException với thông báo "Invalid email format"
     */
    @Test
    void changeRegistrationEmail_InvalidEmailFormat_ThrowsException() {
        // Chuẩn bị
        String invalidEmail = "invalid-email";
        when(verificationRepository.findByUserId(anyLong())).thenReturn(Optional.of(verification));
        
        // Thực hiện & Kiểm tra
        VerificationException exception = assertThrows(VerificationException.class, 
            () -> verificationService.changeRegistrationEmail(1L, invalidEmail));
        
        assertEquals("Invalid email format", exception.getMessage());
        
        // Xác minh
        verify(verificationRepository).findByUserId(1L);
        verify(userRepository, never()).existsUserByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(verificationRepository, never()).save(any(Verification.class));
        verify(emailSenderService, never()).sendVerificationToken(anyString(), any(Map.class));
    }
    
    /**
     * Test case: Quên mật khẩu thành công
     * Mục tiêu: Kiểm tra quy trình quên mật khẩu
     * - Kiểm tra tìm người dùng theo email
     * - Kiểm tra người dùng đã được xác thực
     * - Kiểm tra tạo token đặt lại mật khẩu
     * - Kiểm tra gửi email chứa link đặt lại mật khẩu
     * Expected output: Không có lỗi, token đặt lại mật khẩu được tạo và email được gửi
     */
    @Test
    void forgetPassword_Success() {
        // Chuẩn bị
        String email = "test@example.com";
        user.setStatus(1); // Người dùng đã xác thực
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        doNothing().when(emailSenderService).sendForgetPasswordToken(anyString(), any(Map.class));
        
        // Thực hiện
        verificationService.forgetPassword(email);
        
        // Xác minh
        verify(userRepository).findByEmail(email);
        verify(userRepository).save(userCaptor.capture());
        verify(emailSenderService).sendForgetPasswordToken(eq(email), emailAttributesCaptor.capture());
        
        // Kiểm tra token được tạo
        User updatedUser = userCaptor.getValue();
        assertNotNull(updatedUser.getResetPasswordToken());
        assertTrue(updatedUser.getResetPasswordToken().length() > 0);
        
        // Kiểm tra email được gửi
        Map<String, Object> emailAttributes = emailAttributesCaptor.getValue();
        assertTrue(((String) emailAttributes.get("link")).contains("/change-password"));
        assertTrue(((String) emailAttributes.get("link")).contains("token=" + updatedUser.getResetPasswordToken()));
        assertTrue(((String) emailAttributes.get("link")).contains("email=" + email));
    }
    
    /**
     * Test case: Email không tồn tại khi quên mật khẩu
     * Mục tiêu: Kiểm tra hệ thống từ chối quên mật khẩu khi email không tồn tại
     * - Kiểm tra ném ra ngoại lệ RuntimeException
     * - Kiểm tra thông báo lỗi chính xác
     * - Kiểm tra không có thao tác nào được thực hiện (không lưu user, không gửi email)
     * Expected output: Ném ra RuntimeException với thông báo "Email doesn't exist"
     */
    @Test
    void forgetPassword_EmailNotFound_ThrowsException() {
        // Chuẩn bị
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        
        // Thực hiện & Kiểm tra
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> verificationService.forgetPassword(email));
        
        assertEquals("Email doesn't exist", exception.getMessage());
        
        // Xác minh
        verify(userRepository).findByEmail(email);
        verify(userRepository, never()).save(any(User.class));
        verify(emailSenderService, never()).sendForgetPasswordToken(anyString(), any(Map.class));
    }
    
    /**
     * Test case: Tài khoản chưa kích hoạt khi quên mật khẩu
     * Mục tiêu: Kiểm tra hệ thống từ chối quên mật khẩu khi tài khoản chưa kích hoạt
     * - Kiểm tra ném ra ngoại lệ VerificationException
     * - Kiểm tra thông báo lỗi chính xác
     * - Kiểm tra không có thao tác nào được thực hiện (không lưu user, không gửi email)
     * Expected output: Ném ra VerificationException với thông báo "Account is not activated"
     */
    @Test
    void forgetPassword_AccountNotActivated_ThrowsException() {
        // Chuẩn bị
        String email = "test@example.com";
        user.setStatus(2); // Người dùng chưa xác thực
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        
        // Thực hiện & Kiểm tra
        VerificationException exception = assertThrows(VerificationException.class, 
            () -> verificationService.forgetPassword(email));
        
        assertEquals("Account is not activated", exception.getMessage());
        
        // Xác minh
        verify(userRepository).findByEmail(email);
        verify(userRepository, never()).save(any(User.class));
        verify(emailSenderService, never()).sendForgetPasswordToken(anyString(), any(Map.class));
    }
    
    /**
     * Test case: Đặt lại mật khẩu thành công
     * Mục tiêu: Kiểm tra quy trình đặt lại mật khẩu
     * - Kiểm tra tìm người dùng theo email và token
     * - Kiểm tra mã hóa mật khẩu mới
     * - Kiểm tra cập nhật mật khẩu mới cho người dùng
     * Expected output: Không có lỗi, mật khẩu mới được mã hóa và cập nhật cho người dùng
     */
    @Test
    void resetPassword_Success() {
        // Chuẩn bị
        String email = "test@example.com";
        String token = "resetToken123";
        String newPassword = "newPassword123";
        resetPasswordRequest.setEmail(email);
        resetPasswordRequest.setToken(token);
        resetPasswordRequest.setPassword(newPassword);
        
        user.setResetPasswordToken(token);
        when(userRepository.findByEmailAndResetPasswordToken(email, token)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        // Thực hiện
        verificationService.resetPassword(resetPasswordRequest);
        
        // Xác minh
        verify(userRepository).findByEmailAndResetPasswordToken(email, token);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(userCaptor.capture());
        
        // Kiểm tra mật khẩu được cập nhật
        User updatedUser = userCaptor.getValue();
        assertEquals("encodedNewPassword", updatedUser.getPassword());
    }
    
    /**
     * Test case: Email hoặc token không hợp lệ khi đặt lại mật khẩu
     * Mục tiêu: Kiểm tra hệ thống từ chối đặt lại mật khẩu khi email hoặc token không hợp lệ
     * - Kiểm tra ném ra ngoại lệ RuntimeException
     * - Kiểm tra thông báo lỗi chính xác
     * - Kiểm tra không có thao tác nào được thực hiện (không mã hóa mật khẩu, không lưu user)
     * Expected output: Ném ra RuntimeException với thông báo "Email and/or token are invalid"
     */
    @Test
    void resetPassword_InvalidEmailOrToken_ThrowsException() {
        // Chuẩn bị
        String email = "test@example.com";
        String token = "invalidToken";
        resetPasswordRequest.setEmail(email);
        resetPasswordRequest.setToken(token);
        
        when(userRepository.findByEmailAndResetPasswordToken(email, token)).thenReturn(Optional.empty());
        
        // Thực hiện & Kiểm tra
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> verificationService.resetPassword(resetPasswordRequest));
        
        assertEquals("Email and/or token are invalid", exception.getMessage());
        
        // Xác minh
        verify(userRepository).findByEmailAndResetPasswordToken(email, token);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
} 