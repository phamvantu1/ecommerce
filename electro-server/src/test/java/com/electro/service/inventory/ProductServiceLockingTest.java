package com.electro.service.inventory;

import com.electro.entity.product.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.electro.repository.product.ProductRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ProductServiceLockingTest {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * Test case 226-5: Kiểm tra database locking khi tạo sản phẩm trùng mã
     * 
     * Mục tiêu:
     * - Kiểm tra xử lý đồng thời khi hai người dùng cùng tạo sản phẩm trùng mã
     * - Đảm bảo chỉ một request được xử lý thành công
     */
    @Test
    @Transactional
    public void testConcurrentProductCodeCreation() throws InterruptedException, ExecutionException {
        String duplicateCode = "TEST-CODE-001";
        
        // Tạo sản phẩm 1
        Product product1 = new Product();
        product1.setName("Sản phẩm 1");
        product1.setCode(duplicateCode);
        product1.setSlug("san-pham-1");
        product1.setStatus(1);

        // Tạo sản phẩm 2
        Product product2 = new Product();
        product2.setName("Sản phẩm 2");
        product2.setCode(duplicateCode);
        product2.setSlug("san-pham-2");
        product2.setStatus(1);

        // Tạo thread pool với 2 thread
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        // Thực hiện đồng thời 2 request tạo sản phẩm
        futures.add(CompletableFuture.supplyAsync(() -> {
            try {
                Product savedProduct = productRepository.save(product1);
                System.out.println("Request 1 thành công, sản phẩm ID: " + savedProduct.getId());
                return true;
            } catch (Exception e) {
                System.out.println("Request 1 thất bại: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor));

        futures.add(CompletableFuture.supplyAsync(() -> {
            try {
                Product savedProduct = productRepository.save(product2);
                System.out.println("Request 2 thành công, sản phẩm ID: " + savedProduct.getId());
                return true;
            } catch (Exception e) {
                System.out.println("Request 2 thất bại: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor));

        // Đợi kết quả
        List<Boolean> results = new ArrayList<>();
        for (CompletableFuture<Boolean> future : futures) {
            results.add(future.get());
        }

        System.out.println("Kết quả các request: " + results);

        // Kiểm tra kết quả
        int successCount = (int) results.stream().filter(Boolean::booleanValue).count();
        int failureCount = (int) results.stream().filter(b -> !b).count();
        
        System.out.println("Số request thành công: " + successCount);
        System.out.println("Số request thất bại: " + failureCount);
        
        assertEquals(1, successCount, "Chỉ một request phải thành công");
        assertEquals(1, failureCount, "Chỉ một request phải thất bại");
        
        // Kiểm tra trong database
        List<Product> allProducts = productRepository.findAll();
        List<Product> productsWithDuplicateCode = allProducts.stream()
                .filter(p -> duplicateCode.equals(p.getCode()))
                .collect(Collectors.toList());
        
        System.out.println("Số sản phẩm tìm thấy với mã " + duplicateCode + ": " + productsWithDuplicateCode.size());
        productsWithDuplicateCode.forEach(p -> System.out.println("Sản phẩm ID: " + p.getId() + ", Mã: " + p.getCode()));
        
        assertEquals(1, productsWithDuplicateCode.size(), "Chỉ một sản phẩm được lưu thành công");
    }

    /**
     * Test case 226-6: Kiểm tra database locking khi chỉnh sửa sản phẩm đồng thời
     * 
     * Mục tiêu:
     * - Kiểm tra xử lý đồng thời khi hai người dùng cùng chỉnh sửa một sản phẩm
     * - Đảm bảo chỉ một request được xử lý thành công
     */
    @Test
    public void testConcurrentProductUpdate() throws InterruptedException, ExecutionException {
        // Tạo transaction riêng để lưu sản phẩm ban đầu
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            // Tạo sản phẩm ban đầu
            Product initialProduct = new Product();
            initialProduct.setName("Sản phẩm ban đầu");
            initialProduct.setCode("TEST-CODE-002");
            initialProduct.setSlug("san-pham-ban-dau");
            initialProduct.setStatus(1);
            
            Product savedProduct = productRepository.save(initialProduct);
            transactionManager.commit(status);
            System.out.println("Đã tạo sản phẩm ban đầu với ID: " + savedProduct.getId());

            // Tạo thread pool với 2 thread
            ExecutorService executor = Executors.newFixedThreadPool(2);
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();

            // Request 1: Cập nhật tên sản phẩm
            futures.add(CompletableFuture.supplyAsync(() -> {
                TransactionStatus status1 = transactionManager.getTransaction(new DefaultTransactionDefinition());
                try {
                    Product productToUpdate = productRepository.findById(savedProduct.getId()).orElseThrow();
                    productToUpdate.setName("Sản phẩm cập nhật 1");
                    Product updatedProduct = productRepository.save(productToUpdate);
                    transactionManager.commit(status1);
                    System.out.println("Request 1 thành công, sản phẩm ID: " + updatedProduct.getId() + 
                                     ", tên mới: " + updatedProduct.getName());
                    return true;
                } catch (Exception e) {
                    transactionManager.rollback(status1);
                    System.out.println("Request 1 thất bại: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }, executor));

            // Request 2: Cập nhật slug sản phẩm
            futures.add(CompletableFuture.supplyAsync(() -> {
                TransactionStatus status2 = transactionManager.getTransaction(new DefaultTransactionDefinition());
                try {
                    Product productToUpdate = productRepository.findById(savedProduct.getId()).orElseThrow();
                    productToUpdate.setSlug("san-pham-cap-nhat-2");
                    Product updatedProduct = productRepository.save(productToUpdate);
                    transactionManager.commit(status2);
                    System.out.println("Request 2 thành công, sản phẩm ID: " + updatedProduct.getId() + 
                                     ", slug mới: " + updatedProduct.getSlug());
                    return true;
                } catch (Exception e) {
                    transactionManager.rollback(status2);
                    System.out.println("Request 2 thất bại: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }, executor));

            // Đợi kết quả
            List<Boolean> results = new ArrayList<>();
            for (CompletableFuture<Boolean> future : futures) {
                results.add(future.get());
            }

            System.out.println("Kết quả các request: " + results);

            // Kiểm tra kết quả
            int successCount = (int) results.stream().filter(Boolean::booleanValue).count();
            int failureCount = (int) results.stream().filter(b -> !b).count();
            
            System.out.println("Số request thành công: " + successCount);
            System.out.println("Số request thất bại: " + failureCount);
            
            assertEquals(1, successCount, "Chỉ một request phải thành công");
            assertEquals(1, failureCount, "Chỉ một request phải thất bại");
            
            // Kiểm tra trong database
            TransactionStatus finalStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
            try {
                Product finalProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
                System.out.println("Trạng thái cuối cùng của sản phẩm:");
                System.out.println("ID: " + finalProduct.getId());
                System.out.println("Tên: " + finalProduct.getName());
                System.out.println("Slug: " + finalProduct.getSlug());
                
                // Kiểm tra xem chỉ một trong hai thay đổi được áp dụng
                boolean nameUpdated = "Sản phẩm cập nhật 1".equals(finalProduct.getName());
                boolean slugUpdated = "san-pham-cap-nhat-2".equals(finalProduct.getSlug());
                assertTrue(nameUpdated || slugUpdated, "Ít nhất một thay đổi phải được áp dụng");
                assertFalse(nameUpdated && slugUpdated, "Không thể cả hai thay đổi đều được áp dụng");
                transactionManager.commit(finalStatus);
            } catch (Exception e) {
                transactionManager.rollback(finalStatus);
                throw e;
            }
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        }
    }
}