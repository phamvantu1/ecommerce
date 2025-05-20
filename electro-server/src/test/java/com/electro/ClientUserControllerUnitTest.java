package com.electro;

import com.electro.controller.client.ClientUserController;
import com.electro.dto.address.AddressRequest;
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

    // test case 1: Lấy thông tin người dùng thành công
    @Test
    void testGetUserInfo_WhenUserExists_ShouldReturnUserInfo() {
        // Giả lập đối tượng Authentication trả về tên người dùng là "existingUser"
        when(authentication.getName()).thenReturn("existingUser");

        //Tạo đối tượng User mô phỏng người dùng tồn tại trong hệ thống
        User user = new User();
        user.setUsername("existingUser");

        // Tạo đối tượng UserResponse là kết quả mong muốn sau khi ánh xạ từ User
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername("existingUser");

        // Giả lập hành vi của userRepository: trả về Optional chứa user nếu tìm thấy theo username
        when(userRepository.findByUsername("existingUser")).thenReturn(Optional.of(user));

        // Giả lập hành vi của userMapper: ánh xạ User entity sang UserResponse DTO
        when(userMapper.entityToResponse(user)).thenReturn(userResponse);

        // Gọi phương thức cần kiểm thử trong controller, truyền vào authentication giả lập
        ResponseEntity<UserResponse> response = clientUserController.getUserInfo(authentication);

        // Kiểm tra kết quả trả về: HTTP status phải là 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Đảm bảo rằng body của response không null
        assertNotNull(response.getBody());

        // Đảm bảo username trong response đúng như mong đợi
        assertEquals("existingUser", response.getBody().getUsername());
    }

    // test case 2: Lấy thông tin người dùng thất bại (người dùng không tồn tại)
    @Test
    void testGetUserInfo_WhenUserDoesNotExist_ShouldThrowUsernameNotFoundException() {
        // Arrange: Giả lập authentication trả về tên người dùng không tồn tại
        when(authentication.getName()).thenReturn("nonExistentUser");

        // Giả lập repository trả về Optional.empty() khi tìm user theo username
        when(userRepository.findByUsername("nonExistentUser")).thenReturn(Optional.empty());

        // Act & Assert: Kiểm tra rằng hàm sẽ ném UsernameNotFoundException nếu không tìm thấy user
        assertThrows(UsernameNotFoundException.class, () -> {
            clientUserController.getUserInfo(authentication);
        });

        // Verify: Đảm bảo rằng phương thức tìm kiếm đã được gọi đúng với username
        verify(userRepository).findByUsername("nonExistentUser");
    }



    // test case 3: Cập nhật cài đặt cá nhân thành công khi người dùng tồn tại
    @Test
    void testUpdatePersonalSetting_ShouldUpdateSuccessfully_WhenUserExists() {
        // 🧪 Giả lập Authentication trả về username của người dùng hiện tại
        when(authentication.getName()).thenReturn("existingUser");

        // 🧪 Tạo dữ liệu đầu vào giả lập từ client (yêu cầu cập nhật thông tin người dùng)
        ClientPersonalSettingUserRequest userRequest = new ClientPersonalSettingUserRequest();
        userRequest.setUsername("existingUser");
        userRequest.setFullname("Updated Fullname");

        // 🧪 Giả lập user hiện tại được lấy từ cơ sở dữ liệu
        User existingUser = new User();
        existingUser.setUsername("existingUser");

        // 🧪 Tạo UserResponse giả lập là kết quả mong muốn sau khi cập nhật
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername("existingUser");
        userResponse.setFullname("Updated Fullname");

        // 🔁 Giả lập các hành vi repository và mapper
        when(userRepository.findByUsername("existingUser")).thenReturn(Optional.of(existingUser));
        when(userMapper.partialUpdate(existingUser, userRequest)).thenReturn(existingUser);
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.entityToResponse(existingUser)).thenReturn(userResponse);

        // 🎯 Gọi phương thức updatePersonalSetting trong controller
        ResponseEntity<UserResponse> response = clientUserController.updatePersonalSetting(authentication, userRequest);

        // ✅ Kiểm tra HTTP status là 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // ✅ Kiểm tra response body không null
        assertNotNull(response.getBody());

        // ✅ Kiểm tra thông tin trả về đúng như kỳ vọng
        assertEquals("existingUser", response.getBody().getUsername());
        assertEquals("Updated Fullname", response.getBody().getFullname());
    }

    // test case 4: Cập nhật cài đặt cá nhân thất bại khi người dùng không tồn tại
    @Test
    void testUpdatePersonalSetting_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
        // Arrange: Giả lập authentication trả về username không tồn tại
        when(authentication.getName()).thenReturn("nonExistentUser");

        // Giả lập repository trả về Optional rỗng khi tìm user theo username
        when(userRepository.findByUsername("nonExistentUser")).thenReturn(Optional.empty());

        // Act & Assert: Kiểm tra rằng phương thức sẽ ném ra UsernameNotFoundException
        assertThrows(UsernameNotFoundException.class, () -> {
            clientUserController.updatePersonalSetting(authentication, new ClientPersonalSettingUserRequest());
        });

        // Verify: Đảm bảo rằng repository đã được gọi với đúng username
        verify(userRepository).findByUsername("nonExistentUser");
    }

    // test case 5: Cập nhật cài đặt cá nhân thất bại khi username là null hoặc rỗng
    @Test
    void testUpdatePersonalSetting_ShouldThrowException_WhenUsernameIsNullOrEmpty() {
        // 🧪 Giả lập Authentication trả về một username bất kỳ
        when(authentication.getName()).thenReturn("testUsername");

        // --- Trường hợp 1: Username là null ---
        ClientPersonalSettingUserRequest userRequestWithNullUsername = new ClientPersonalSettingUserRequest();
        userRequestWithNullUsername.setUsername(null);
        userRequestWithNullUsername.setFullname("Test Fullname");

        // 🎯 Gọi phương thức và kiểm tra exception được ném ra
        Exception exception1 = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updatePersonalSetting(authentication, userRequestWithNullUsername);
        });
        // ✅ Kiểm tra thông báo lỗi
        assertEquals("Username cannot be null or empty", exception1.getMessage());

        // --- Trường hợp 2: Username là chuỗi rỗng ---
        ClientPersonalSettingUserRequest userRequestWithEmptyUsername = new ClientPersonalSettingUserRequest();
        userRequestWithEmptyUsername.setUsername("");
        userRequestWithEmptyUsername.setFullname("Test Fullname");

        // 🎯 Gọi phương thức và kiểm tra exception được ném ra
        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updatePersonalSetting(authentication, userRequestWithEmptyUsername);
        });
        // ✅ Kiểm tra thông báo lỗi
        assertEquals("Username cannot be null or empty", exception2.getMessage());
    }

    // test case 6: Cập nhật cài đặt cá nhân thành công khi fullname hợp lệ
    @Test
    void testUpdatePersonalSetting_ShouldUpdateSuccessfully_WhenFullnameIsValid() {
        // 🧪 Giả lập Authentication trả về username hợp lệ
        when(authentication.getName()).thenReturn("validUsername");

        // 🧪 Tạo dữ liệu đầu vào với fullname hợp lệ
        ClientPersonalSettingUserRequest userRequest = new ClientPersonalSettingUserRequest();
        userRequest.setUsername("validUsername");
        userRequest.setFullname("Valid Fullname");

        // 🧪 Giả lập user hiện tại trong database
        User existingUser = new User();
        existingUser.setUsername("validUsername");

        // 🧪 Chuẩn bị UserResponse trả về sau khi cập nhật
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername("validUsername");
        userResponse.setFullname("Valid Fullname");

        // 🔁 Giả lập các tương tác với repository và mapper
        when(userRepository.findByUsername("validUsername")).thenReturn(Optional.of(existingUser));
        when(userMapper.partialUpdate(existingUser, userRequest)).thenReturn(existingUser);
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.entityToResponse(existingUser)).thenReturn(userResponse);

        // 🎯 Gọi hàm cập nhật cài đặt cá nhân
        ResponseEntity<UserResponse> response = clientUserController.updatePersonalSetting(authentication, userRequest);

        // ✅ Kiểm tra trạng thái HTTP trả về là 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // ✅ Kiểm tra response body không null và dữ liệu chính xác
        assertNotNull(response.getBody());
        assertEquals("validUsername", response.getBody().getUsername());
        assertEquals("Valid Fullname", response.getBody().getFullname());
    }

    // test case 7: Cập nhật cài đặt cá nhân thất bại khi fullname là null
    @Test
    void testUpdatePersonalSetting_ShouldThrowException_WhenFullnameIsNull() {
        // 🧪 Giả lập Authentication trả về username hợp lệ
        when(authentication.getName()).thenReturn("testUsername");

        // 🧪 Giả lập UserRepository trả về user tồn tại
        User mockedUser = new User();
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(mockedUser));

        // 🧪 Tạo request với fullname là null
        ClientPersonalSettingUserRequest requestWithNullFullname = new ClientPersonalSettingUserRequest();
        requestWithNullFullname.setUsername("testUsername");
        requestWithNullFullname.setFullname(null);

        // 🎯 Gọi hàm update và kiểm tra ném ra IllegalArgumentException
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updatePersonalSetting(authentication, requestWithNullFullname);
        });

        // ✅ Kiểm tra thông báo lỗi đúng như mong đợi
        assertEquals("Fullname cannot be null or empty", exception.getMessage());
    }

    // test case 8: Cập nhật cài đặt cá nhân thất bại khi fullname chứa ký tự đặc biệt
    @Test
    void testUpdatePersonalSetting_ShouldThrowException_WhenFullnameContainsSpecialCharacters() {
        // 🧪 Giả lập Authentication trả về username hợp lệ
        when(authentication.getName()).thenReturn("testUsername");

        // 🧪 Tạo request với fullname chứa ký tự đặc biệt không hợp lệ
        ClientPersonalSettingUserRequest requestWithInvalidFullname = new ClientPersonalSettingUserRequest();
        requestWithInvalidFullname.setUsername("testUsername");
        requestWithInvalidFullname.setFullname("Invalid@Name!");

        // 🎯 Gọi hàm update và kiểm tra ném ra IllegalArgumentException
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updatePersonalSetting(authentication, requestWithInvalidFullname);
        });

        // ✅ Kiểm tra thông báo lỗi đúng như mong đợi
        assertEquals("Fullname contains invalid characters", exception.getMessage());
    }

    // test case 9: Cập nhật cài đặt cá nhân thành công khi gender hợp lệ
    @Test
    void testUpdatePersonalSetting_ShouldUpdateSuccessfully_WhenGenderIsValid() {
        // 🧪 Giả lập Authentication trả về username hợp lệ
        when(authentication.getName()).thenReturn("testUsername");

        // 🧪 Tạo dữ liệu đầu vào với gender hợp lệ
        ClientPersonalSettingUserRequest userRequest = new ClientPersonalSettingUserRequest();
        userRequest.setUsername("testUsername");
        userRequest.setGender("Male");

        // 🧪 Giả lập user hiện tại trong database
        User existingUser = new User();
        existingUser.setUsername("testUsername");

        // 🧪 Chuẩn bị UserResponse trả về sau khi cập nhật
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername("testUsername");
        userResponse.setGender("Male");

        // 🔁 Giả lập các tương tác với repository và mapper
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(existingUser));
        when(userMapper.partialUpdate(existingUser, userRequest)).thenReturn(existingUser);
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.entityToResponse(existingUser)).thenReturn(userResponse);

        // 🎯 Gọi hàm cập nhật cài đặt cá nhân
        ResponseEntity<UserResponse> response = clientUserController.updatePersonalSetting(authentication, userRequest);

        // ✅ Kiểm tra trạng thái HTTP trả về là 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // ✅ Kiểm tra response body không null và dữ liệu chính xác
        assertNotNull(response.getBody());
        assertEquals("testUsername", response.getBody().getUsername());
        assertEquals("Male", response.getBody().getGender());
    }

    // test case 10: Cập nhật cài đặt cá nhân thất bại khi gender là null hoặc chuỗi rỗng
    @Test
    void testUpdatePersonalSetting_ShouldThrowException_WhenGenderIsNullOrEmpty() {
        // 🧪 Giả lập Authentication trả về username hợp lệ
        when(authentication.getName()).thenReturn("testUsername");

        // 🧪 Trường hợp 1: gender là null
        ClientPersonalSettingUserRequest requestWithNullGender = new ClientPersonalSettingUserRequest();
        requestWithNullGender.setUsername("testUsername");
        requestWithNullGender.setGender(null);

        // 🎯 Gọi hàm update và kiểm tra ném IllegalArgumentException với thông báo chính xác
        Exception exception1 = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updatePersonalSetting(authentication, requestWithNullGender);
        });
        assertEquals("Gender cannot be null or empty", exception1.getMessage());

        // 🧪 Trường hợp 2: gender là chuỗi rỗng
        ClientPersonalSettingUserRequest requestWithEmptyGender = new ClientPersonalSettingUserRequest();
        requestWithEmptyGender.setUsername("testUsername");
        requestWithEmptyGender.setGender("");

        // 🎯 Gọi hàm update và kiểm tra ném IllegalArgumentException với thông báo chính xác
        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updatePersonalSetting(authentication, requestWithEmptyGender);
        });
        assertEquals("Gender cannot be null or empty", exception2.getMessage());
    }

    // test case 11: Cập nhật cài đặt cá nhân thất bại khi giá trị gender không hợp lệ
    @Test
    void testUpdatePersonalSetting_ShouldThrowException_WhenGenderIsInvalid() {
        // 🧪 Giả lập Authentication trả về username hợp lệ
        when(authentication.getName()).thenReturn("testUsername");

        // 🧪 Trường hợp gender có giá trị không hợp lệ (không phải "Male", "Female", ...)
        ClientPersonalSettingUserRequest requestWithInvalidGender = new ClientPersonalSettingUserRequest();
        requestWithInvalidGender.setUsername("testUsername");
        requestWithInvalidGender.setGender("InvalidGender");

        // 🎯 Gọi hàm update và kiểm tra ném IllegalArgumentException với thông báo chính xác
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updatePersonalSetting(authentication, requestWithInvalidGender);
        });
        assertEquals("Gender value is invalid", exception.getMessage());
    }

    // test case 12: Cập nhật cài đặt cá nhân thành công khi địa chỉ hợp lệ
    @Test
    void testUpdatePersonalSetting_ShouldUpdateSuccessfully_WhenAddressIsValid() {
        // 🧪 Giả lập Authentication trả về username "testUsername"
        when(authentication.getName()).thenReturn("testUsername");

        // 🧪 Giả lập dữ liệu đầu vào AddressRequest với thông tin địa chỉ hợp lệ
        AddressRequest addressRequest = new AddressRequest();
        addressRequest.setLine("123 Main Street");
        addressRequest.setProvinceId(1L);
        addressRequest.setDistrictId(2L);
        addressRequest.setWardId(3L);

        // 🧪 Tạo request cập nhật cài đặt cá nhân, gán địa chỉ vào
        ClientPersonalSettingUserRequest userRequest = new ClientPersonalSettingUserRequest();
        userRequest.setUsername("testUsername");
        userRequest.setAddress(addressRequest);

        // 🧪 Giả lập User tồn tại trong repository
        User existingUser = new User();
        existingUser.setUsername("testUsername");

        // 🧪 Giả lập phản hồi trả về sau khi cập nhật thành công
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername("testUsername");

        // 🧪 Giả lập hành vi của các phương thức liên quan
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(existingUser));
        when(userMapper.partialUpdate(existingUser, userRequest)).thenReturn(existingUser);
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.entityToResponse(existingUser)).thenReturn(userResponse);

        // 🎯 Gọi phương thức updatePersonalSetting và kiểm tra kết quả trả về
        ResponseEntity<UserResponse> response = clientUserController.updatePersonalSetting(authentication, userRequest);

        // ✅ Kiểm tra mã HTTP trả về là 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // ✅ Kiểm tra body trả về không null
        assertNotNull(response.getBody());
        // ✅ Kiểm tra username trả về đúng với username yêu cầu cập nhật
        assertEquals("testUsername", response.getBody().getUsername());
    }

    // test case 13: Cập nhật cài đặt cá nhân thất bại khi địa chỉ là null
    @Test
    void testUpdatePersonalSetting_ShouldThrowException_WhenAddressIsNull() {
        // 🧪 Giả lập Authentication trả về username "testUsername"
        when(authentication.getName()).thenReturn("testUsername");

        // 🧪 Tạo request với địa chỉ là null (không hợp lệ)
        ClientPersonalSettingUserRequest userRequest = new ClientPersonalSettingUserRequest();
        userRequest.setUsername("testUsername");
        userRequest.setAddress(null);

        // 🎯 Gọi phương thức updatePersonalSetting và kiểm tra việc ném exception IllegalArgumentException với thông báo đúng
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updatePersonalSetting(authentication, userRequest);
        });

        // ✅ Kiểm tra thông báo lỗi chính xác khi địa chỉ là null
        assertEquals("Address cannot be null", exception.getMessage());
    }

    // test case 14: Cập nhật cài đặt cá nhân thất bại khi địa chỉ không đầy đủ (thiếu trường line)
    @Test
    void testUpdatePersonalSetting_ShouldThrowException_WhenAddressIsIncomplete() {
        // Giả lập Authentication trả về username "testUsername"
        when(authentication.getName()).thenReturn("testUsername");

        // Tạo AddressRequest thiếu trường line (null)
        AddressRequest incompleteAddress = new AddressRequest();
        incompleteAddress.setLine(null); // Thiếu line
        incompleteAddress.setProvinceId(1L);
        incompleteAddress.setDistrictId(2L);
        incompleteAddress.setWardId(3L);

        // Tạo request với địa chỉ không đầy đủ
        ClientPersonalSettingUserRequest userRequest = new ClientPersonalSettingUserRequest();
        userRequest.setUsername("testUsername");
        userRequest.setAddress(incompleteAddress);

        // Gọi phương thức và kiểm tra ném IllegalArgumentException với thông báo "Address is incomplete"
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updatePersonalSetting(authentication, userRequest);
        });

        // Kiểm tra thông báo lỗi đúng
        assertEquals("Address is incomplete", exception.getMessage());
    }

    // test case 15: Cập nhật cài đặt cá nhân thất bại khi địa chỉ chứa giá trị không hợp lệ (ví dụ: provinceId âm)
    @Test
    void testUpdatePersonalSetting_ShouldThrowException_WhenAddressHasInvalidValues() {
        // Giả lập Authentication trả về username "testUsername"
        when(authentication.getName()).thenReturn("testUsername");

        // Tạo AddressRequest với giá trị provinceId không hợp lệ (-1)
        AddressRequest invalidAddress = new AddressRequest();
        invalidAddress.setLine("123 Main Street");
        invalidAddress.setProvinceId(-1L); // Giá trị không hợp lệ
        invalidAddress.setDistrictId(2L);
        invalidAddress.setWardId(3L);

        // Tạo request với địa chỉ không hợp lệ
        ClientPersonalSettingUserRequest userRequest = new ClientPersonalSettingUserRequest();
        userRequest.setUsername("testUsername");
        userRequest.setAddress(invalidAddress);

        // Gọi phương thức và kiểm tra ném IllegalArgumentException với thông báo "Address contains invalid values"
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updatePersonalSetting(authentication, userRequest);
        });

        // Kiểm tra thông báo lỗi đúng
        assertEquals("Address contains invalid values", exception.getMessage());
    }

    // test case 16: Cập nhật cài đặt số điện thoại thành công khi phone hợp lệ
    @Test
    void testUpdatePhoneSetting_ShouldUpdateSuccessfully_WhenPhoneIsValid() {
        // Giả lập Authentication trả về username "testUsername"
        when(authentication.getName()).thenReturn("testUsername");

        // Tạo đối tượng request đầu vào với số điện thoại hợp lệ
        ClientPhoneSettingUserRequest userRequest = new ClientPhoneSettingUserRequest();
        userRequest.setPhone("1234567890");

        // Giả lập một User đang tồn tại trong hệ thống
        User existingUser = new User();
        existingUser.setUsername("testUsername");

        // Tạo UserResponse giả định là kết quả trả về sau cập nhật
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername("testUsername");
        userResponse.setPhone("1234567890");

        // Giả lập quá trình tìm kiếm user theo username
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(existingUser));

        // Giả lập cập nhật thông tin user từ request
        when(userMapper.partialUpdate(existingUser, userRequest)).thenReturn(existingUser);

        // Giả lập lưu user vào database
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        // Giả lập chuyển đổi từ entity sang response DTO
        when(userMapper.entityToResponse(existingUser)).thenReturn(userResponse);

        // Gọi hàm updatePhoneSetting() trong controller và lưu kết quả trả về
        ResponseEntity<UserResponse> response = clientUserController.updatePhoneSetting(authentication, userRequest);

        // Kiểm tra HTTP status là 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Kiểm tra dữ liệu trả về không null
        assertNotNull(response.getBody());

        // Kiểm tra username và phone trong response giống với dữ liệu đầu vào
        assertEquals("testUsername", response.getBody().getUsername());
        assertEquals("1234567890", response.getBody().getPhone());
    }

    // test case 17: Cập nhật cài đặt số điện thoại thất bại khi không tìm thấy người dùng
