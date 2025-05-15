package com.electro.controller.client;

import com.electro.constant.AppConstants;
import com.electro.dto.ListResponse;
import com.electro.dto.client.ClientListedProductResponse;
import com.electro.dto.client.ClientProductResponse;
import com.electro.entity.product.Category;
import com.electro.entity.product.Product;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.client.ClientProductMapper;
import com.electro.projection.inventory.SimpleProductInventory;
import com.electro.repository.ProjectionRepository;
import com.electro.repository.product.ProductRepository;
import com.electro.repository.review.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Kích hoạt Mockito để hỗ trợ mock dependencies
@ExtendWith(MockitoExtension.class)
class ClientProductControllerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProjectionRepository projectionRepository;

    @Mock
    private ClientProductMapper clientProductMapper;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ClientProductController clientProductController;

    private Product product;
    private SimpleProductInventory inventory;
    private ClientListedProductResponse listedResponse;
    private ClientProductResponse productResponse;

    // Thiết lập dữ liệu ban đầu trước mỗi test case
    @BeforeEach
    void setUp() {
        // Khởi tạo đối tượng Product với ID, slug và category
        product = new Product();
        product.setId(1L);
        product.setSlug("test-product");
        Category category = new Category();
        category.setId(1L);
        product.setCategory(category);

        // Khởi tạo SimpleProductInventory với constructor đã cung cấp
        inventory = new SimpleProductInventory(1L, 100L, 20L, 80L, 50L);

        // Khởi tạo ClientListedProductResponse để trả về trong danh sách sản phẩm
        listedResponse = new ClientListedProductResponse();
        listedResponse.setProductId(1L);
        listedResponse.setProductSlug("test-product");

        // Khởi tạo ClientProductResponse để trả về chi tiết sản phẩm
        productResponse = new ClientProductResponse();
        productResponse.setProductId(1L);
        productResponse.setProductSlug("test-product");
    }


    @Test
    void getAllProducts_Testcase001() {
        // Mục đích: Kiểm tra lấy danh sách sản phẩm
        // Bối cảnh (Arrange):
        // - Thiết lập tham số phân trang: page=1, size=10
        // - Mock productRepository trả về một trang chứa 1 sản phẩm
        // - Mock projectionRepository trả về danh sách tồn kho với thông tin đầy đủ
        // - Mock clientProductMapper ánh xạ sản phẩm và tồn kho thành ClientListedProductResponse
        int page = 1;
        int size = 10;
        Pageable pageable = PageRequest.of(page - 1, size);
        List<Product> productList = List.of(product);
        Page<Product> productPage = new PageImpl<>(productList, pageable, 1);
        List<SimpleProductInventory> inventories = List.of(inventory);

        when(productRepository.findByParams(null, null, null, false, false, pageable)).thenReturn(productPage);
        when(projectionRepository.findSimpleProductInventories(List.of(1L))).thenReturn(inventories);
        when(clientProductMapper.entityToListedResponse(product, inventories)).thenReturn(listedResponse);

        // Hành động (Act):
        // - Gọi API với các tham số mặc định
        ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController.getAllProducts(page, size, null, null, null, false, false);

        // Kỳ vọng (Assert):
        // - Trạng thái HTTP là 200 OK
        // - Response chứa danh sách sản phẩm đúng (1 sản phẩm)
        // - Tổng số phần tử là 1
        // - Xác minh các phương thức mock được gọi đúng
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ListResponse<ClientListedProductResponse> body = response.getBody();
        assertNotNull(body);
        assertEquals(List.of(listedResponse), body.getContent());
        assertEquals(1, body.getPage());
        verify(productRepository).findByParams(null, null, null, false, false, pageable);
        verify(projectionRepository).findSimpleProductInventories(List.of(1L));
        verify(clientProductMapper).entityToListedResponse(product, inventories);
    }

    @Test
    void getAllProducts_Testcase002() {
        // Mục đích: Kiểm tra lấy danh sách sản phẩm mới (newable=true) với thông tin tồn kho
        // Bối cảnh (Arrange):
        // - Thiết lập phân trang: page=1, size=5
        // - Mock productRepository trả về một trang chứa 1 sản phẩm với newable=true
        // - Mock projectionRepository trả về tồn kho với các giá trị khác để kiểm tra tính linh hoạt
        // - Mock clientProductMapper ánh xạ sản phẩm
        int page = 1;
        int size = 5;
        Pageable pageable = PageRequest.of(page - 1, size);
        List<Product> productList = List.of(product);
        Page<Product> productPage = new PageImpl<>(productList, pageable, 1);
        List<SimpleProductInventory> inventories = List.of(new SimpleProductInventory(1L, 50L, 10L, 40L, 30L));

        when(productRepository.findByParams(null, null, null, false, true, pageable)).thenReturn(productPage);
        when(projectionRepository.findSimpleProductInventories(List.of(1L))).thenReturn(inventories);
        when(clientProductMapper.entityToListedResponse(product, inventories)).thenReturn(listedResponse);

        // Hành động (Act):
        // - Gọi API với newable=true
        ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController.getAllProducts(page, size, null, null, null, false, true);

        // Kỳ vọng (Assert):
        // - Trạng thái HTTP là 200 OK
        // - Response chứa 1 sản phẩm
        // - Xác minh tham số newable được truyền vào repository
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getContent().size());
        verify(productRepository).findByParams(null, null, null, false, true, pageable);
        verify(projectionRepository).findSimpleProductInventories(List.of(1L));
        verify(clientProductMapper).entityToListedResponse(product, inventories);

        // Ý nghĩa thực tế:
        // - Đảm bảo API lọc đúng các sản phẩm mới
        // - Xác minh khả năng xử lý các giá trị tồn kho khác nhau
    }

    @Test
    void getAllProducts_Testcase003() {
        // Mục đích: Kiểm tra khi không có thông tin tồn kho cho sản phẩm
        // Bối cảnh (Arrange):
        // - Thiết lập phân trang: page=1, size=10
        // - Mock productRepository trả về một trang chứa 1 sản phẩm
        // - Mock projectionRepository trả về danh sách tồn kho rỗng
        // - Mock clientProductMapper ánh xạ với danh sách tồn kho rỗng
        int page = 1;
        int size = 10;
        Pageable pageable = PageRequest.of(page - 1, size);
        List<Product> productList = List.of(product);
        Page<Product> productPage = new PageImpl<>(productList, pageable, 1);

        when(productRepository.findByParams(null, null, null, false, false, pageable)).thenReturn(productPage);
        when(projectionRepository.findSimpleProductInventories(List.of(1L))).thenReturn(Collections.emptyList());
        when(clientProductMapper.entityToListedResponse(product, Collections.emptyList())).thenReturn(listedResponse);

        // Hành động (Act):
        // - Gọi API với tham số mặc định
        ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController.getAllProducts(page, size, null, null, null, false, false);

        // Kỳ vọng (Assert):
        // - Trạng thái HTTP là 200 OK
        // - Response chứa 1 sản phẩm
        // - Mapper được gọi với danh sách tồn kho rỗng
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getContent().size());
        verify(projectionRepository).findSimpleProductInventories(List.of(1L));
        verify(clientProductMapper).entityToListedResponse(product, Collections.emptyList());
    }

    @Test
    void getAllProducts_Testcase004() {
        // Mục đích: Kiểm tra khi không có sản phẩm nào khớp với tham số tìm kiếm
        // Bối cảnh (Arrange):
        // - Thiết lập phân trang: page=1, size=10
        // - Mock productRepository trả về trang rỗng
        // - Mock projectionRepository trả về danh sách tồn kho rỗng (do không có sản phẩm)
        int page = 1;
        int size = 10;
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(productRepository.findByParams(null, null, null, false, false, pageable)).thenReturn(emptyPage);
        when(projectionRepository.findSimpleProductInventories(Collections.emptyList())).thenReturn(Collections.emptyList());

        // Hành động (Act):
        // - Gọi API với tham số mặc định
        ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController.getAllProducts(page, size, null, null, null, false, false);

        // Kỳ vọng (Assert):
        // - Trạng thái HTTP là 200 OK
        // - Response chứa danh sách rỗng
        // - Tổng số phần tử là 0
        // - Không gọi clientProductMapper vì không có sản phẩm
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getContent().isEmpty());
        assertEquals(0, response.getBody().getPage());
        verify(productRepository).findByParams(null, null, null, false, false, pageable);
        verify(projectionRepository).findSimpleProductInventories(Collections.emptyList());
        verifyNoInteractions(clientProductMapper);

    }

    @Test
    void getAllProducts_Testcase005() {
        // Mục đích: Kiểm tra lấy danh sách sản phẩm với bộ lọc và sắp xếp
        // Bối cảnh (Arrange):
        // - Thiết lập phân trang: page=1, size=10
        // - Thiết lập filter="category.id==1" và sort="price,desc"
        // - Mock productRepository trả về một trang chứa 1 sản phẩm
        // - Mock projectionRepository trả về tồn kho với giá trị cao hơn
        // - Mock clientProductMapper ánh xạ sản phẩm
        int page = 1;
        int size = 10;
        String filter = "category.id==1";
        String sort = "price,desc";
        Pageable pageable = PageRequest.of(page - 1, size);
        List<Product> productList = List.of(product);
        Page<Product> productPage = new PageImpl<>(productList, pageable, 1);
        List<SimpleProductInventory> inventories = List.of(new SimpleProductInventory(1L, 200L, 30L, 170L, 100L));

        when(productRepository.findByParams(filter, sort, null, false, false, pageable)).thenReturn(productPage);
        when(projectionRepository.findSimpleProductInventories(List.of(1L))).thenReturn(inventories);
        when(clientProductMapper.entityToListedResponse(product, inventories)).thenReturn(listedResponse);

        // Hành động (Act):
        // - Gọi API với filter và sort
        ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController.getAllProducts(page, size, filter, sort, null, false, false);

        // Kỳ vọng (Assert):
        // - Trạng thái HTTP là 200 OK
        // - Response chứa 1 sản phẩm
        // - Xác minh filter và sort được truyền đúng vào repository
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getContent().size());
        verify(productRepository).findByParams(filter, sort, null, false, false, pageable);
        verify(projectionRepository).findSimpleProductInventories(List.of(1L));
        verify(clientProductMapper).entityToListedResponse(product, inventories);

    }

    @Test
    void getAllProducts_Testcase006() {
        // Mục đích: Kiểm tra khi tham số filter có cú pháp không hợp lệ
        // Bối cảnh (Arrange):
        // - Thiết lập phân trang: page=1, size=10
        // - Thiết lập filter không hợp lệ: "invalid-filter-syntax"
        // - Mock productRepository ném IllegalArgumentException khi nhận filter sai
        int page = 1;
        int size = 10;
        String filter = "invalid-filter-syntax";
        Pageable pageable = PageRequest.of(page - 1, size);
        when(productRepository.findByParams(filter, null, null, false, false, pageable))
                .thenThrow(new IllegalArgumentException("Invalid filter syntax"));

        // Hành động (Act) & Kỳ vọng (Assert):
        // - Gọi API với filter không hợp lệ
        // - Mong đợi ném IllegalArgumentException
        // - Xác minh không gọi projectionRepository hoặc clientProductMapper
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                clientProductController.getAllProducts(page, size, filter, null, null, false, false));
        assertEquals("Invalid filter syntax", exception.getMessage());
        verify(productRepository).findByParams(filter, null, null, false, false, pageable);
        verifyNoInteractions(projectionRepository, clientProductMapper);

    }


    @Test
    void getProduct_Testcase001() {
        // Mục đích: Kiểm tra lấy chi tiết sản phẩm với slug hợp lệ và thông tin tồn kho đầy đủ
        // Bối cảnh (Arrange):
        // - Thiết lập slug="test-product"
        // - Mock productRepository trả về sản phẩm hợp lệ
        // - Mock projectionRepository trả về tồn kho với tất cả các trường (inventory, waitingForDelivery, canBeSold, areComing)
        // - Mock reviewRepository trả về điểm đánh giá trung bình (4) và số lượng đánh giá (15)
        // - Mock không có sản phẩm liên quan
        // - Mock clientProductMapper ánh xạ sản phẩm và tồn kho
        String slug = "test-product";
        int averageRatingScore = 4;
        int countReviews = 15;
        List<SimpleProductInventory> inventories = List.of(new SimpleProductInventory(1L, 100L, 20L, 80L, 50L));
        Pageable relatedPageable = PageRequest.of(0, 4);
        Page<Product> relatedPage = new PageImpl<>(Collections.emptyList(), relatedPageable, 0);

        when(productRepository.findBySlug(slug)).thenReturn(Optional.of(product));
        when(projectionRepository.findSimpleProductInventories(List.of(1L))).thenReturn(inventories);
        when(reviewRepository.findAverageRatingScoreByProductId(1L)).thenReturn(averageRatingScore);
        when(reviewRepository.countByProductId(1L)).thenReturn(countReviews);
        when(productRepository.findByParams(anyString(), eq("random"), isNull(), eq(false), eq(false), eq(relatedPageable)))
                .thenReturn(relatedPage);
        when(projectionRepository.findSimpleProductInventories(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(clientProductMapper.entityToResponse(eq(product), eq(inventories), eq(averageRatingScore), eq(countReviews), eq(Collections.emptyList())))
                .thenReturn(productResponse);

        // Hành động (Act):
        // - Gọi API với slug hợp lệ
        ResponseEntity<ClientProductResponse> response = clientProductController.getProduct(slug);

        // Kỳ vọng (Assert):
        // - Trạng thái HTTP là 200 OK
        // - Response trả về ClientProductResponse đúng
        // - Xác minh các phương thức mock được gọi đúng
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productResponse, response.getBody());
        verify(projectionRepository).findSimpleProductInventories(List.of(1L));
        verify(clientProductMapper).entityToResponse(product, inventories, averageRatingScore, countReviews, Collections.emptyList());

    }

    @Test
    void getProduct_Testcase002() {
        // Mục đích: Kiểm tra khi slug không tồn tại trong hệ thống
        // Bối cảnh (Arrange):
        // - Thiết lập slug không hợp lệ: "invalid-slug"
        // - Mock productRepository trả về Optional.empty()
        String slug = "invalid-slug";
        when(productRepository.findBySlug(slug)).thenReturn(Optional.empty());

        // Hành động (Act) & Kỳ vọng (Assert):
        // - Gọi API với slug không hợp lệ
        // - Mong đợi ném ResourceNotFoundException với thông tin resource, field, và giá trị
        // - Xác minh không tương tác với các repository khác
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                clientProductController.getProduct(slug));
        verify(productRepository).findBySlug(slug);
        verifyNoInteractions(projectionRepository, reviewRepository, clientProductMapper);
    }

    @Test
    void getProduct_Testcase003() {
        // Mục đích: Kiểm tra khi sản phẩm có tất cả các trường tồn kho bằng 0
        // Bối cảnh (Arrange):
        // - Thiết lập slug="test-product"
        // - Mock productRepository trả về sản phẩm hợp lệ
        // - Mock projectionRepository trả về tồn kho với tất cả các trường bằng 0
        // - Mock reviewRepository trả về 0 đánh giá
        // - Mock không có sản phẩm liên quan
        String slug = "test-product";
        int averageRatingScore = 0;
        int countReviews = 0;
        List<SimpleProductInventory> inventories = List.of(new SimpleProductInventory(1L, 0L, 0L, 0L, 0L));
        Pageable relatedPageable = PageRequest.of(0, 4);
        Page<Product> relatedPage = new PageImpl<>(Collections.emptyList(), relatedPageable, 0);

        when(productRepository.findBySlug(slug)).thenReturn(Optional.of(product));
        when(projectionRepository.findSimpleProductInventories(List.of(1L))).thenReturn(inventories);
        when(reviewRepository.findAverageRatingScoreByProductId(1L)).thenReturn(averageRatingScore);
        when(reviewRepository.countByProductId(1L)).thenReturn(countReviews);
        when(productRepository.findByParams(anyString(), eq("random"), isNull(), eq(false), eq(false), eq(relatedPageable)))
                .thenReturn(relatedPage);
        when(projectionRepository.findSimpleProductInventories(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(clientProductMapper.entityToResponse(eq(product), eq(inventories), eq(averageRatingScore), eq(countReviews), eq(Collections.emptyList())))
                .thenReturn(productResponse);

        // Hành động (Act):
        // - Gọi API với slug hợp lệ
        ResponseEntity<ClientProductResponse> response = clientProductController.getProduct(slug);

        // Kỳ vọng (Assert):
        // - Trạng thái HTTP là 200 OK
        // - Response trả về ClientProductResponse với tồn kho bằng 0
        // - Xác minh tồn kho được truyền vào mapper
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productResponse, response.getBody());
        verify(projectionRepository).findSimpleProductInventories(List.of(1L));
        verify(clientProductMapper).entityToResponse(product, inventories, averageRatingScore, countReviews, Collections.emptyList());

    }

    @Test
    void getProduct_Testcase004() {
        // Mục đích: Kiểm tra lấy sản phẩm với sản phẩm liên quan, mỗi sản phẩm có thông tin tồn kho
        // Bối cảnh (Arrange):
        // - Thiết lập slug="test-product"
        // - Mock productRepository trả về sản phẩm hợp lệ
        // - Mock projectionRepository trả về tồn kho cho sản phẩm chính
        // - Mock reviewRepository trả về đánh giá (điểm 4, 10 đánh giá)
        // - Mock productRepository trả về 1 sản phẩm liên quan
        // - Mock projectionRepository trả về tồn kho cho sản phẩm liên quan
        // - Mock clientProductMapper ánh xạ sản phẩm và sản phẩm liên quan
        String slug = "test-product";
        int averageRatingScore = 4;
        int countReviews = 10;
        List<SimpleProductInventory> inventories = List.of(new SimpleProductInventory(1L, 100L, 20L, 80L, 50L));
        Pageable relatedPageable = PageRequest.of(0, 4);
        Product relatedProduct = new Product();
        relatedProduct.setId(2L);
        Page<Product> relatedPage = new PageImpl<>(List.of(relatedProduct), relatedPageable, 1);
        List<SimpleProductInventory> relatedInventories = List.of(new SimpleProductInventory(2L, 200L, 30L, 170L, 100L));
        List<ClientListedProductResponse> relatedResponses = List.of(listedResponse);

        when(productRepository.findBySlug(slug)).thenReturn(Optional.of(product));
        when(projectionRepository.findSimpleProductInventories(List.of(1L))).thenReturn(inventories);
        when(reviewRepository.findAverageRatingScoreByProductId(1L)).thenReturn(averageRatingScore);
        when(reviewRepository.countByProductId(1L)).thenReturn(countReviews);
        when(productRepository.findByParams(eq("category.id==1;id!=1"), eq("random"), isNull(), eq(false), eq(false), eq(relatedPageable)))
                .thenReturn(relatedPage);
        when(projectionRepository.findSimpleProductInventories(List.of(2L))).thenReturn(relatedInventories);
        when(clientProductMapper.entityToListedResponse(relatedProduct, relatedInventories)).thenReturn(listedResponse);
        when(clientProductMapper.entityToResponse(eq(product), eq(inventories), eq(averageRatingScore), eq(countReviews), eq(relatedResponses)))
                .thenReturn(productResponse);

        // Hành động (Act):
        // - Gọi API với slug hợp lệ
        ResponseEntity<ClientProductResponse> response = clientProductController.getProduct(slug);

        // Kỳ vọng (Assert):
        // - Trạng thái HTTP là 200 OK
        // - Response trả về ClientProductResponse với sản phẩm liên quan
        // - Xác minh tồn kho của cả sản phẩm chính và liên quan được xử lý đúng
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productResponse, response.getBody());
        verify(projectionRepository).findSimpleProductInventories(List.of(1L));
        verify(projectionRepository).findSimpleProductInventories(List.of(2L));
        verify(clientProductMapper).entityToListedResponse(relatedProduct, relatedInventories);
        verify(clientProductMapper).entityToResponse(product, inventories, averageRatingScore, countReviews, relatedResponses);

        // Ý nghĩa thực tế:
        // - Đảm bảo API trả về sản phẩm với thông tin liên quan chính xác
        // - Xác minh xử lý tồn kho riêng cho sản phẩm chính và liên quan
    }

    @Test
    void getProduct_Testcase005() {
        // Mục đích: Kiểm tra khi các trường tồn kho (inventory, waitingForDelivery, canBeSold, areComing) là null
        // Bối cảnh (Arrange):
        // - Thiết lập slug="test-product"
        // - Mock productRepository trả về sản phẩm hợp lệ
        // - Mock projectionRepository trả về tồn kho với tất cả các trường null
        // - Mock reviewRepository trả về 0 đánh giá
        // - Mock không có sản phẩm liên quan
        String slug = "test-product";
        int averageRatingScore = 0;
        int countReviews = 0;
        List<SimpleProductInventory> inventories = List.of(new SimpleProductInventory(1L, null, null, null, null));
        Pageable relatedPageable = PageRequest.of(0, 4);
        Page<Product> relatedPage = new PageImpl<>(Collections.emptyList(), relatedPageable, 0);

        when(productRepository.findBySlug(slug)).thenReturn(Optional.of(product));
        when(projectionRepository.findSimpleProductInventories(List.of(1L))).thenReturn(inventories);
        when(reviewRepository.findAverageRatingScoreByProductId(1L)).thenReturn(averageRatingScore);
        when(reviewRepository.countByProductId(1L)).thenReturn(countReviews);
        when(productRepository.findByParams(anyString(), eq("random"), isNull(), eq(false), eq(false), eq(relatedPageable)))
                .thenReturn(relatedPage);
        when(projectionRepository.findSimpleProductInventories(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(clientProductMapper.entityToResponse(eq(product), eq(inventories), eq(averageRatingScore), eq(countReviews), eq(Collections.emptyList())))
                .thenReturn(productResponse);

        // Hành động (Act):
        // - Gọi API với slug hợp lệ
        ResponseEntity<ClientProductResponse> response = clientProductController.getProduct(slug);

        // Kỳ vọng (Assert):
        // - Trạng thái HTTP là 200 OK
        // - Response trả về ClientProductResponse với tồn kho null
        // - Xác minh mapper xử lý đúng dữ liệu null
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productResponse, response.getBody());
        verify(projectionRepository).findSimpleProductInventories(List.of(1L));
        verify(clientProductMapper).entityToResponse(product, inventories, averageRatingScore, countReviews, Collections.emptyList());

        // Ý nghĩa thực tế:
        // - Đảm bảo API xử lý trường hợp dữ liệu tồn kho không đầy đủ
        // - Xác minh hệ thống không bị lỗi khi các trường tồn kho là null
    }

    @Test
    void getProduct_Testcase006() {
        // Mục đích: Kiểm tra khi repository gặp lỗi (ví dụ: lỗi kết nối cơ sở dữ liệu)
        // Bối cảnh (Arrange):
        // - Thiết lập slug="test-product"
        // - Mock productRepository ném RuntimeException khi tìm slug
        String slug = "test-product";
        when(productRepository.findBySlug(slug)).thenThrow(new RuntimeException("Database connection error"));

        // Hành động (Act) & Kỳ vọng (Assert):
        // - Gọi API với slug hợp lệ
        // - Mong đợi ném RuntimeException
        // - Xác minh không tương tác với các repository khác
        RuntimeException exception = assertThrows(RuntimeException.class, () -> clientProductController.getProduct(slug));
        assertEquals("Database connection error", exception.getMessage());
        verify(productRepository).findBySlug(slug);
        verifyNoInteractions(projectionRepository, reviewRepository, clientProductMapper);
    }
}