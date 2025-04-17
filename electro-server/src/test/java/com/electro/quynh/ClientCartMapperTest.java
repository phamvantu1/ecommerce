package com.electro.quynh;

import com.electro.dto.client.ClientCartRequest;
import com.electro.dto.client.ClientCartVariantRequest;
import com.electro.dto.client.UpdateQuantityType;
import com.electro.entity.cart.Cart;
import com.electro.entity.cart.CartVariant;
import com.electro.entity.cart.CartVariantKey;
import com.electro.entity.product.Variant;
import com.electro.mapper.client.ClientCartMapper;
import com.electro.repository.authentication.UserRepository;
import com.electro.repository.inventory.DocketVariantRepository;
import com.electro.repository.product.VariantRepository;
import com.electro.repository.promotion.PromotionRepository;
import com.electro.mapper.promotion.PromotionMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientCartMapperTest {

    // Mock các repository và mapper cần thiết
    private VariantRepository variantRepository;
    private UserRepository userRepository;
    private DocketVariantRepository docketVariantRepository;
    private PromotionRepository promotionRepository;
    private PromotionMapper promotionMapper;

    // Class cần test
    private ClientCartMapper clientCartMapper;

    // Setup mock trước mỗi test
    @BeforeEach
    void setUp() {
        variantRepository = mock(VariantRepository.class);
        userRepository = mock(UserRepository.class);
        docketVariantRepository = mock(DocketVariantRepository.class);
        promotionRepository = mock(PromotionRepository.class);
        promotionMapper = mock(PromotionMapper.class);

        // Khởi tạo đối tượng test
        clientCartMapper = new ClientCartMapper(
                userRepository,
                variantRepository,
                docketVariantRepository,
                promotionRepository,
                promotionMapper
        );
    }

    // Test thêm mới sản phẩm vào giỏ hàng rỗng
    @Test
    void testPartialUpdate_Testcase1() {
        System.out.println("✅ Test: Thêm mới sản phẩm vào giỏ hàng trống");

        Cart cart = new Cart(); // Giỏ hàng mới
        cart.setCartVariants(new HashSet<>());

        Variant variant = new Variant(); // Tạo variant giả lập
        variant.setId(1L);
        when(variantRepository.getById(1L)).thenReturn(variant);

        // Yêu cầu thêm sản phẩm với variantId = 1 và quantity = 2
        ClientCartVariantRequest itemRequest = new ClientCartVariantRequest();
        itemRequest.setVariantId(1L);
        itemRequest.setQuantity(2);

        ClientCartRequest request = new ClientCartRequest();
        request.setCartItems(Set.of(itemRequest));
        request.setUpdateQuantityType(UpdateQuantityType.OVERRIDE); // Ghi đè
        request.setStatus(1);

        Cart result = clientCartMapper.partialUpdate(cart, request);

        System.out.println("🛒 Kết quả số lượng item trong giỏ: " + result.getCartVariants().size());

        assertEquals(1, result.getCartVariants().size());
        CartVariant newItem = result.getCartVariants().iterator().next();
        assertEquals(2, newItem.getQuantity());
        assertEquals(1L, newItem.getVariant().getId());
        assertNotNull(newItem.getCartVariantKey());
    }

    // Test cập nhật lại quantity với kiểu OVERRIDE (ghi đè)
    @Test
    void testPartialUpdate_Testcase2() {
        System.out.println("✅ Test: Override số lượng sản phẩm trong giỏ");

        Variant variant = new Variant(); variant.setId(1L);

        CartVariant cartVariant = new CartVariant(); // Có sẵn trong giỏ
        cartVariant.setVariant(variant);
        cartVariant.setQuantity(1); // Quantity ban đầu
        cartVariant.setCartVariantKey(new CartVariantKey(1L, 1L));

        Cart cart = new Cart();
        cart.setCartVariants(new HashSet<>(Set.of(cartVariant)));

        when(variantRepository.getById(1L)).thenReturn(variant);

        ClientCartVariantRequest requestItem = new ClientCartVariantRequest();
        requestItem.setVariantId(1L);
        requestItem.setQuantity(5); // Ghi đè thành 5

        ClientCartRequest request = new ClientCartRequest();
        request.setCartItems(Set.of(requestItem));
        request.setUpdateQuantityType(UpdateQuantityType.OVERRIDE);

        clientCartMapper.partialUpdate(cart, request);

        System.out.println("🔁 Số lượng sau override: " + cartVariant.getQuantity());

        assertEquals(5, cartVariant.getQuantity());
    }

    // Test cộng thêm số lượng (INCREMENTAL)
    @Test
    void testPartialUpdate_Testcase3() {
        System.out.println("✅ Test: Tăng số lượng sản phẩm trong giỏ (INCREMENT)");

        Variant variant = new Variant(); variant.setId(2L);

        CartVariant cartVariant = new CartVariant();
        cartVariant.setVariant(variant);
        cartVariant.setQuantity(2); // Quantity hiện tại
        cartVariant.setCartVariantKey(new CartVariantKey(1L, 2L));

        Cart cart = new Cart();
        cart.setCartVariants(new HashSet<>(Set.of(cartVariant)));

        when(variantRepository.getById(2L)).thenReturn(variant);

        ClientCartVariantRequest requestItem = new ClientCartVariantRequest();
        requestItem.setVariantId(2L);
        requestItem.setQuantity(3); // Cộng thêm 3

        ClientCartRequest request = new ClientCartRequest();
        request.setCartItems(Set.of(requestItem));
        request.setUpdateQuantityType(UpdateQuantityType.INCREMENTAL);

        clientCartMapper.partialUpdate(cart, request);

        System.out.println("🔼 Số lượng sau cộng dồn: " + cartVariant.getQuantity());

        assertEquals(5, cartVariant.getQuantity());
    }

    // Test cập nhật sản phẩm đã có + thêm sản phẩm mới
    @Test
    void testPartialUpdate_Testcase4() {
        System.out.println("✅ Test: Vừa cập nhật vừa thêm sản phẩm vào giỏ");

        Variant variant1 = new Variant(); variant1.setId(1L);
        Variant variant2 = new Variant(); variant2.setId(2L);

        // Sản phẩm đã có trong giỏ
        CartVariant existing = new CartVariant();
        existing.setVariant(variant1);
        existing.setQuantity(2);
        existing.setCartVariantKey(new CartVariantKey(1L, 1L));

        Cart cart = new Cart();
        cart.setCartVariants(new HashSet<>(Set.of(existing)));

        when(variantRepository.getById(2L)).thenReturn(variant2);

        // Yêu cầu update variant1 + thêm mới variant2
        ClientCartVariantRequest item1 = new ClientCartVariantRequest();
        item1.setVariantId(1L); item1.setQuantity(5);

        ClientCartVariantRequest item2 = new ClientCartVariantRequest();
        item2.setVariantId(2L); item2.setQuantity(3);

        ClientCartRequest request = new ClientCartRequest();
        request.setCartItems(Set.of(item1, item2));
        request.setUpdateQuantityType(UpdateQuantityType.OVERRIDE);

        Cart result = clientCartMapper.partialUpdate(cart, request);

        System.out.println("🧾 Số lượng sản phẩm trong giỏ sau update: " + result.getCartVariants().size());

        assertEquals(2, result.getCartVariants().size());
        assertTrue(result.getCartVariants().stream().anyMatch(cv -> cv.getVariant().getId().equals(2L)));
        assertTrue(result.getCartVariants().stream().anyMatch(cv -> cv.getQuantity() == 5));
    }

    // Test khi variant không tồn tại trong database
    @Test
    void testPartialUpdate_Testcase5() {
        System.out.println("✅ Test: Trường hợp Variant không tồn tại trong DB");

        Cart cart = new Cart();
        cart.setCartVariants(new HashSet<>());

        ClientCartVariantRequest itemRequest = new ClientCartVariantRequest();
        itemRequest.setVariantId(99L);
        itemRequest.setQuantity(1);

        ClientCartRequest request = new ClientCartRequest();
        request.setCartItems(Set.of(itemRequest));
        request.setUpdateQuantityType(UpdateQuantityType.OVERRIDE);

        when(variantRepository.getById(99L)).thenThrow(new RuntimeException("Variant not found"));

        Exception exception = assertThrows(RuntimeException.class, () -> clientCartMapper.partialUpdate(cart, request));

        System.out.println("🚨 Đã bắt được exception như mong đợi: " + exception.getMessage());
    }

    // Test trường hợp request không chứa cartItems
    @Test
    void testPartialUpdate_Testcase6() {
        System.out.println("❌ Request không chứa cartItems");

        Cart cart = new Cart();
        cart.setCartVariants(new HashSet<>());

        ClientCartRequest request = new ClientCartRequest();
        request.setCartItems(null); // không có cartItems

        assertThrows(NullPointerException.class, () -> clientCartMapper.partialUpdate(cart, request));
    }
    // ✅ Test trường hợp variantId bị null trong request (không hợp lệ)
    @Test
    void testPartialUpdate_Testcase7() {
        System.out.println("❌ variantId bị null trong ClientCartVariantRequest");

        // Tạo cart trống (chưa có sản phẩm nào)
        Cart cart = new Cart();
        cart.setCartVariants(new HashSet<>());

        // Tạo request chứa item có variantId = null
        ClientCartVariantRequest item = new ClientCartVariantRequest();
        item.setVariantId(null); // ❌ lỗi ở đây
        item.setQuantity(1);

        // Tạo request chính gửi lên mapper
        ClientCartRequest request = new ClientCartRequest();
        request.setCartItems(Set.of(item));
        request.setUpdateQuantityType(UpdateQuantityType.OVERRIDE);

        // Mong đợi mapper ném ra NullPointerException
        assertThrows(NullPointerException.class, () -> clientCartMapper.partialUpdate(cart, request));
    }

    // ✅ Test khi số lượng sản phẩm (quantity) không hợp lệ (<= 0)
    @Test
    void testPartialUpdate_Testcase8() {
        System.out.println("❌ Quantity âm hoặc bằng 0");

        Cart cart = new Cart();
        cart.setCartVariants(new HashSet<>());

        // Tạo variant giả lập với id = 1
        Variant variant = new Variant();
        variant.setId(1L);
        when(variantRepository.getById(1L)).thenReturn(variant);

        // Tạo item với quantity = 0 (không hợp lệ)
        ClientCartVariantRequest item = new ClientCartVariantRequest();
        item.setVariantId(1L);
        item.setQuantity(0); // ❌ quantity không hợp lệ

        ClientCartRequest request = new ClientCartRequest();
        request.setCartItems(Set.of(item));
        request.setUpdateQuantityType(UpdateQuantityType.OVERRIDE);

        // Gọi mapper
        Cart result = clientCartMapper.partialUpdate(cart, request);

        // ❗️ Không thêm sản phẩm nào vào giỏ (do quantity không hợp lệ)
        assertTrue(result.getCartVariants().isEmpty());
    }

    // ✅ Test trường hợp truyền cart null vào mapper
    @Test
    void testPartialUpdate_Testcase9() {
        System.out.println("❌ Cập nhật sản phẩm khi cart == null");

        // Tạo item hợp lệ
        ClientCartVariantRequest item = new ClientCartVariantRequest();
        item.setVariantId(1L);
        item.setQuantity(2);

        // Gói item vào request
        ClientCartRequest request = new ClientCartRequest();
        request.setCartItems(Set.of(item));
        request.setUpdateQuantityType(UpdateQuantityType.OVERRIDE);

        // ❗️ Truyền cart = null -> mong đợi NullPointerException
        assertThrows(NullPointerException.class, () -> clientCartMapper.partialUpdate(null, request));
    }
    //✅ Override số lượng nhiều sản phẩm cùng lúc
    @Test
    void testPartialUpdate_Testcase10() {
        System.out.println("✅ Override số lượng nhiều sản phẩm cùng lúc");

        // Setup cart hiện có
        Cart cart = new Cart();
        Set<CartVariant> existingItems = new HashSet<>();

        Variant variant1 = new Variant(); variant1.setId(1L);
        Variant variant2 = new Variant(); variant2.setId(2L);

        CartVariant cv1 = new CartVariant(); cv1.setVariant(variant1); cv1.setQuantity(1);
        CartVariant cv2 = new CartVariant(); cv2.setVariant(variant2); cv2.setQuantity(3);

        existingItems.add(cv1);
        existingItems.add(cv2);
        cart.setCartVariants(existingItems);

        // Cập nhật override mới cho nhiều sản phẩm
        ClientCartVariantRequest item1 = new ClientCartVariantRequest();
        item1.setVariantId(1L);
        item1.setQuantity(10);

        ClientCartVariantRequest item2 = new ClientCartVariantRequest();
        item2.setVariantId(2L);
        item2.setQuantity(20);

        ClientCartRequest request = new ClientCartRequest();
        request.setCartItems(Set.of(item1, item2));
        request.setUpdateQuantityType(UpdateQuantityType.OVERRIDE);

        when(variantRepository.getById(1L)).thenReturn(variant1);
        when(variantRepository.getById(2L)).thenReturn(variant2);

        Cart updatedCart = clientCartMapper.partialUpdate(cart, request);

        assertEquals(2, updatedCart.getCartVariants().size());

        for (CartVariant cv : updatedCart.getCartVariants()) {
            if (cv.getVariant().getId().equals(1L)) {
                assertEquals(10, cv.getQuantity());
            } else if (cv.getVariant().getId().equals(2L)) {
                assertEquals(20, cv.getQuantity());
            }
        }
    }
    //❌ Cập nhật sản phẩm khi cart == null
    @Test
    void testPartialUpdate_Testcase11() {
        System.out.println("❌ Cập nhật sản phẩm khi cart == null");

        Variant variant = new Variant(); variant.setId(1L);
        when(variantRepository.getById(1L)).thenReturn(variant);

        ClientCartVariantRequest item = new ClientCartVariantRequest();
        item.setVariantId(1L);
        item.setQuantity(2);

        ClientCartRequest request = new ClientCartRequest();
        request.setCartItems(Set.of(item));
        request.setUpdateQuantityType(UpdateQuantityType.OVERRIDE);

        // Truyền vào null cart
        assertThrows(NullPointerException.class, () -> clientCartMapper.partialUpdate(null, request));
    }

}