// Đầu vào: Authentication trả về "testUsername", nhưng không có User nào tương ứng trong database
// Đầu ra mong muốn: Ném ra UsernameNotFoundException
    @Test
    void testUpdatePhoneSetting_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
        // Giả lập Authentication trả về tên người dùng "testUsername"
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập không tìm thấy User trong database
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.empty());

        // Gọi phương thức updatePhoneSetting và kiểm tra xem có ném ra UsernameNotFoundException không
        assertThrows(UsernameNotFoundException.class, () -> {
            clientUserController.updatePhoneSetting(authentication, new ClientPhoneSettingUserRequest());
        });

        // Xác minh rằng phương thức findByUsername được gọi đúng với tham số "testUsername"
        verify(userRepository).findByUsername("testUsername");
    }

    // test case 18: Cập nhật số điện thoại thất bại khi giá trị phone là null hoặc rỗng
// Đầu vào:
//   - Trường hợp 1: phone = null
//   - Trường hợp 2: phone = ""
// Đầu ra mong muốn:
//   - Ném ra IllegalArgumentException với thông điệp "Phone cannot be null or empty" trong cả hai trường hợp
    @Test
    void testUpdatePhoneSetting_ShouldThrowException_WhenPhoneIsNullOrEmpty() {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Trường hợp 1: Phone là null
        ClientPhoneSettingUserRequest requestWithNullPhone = new ClientPhoneSettingUserRequest();
        requestWithNullPhone.setPhone(null);

        // Kiểm tra ngoại lệ cho phone = null
        Exception exception1 = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updatePhoneSetting(authentication, requestWithNullPhone);
        });
        assertEquals("Phone cannot be null or empty", exception1.getMessage());

        // Trường hợp 2: Phone là chuỗi rỗng
        ClientPhoneSettingUserRequest requestWithEmptyPhone = new ClientPhoneSettingUserRequest();
        requestWithEmptyPhone.setPhone("");

        // Kiểm tra ngoại lệ cho phone = ""
        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updatePhoneSetting(authentication, requestWithEmptyPhone);
        });
        assertEquals("Phone cannot be null or empty", exception2.getMessage());
    }

// test case 19: Cập nhật số điện thoại thất bại khi số điện thoại chứa ký tự không hợp lệ
// Đầu vào:
//   - phone = "123-ABC-7890" (chứa chữ cái và dấu gạch ngang)
// Đầu ra mong muốn:
//   - Ném ra IllegalArgumentException với thông báo "Phone contains invalid characters"
    @Test
    void testUpdatePhoneSetting_ShouldThrowException_WhenPhoneContainsInvalidCharacters() {
        // Mock Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Invalid phone number
        ClientPhoneSettingUserRequest requestWithInvalidPhone = new ClientPhoneSettingUserRequest();
        requestWithInvalidPhone.setPhone("123-ABC-7890");

        // Kiểm tra ngoại lệ ném ra khi số điện thoại chứa ký tự không hợp lệ
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updatePhoneSetting(authentication, requestWithInvalidPhone);
        });
        assertEquals("Phone contains invalid characters", exception.getMessage());
    }

// test case 20: Cập nhật email thành công khi email hợp lệ
// Đầu vào:
//   - email = "test@example.com"
//   - username từ authentication = "testUsername"
//   - user tồn tại trong cơ sở dữ liệu
// Đầu ra mong muốn:
//   - Trả về ResponseEntity<UserResponse> với HTTP status 200 (OK)
//   - Thông tin email trong phản hồi là "test@example.com"
    @Test
    void testUpdateEmailSetting_ShouldUpdateSuccessfully_WhenEmailIsValid() {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập dữ liệu đầu vào
        ClientEmailSettingUserRequest userRequest = new ClientEmailSettingUserRequest();
        userRequest.setEmail("test@example.com");

        // Giả lập repository trả về User
        User existingUser = new User();
        existingUser.setUsername("testUsername");

        // Giả lập phản hồi từ mapper
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername("testUsername");
        userResponse.setEmail("test@example.com");

        // Định nghĩa hành vi của các mock
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(existingUser));
        when(userMapper.partialUpdate(existingUser, userRequest)).thenReturn(existingUser);
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.entityToResponse(existingUser)).thenReturn(userResponse);

        // Gọi phương thức và kiểm tra kết quả
        ResponseEntity<UserResponse> response = clientUserController.updateEmailSetting(authentication, userRequest);

        // Kiểm tra HTTP status và dữ liệu trả về
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testUsername", response.getBody().getUsername());
        assertEquals("test@example.com", response.getBody().getEmail());
    }

    // test case 21: Ném UsernameNotFoundException khi không tìm thấy người dùng
    @Test
    void testUpdateEmailSetting_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập không tìm thấy User trong DB
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.empty());

        // Gọi phương thức và kiểm tra exception
        assertThrows(UsernameNotFoundException.class, () -> {
            clientUserController.updateEmailSetting(authentication, new ClientEmailSettingUserRequest());
        });

        // Đảm bảo repository được gọi đúng
        verify(userRepository).findByUsername("testUsername");
    }

    //Ném IllegalArgumentException khi email là null hoặc rỗng
// test case 22: Cập nhật email thất bại khi email là null hoặc chuỗi rỗng
// Đầu vào:
//   - email = null (trường hợp 1)
//   - email = "" (trường hợp 2)
//   - username từ authentication = "testUsername"
// Đầu ra mong muốn:
//   - Phương thức ném ra IllegalArgumentException với message "Email cannot be null or empty"
    @Test
    void testUpdateEmailSetting_ShouldThrowException_WhenEmailIsNullOrEmpty() {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Trường hợp 1: Email là null
        ClientEmailSettingUserRequest requestWithNullEmail = new ClientEmailSettingUserRequest();
        requestWithNullEmail.setEmail(null);

        // Kiểm tra ngoại lệ khi email là null
        Exception exception1 = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updateEmailSetting(authentication, requestWithNullEmail);
        });
        assertEquals("Email cannot be null or empty", exception1.getMessage());

        // Trường hợp 2: Email là chuỗi rỗng
        ClientEmailSettingUserRequest requestWithEmptyEmail = new ClientEmailSettingUserRequest();
        requestWithEmptyEmail.setEmail("");

        // Kiểm tra ngoại lệ khi email là chuỗi rỗng
        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updateEmailSetting(authentication, requestWithEmptyEmail);
        });
        assertEquals("Email cannot be null or empty", exception2.getMessage());
    }


    // test case 23: Cập nhật email thất bại khi email không đúng định dạng
// Đầu vào:
//   - email = "invalid-email" (không có định dạng email hợp lệ)
//   - username từ authentication = "testUsername"
// Đầu ra mong muốn:
//   - Phương thức ném ra IllegalArgumentException với message "Email format is invalid"
    @Test
    void testUpdateEmailSetting_ShouldThrowException_WhenEmailIsInvalid() {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Email không hợp lệ
        ClientEmailSettingUserRequest requestWithInvalidEmail = new ClientEmailSettingUserRequest();
        requestWithInvalidEmail.setEmail("invalid-email");

        // Kiểm tra ngoại lệ khi email không hợp lệ
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updateEmailSetting(authentication, requestWithInvalidEmail);
        });
        assertEquals("Email format is invalid", exception.getMessage());
    }

// test case 24: Cập nhật mật khẩu thành công khi mật khẩu cũ đúng
// Đầu vào:
//   - username trong authentication: "testUsername"
//   - oldPassword: "correctOldPassword" (đúng mật khẩu cũ của user)
//   - newPassword: "newPassword" (mật khẩu mới hợp lệ)
// Đầu ra mong muốn:
//   - HTTP Status 200 OK
//   - Response body là một ObjectNode không rỗng (đại diện cho phản hồi thành công)
    @Test
    void testUpdatePasswordSetting_ShouldUpdateSuccessfully_WhenOldPasswordIsCorrect() throws Exception {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập dữ liệu đầu vào với mật khẩu cũ và mật khẩu mới
        ClientPasswordSettingUserRequest userRequest = new ClientPasswordSettingUserRequest();
        userRequest.setOldPassword("correctOldPassword");
        userRequest.setNewPassword("newPassword");

        // Giả lập repository trả về user tồn tại với mật khẩu mã hóa
        User existingUser = new User();
        existingUser.setUsername("testUsername");
        existingUser.setPassword("encodedOldPassword");

        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(existingUser));
        // Giả lập so sánh mật khẩu cũ đúng
        when(passwordEncoder.matches("correctOldPassword", "encodedOldPassword")).thenReturn(true);
        // Giả lập mã hóa mật khẩu mới
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        // Gọi phương thức và kiểm tra kết quả trả về
        ResponseEntity<ObjectNode> response = clientUserController.updatePasswordSetting(authentication, userRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isObject());
    }

// test case 25: Cập nhật mật khẩu thất bại khi không tìm thấy user theo username trong authentication
// Đầu vào:
//   - username trong authentication: "nonExistentUser" (user không tồn tại trong database)
//   - ClientPasswordSettingUserRequest: bất kỳ (ở đây dùng mặc định không chứa dữ liệu)
// Đầu ra mong muốn:
//   - Ném ra UsernameNotFoundException do không tìm thấy user
    @Test
    void testUpdatePasswordSetting_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
        // Giả lập Authentication với username không tồn tại
        when(authentication.getName()).thenReturn("nonExistentUser");

        // Giả lập repository trả về Optional.empty() nghĩa là user không tồn tại
        when(userRepository.findByUsername("nonExistentUser")).thenReturn(Optional.empty());

        // Gọi phương thức và kiểm tra ném ra UsernameNotFoundException
        assertThrows(UsernameNotFoundException.class, () -> {
            clientUserController.updatePasswordSetting(authentication, new ClientPasswordSettingUserRequest());
        });
    }

// test case 26: Ném Exception khi mật khẩu cũ không đúng
// Đầu vào:
//   - username trong authentication: "testUsername"
//   - ClientPasswordSettingUserRequest với oldPassword = "wrongOldPassword", newPassword = "newPassword"
//   - User trong database với mật khẩu mã hóa "encodedOldPassword"
// Đầu ra mong muốn:
//   - Ném Exception với message "Wrong old password" do mật khẩu cũ không khớp
    @Test
    void testUpdatePasswordSetting_ShouldThrowException_WhenOldPasswordIsIncorrect() {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Giả lập dữ liệu đầu vào
        ClientPasswordSettingUserRequest userRequest = new ClientPasswordSettingUserRequest();
        userRequest.setOldPassword("wrongOldPassword");
        userRequest.setNewPassword("newPassword");

        // Giả lập User tồn tại với mật khẩu mã hóa
        User existingUser = new User();
        existingUser.setUsername("testUsername");
        existingUser.setPassword("encodedOldPassword");

        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(existingUser));
        // Giả lập passwordEncoder.matches trả về false vì mật khẩu cũ không khớp
        when(passwordEncoder.matches("wrongOldPassword", "encodedOldPassword")).thenReturn(false);

        // Gọi phương thức và kiểm tra exception
        Exception exception = assertThrows(Exception.class, () -> {
            clientUserController.updatePasswordSetting(authentication, userRequest);
        });

        assertEquals("Wrong old password", exception.getMessage());
    }

    // test case 27: Ném IllegalArgumentException khi newPassword là null hoặc rỗng
// Đầu vào:
//   - username trong authentication: "testUsername"
//   - ClientPasswordSettingUserRequest với oldPassword = "oldPassword"
//   - newPassword là null hoặc ""
// Đầu ra mong muốn:
//   - Ném IllegalArgumentException với message "New password cannot be null or empty"
    @Test
    void testUpdatePasswordSetting_ShouldThrowException_WhenNewPasswordIsNullOrEmpty() {
        // Giả lập Authentication
        when(authentication.getName()).thenReturn("testUsername");

        // Trường hợp 1: `newPassword` là null
        ClientPasswordSettingUserRequest requestWithNullPassword = new ClientPasswordSettingUserRequest();
        requestWithNullPassword.setOldPassword("oldPassword");
        requestWithNullPassword.setNewPassword(null);

        Exception exception1 = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updatePasswordSetting(authentication, requestWithNullPassword);
        });
        assertEquals("New password cannot be null or empty", exception1.getMessage());

        // Trường hợp 2: `newPassword` là chuỗi rỗng
        ClientPasswordSettingUserRequest requestWithEmptyPassword = new ClientPasswordSettingUserRequest();
        requestWithEmptyPassword.setOldPassword("oldPassword");
        requestWithEmptyPassword.setNewPassword("");

        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> {
            clientUserController.updatePasswordSetting(authentication, requestWithEmptyPassword);
        });
        assertEquals("New password cannot be null or empty", exception2.getMessage());
    }

}

