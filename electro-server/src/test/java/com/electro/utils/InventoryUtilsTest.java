package com.electro.utils;

import com.electro.entity.inventory.Docket;
import com.electro.entity.inventory.DocketVariant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class InventoryUtilsTest {

    /**
     * Test ID: 5
     * Test case: Tính toán chỉ số tồn kho với danh sách rỗng
     * 
     * Mục tiêu: 
     * - Kiểm tra hàm trả về tất cả các chỉ số bằng 0 khi không có giao dịch nào
     * - Đảm bảo không có lỗi xảy ra khi xử lý danh sách rỗng
     * 
     * Dữ liệu test:
     * - Danh sách giao dịch rỗng
     * 
     * Kết quả mong muốn:
     * - inventory = 0
     * - waitingForDelivery = 0
     * - canBeSold = 0
     * - areComing = 0
     */
    @Test
    void calculateInventoryIndices_WithEmptyList_ShouldReturnZeroValues() {
        // Arrange
        List<DocketVariant> transactions = new ArrayList<>();

        // Act
        Map<String, Integer> result = InventoryUtils.calculateInventoryIndices(transactions);
        
        // Assert
        assertEquals(0, result.get("inventory"));
        assertEquals(0, result.get("waitingForDelivery"));
        assertEquals(0, result.get("canBeSold"));
        assertEquals(0, result.get("areComing"));
    }

    /**
     * Test ID: 6
     * Test case: Tính toán chỉ số tồn kho với phiếu nhập đã hoàn thành
     * 
     * Mục tiêu:
     * - Kiểm tra hàm tăng tồn kho khi có phiếu nhập hoàn thành
     * - Kiểm tra các chỉ số khác không bị ảnh hưởng
     * 
     * Dữ liệu test:
     * - Phiếu nhập: type = 1 (Import), status = 3 (Completed)
     * - Số lượng: 10 đơn vị
     * 
     * Kết quả mong muốn:
     * - inventory = 10 (tăng theo số lượng nhập)
     * - waitingForDelivery = 0 (không có phiếu xuất)
     * - canBeSold = 10 (bằng tồn kho vì không có đơn chờ)
     * - areComing = 0 (không có phiếu nhập đang xử lý)
     */
    @Test
    void calculateInventoryIndices_WithCompletedImport_ShouldIncreaseInventory() {
        // Arrange
        List<DocketVariant> transactions = new ArrayList<>();
        Docket importDocket = new Docket();
        importDocket.setType(1); // Import
        importDocket.setStatus(3); // Completed
        
        DocketVariant variant = new DocketVariant();
        variant.setDocket(importDocket);
        variant.setQuantity(10);
        transactions.add(variant);

        // Act
        Map<String, Integer> result = InventoryUtils.calculateInventoryIndices(transactions);
        
        // Assert
        assertEquals(10, result.get("inventory"));
        assertEquals(0, result.get("waitingForDelivery"));
        assertEquals(10, result.get("canBeSold"));
        assertEquals(0, result.get("areComing"));
    }

    /**
     * Test ID: 7
     * Test case: Tính toán chỉ số tồn kho với phiếu xuất đã hoàn thành
     * 
     * Mục tiêu:
     * - Kiểm tra hàm giảm tồn kho khi có phiếu xuất hoàn thành
     * - Kiểm tra tồn kho được tính đúng sau khi trừ đi số lượng xuất
     * 
     * Dữ liệu test:
     * - Phiếu nhập: 20 đơn vị (đã hoàn thành)
     * - Phiếu xuất: 5 đơn vị (đã hoàn thành)
     * 
     * Kết quả mong muốn:
     * - inventory = 15 (20 - 5)
     * - waitingForDelivery = 0 (không có phiếu xuất đang chờ)
     * - canBeSold = 15 (bằng tồn kho vì không có đơn chờ)
     * - areComing = 0 (không có phiếu nhập đang xử lý)
     */
    @Test
    void calculateInventoryIndices_WithCompletedExport_ShouldDecreaseInventory() {
        // Arrange
        List<DocketVariant> transactions = new ArrayList<>();
        
        // Thêm phiếu nhập trước để có tồn kho
        Docket importDocket = new Docket();
        importDocket.setType(1);
        importDocket.setStatus(3);
        DocketVariant importVariant = new DocketVariant();
        importVariant.setDocket(importDocket);
        importVariant.setQuantity(20);
        transactions.add(importVariant);

        // Thêm phiếu xuất
        Docket exportDocket = new Docket();
        exportDocket.setType(2); // Export
        exportDocket.setStatus(3); // Completed
        DocketVariant exportVariant = new DocketVariant();
        exportVariant.setDocket(exportDocket);
        exportVariant.setQuantity(5);
        transactions.add(exportVariant);

        // Act
        Map<String, Integer> result = InventoryUtils.calculateInventoryIndices(transactions);
        
        // Assert
        assertEquals(15, result.get("inventory")); // 20 - 5
        assertEquals(0, result.get("waitingForDelivery"));
        assertEquals(15, result.get("canBeSold"));
        assertEquals(0, result.get("areComing"));
    }

    /**
     * Test ID: 8
     * Test case: Tính toán chỉ số tồn kho với phiếu xuất đang chờ xử lý
     * 
     * Mục tiêu:
     * - Kiểm tra hàm tăng số lượng đang chờ giao khi có phiếu xuất đang xử lý
     * - Kiểm tra số lượng có thể bán được giảm đi tương ứng
     * 
     * Dữ liệu test:
     * - Phiếu nhập: 20 đơn vị (đã hoàn thành)
     * - Phiếu xuất: 8 đơn vị (đang chờ xử lý)
     * 
     * Kết quả mong muốn:
     * - inventory = 20 (chưa trừ vì chưa hoàn thành)
     * - waitingForDelivery = 8 (số lượng đang chờ xuất)
     * - canBeSold = 12 (20 - 8)
     * - areComing = 0 (không có phiếu nhập đang xử lý)
     */
    @Test
    void calculateInventoryIndices_WithPendingExport_ShouldIncreaseWaitingForDelivery() {
        // Arrange
        List<DocketVariant> transactions = new ArrayList<>();
        
        // Thêm phiếu nhập trước để có tồn kho
        Docket importDocket = new Docket();
        importDocket.setType(1);
        importDocket.setStatus(3);
        DocketVariant importVariant = new DocketVariant();
        importVariant.setDocket(importDocket);
        importVariant.setQuantity(20);
        transactions.add(importVariant);

        // Thêm phiếu xuất đang chờ
        Docket exportDocket = new Docket();
        exportDocket.setType(2); // Export
        exportDocket.setStatus(1); // New
        DocketVariant exportVariant = new DocketVariant();
        exportVariant.setDocket(exportDocket);
        exportVariant.setQuantity(8);
        transactions.add(exportVariant);

        // Act
        Map<String, Integer> result = InventoryUtils.calculateInventoryIndices(transactions);
        
        // Assert
        assertEquals(20, result.get("inventory")); // Chỉ tính phiếu nhập
        assertEquals(8, result.get("waitingForDelivery")); // Phiếu xuất đang chờ
        assertEquals(12, result.get("canBeSold")); // 20 - 8
        assertEquals(0, result.get("areComing"));
    }

    /**
     * Test ID: 9
     * Test case: Tính toán chỉ số tồn kho với phiếu nhập đang xử lý
     * 
     * Mục tiêu:
     * - Kiểm tra hàm tăng số lượng đang về khi có phiếu nhập đang xử lý
     * - Kiểm tra tồn kho không bị ảnh hưởng bởi phiếu nhập chưa hoàn thành
     * 
     * Dữ liệu test:
     * - Phiếu nhập: 15 đơn vị (đang xử lý)
     * 
     * Kết quả mong muốn:
     * - inventory = 0 (chưa tính vì chưa hoàn thành)
     * - waitingForDelivery = 0 (không có phiếu xuất)
     * - canBeSold = 0 (bằng tồn kho vì không có đơn chờ)
     * - areComing = 15 (số lượng đang về)
     */
    @Test
    void calculateInventoryIndices_WithPendingImport_ShouldIncreaseAreComing() {
        // Arrange
        List<DocketVariant> transactions = new ArrayList<>();
        Docket importDocket = new Docket();
        importDocket.setType(1); // Import
        importDocket.setStatus(2); // Processing
        
        DocketVariant variant = new DocketVariant();
        variant.setDocket(importDocket);
        variant.setQuantity(15);
        transactions.add(variant);

        // Act
        Map<String, Integer> result = InventoryUtils.calculateInventoryIndices(transactions);
        
        // Assert
        assertEquals(0, result.get("inventory")); // Chưa tính vào tồn kho
        assertEquals(0, result.get("waitingForDelivery"));
        assertEquals(0, result.get("canBeSold"));
        assertEquals(15, result.get("areComing")); // Đang về
    }

    /**
     * Test ID: 10
     * Test case: Tính toán chỉ số tồn kho với nhiều giao dịch
     * 
     * Mục tiêu:
     * - Kiểm tra hàm tính toán chính xác với nhiều loại giao dịch khác nhau
     * - Kiểm tra tương tác giữa các loại giao dịch
     * 
     * Dữ liệu test:
     * - Phiếu nhập hoàn thành: 20 đơn vị
     * - Phiếu xuất hoàn thành: 5 đơn vị
     * - Phiếu xuất đang chờ: 8 đơn vị
     * - Phiếu nhập đang xử lý: 10 đơn vị
     * 
     * Kết quả mong muốn:
     * - inventory = 15 (20 - 5)
     * - waitingForDelivery = 8 (số lượng đang chờ xuất)
     * - canBeSold = 7 (15 - 8)
     * - areComing = 10 (số lượng đang về)
     */
    @Test
    void calculateInventoryIndices_WithMultipleTransactions_ShouldCalculateCorrectly() {
        // Arrange
        List<DocketVariant> transactions = new ArrayList<>();
        
        // Nhập hàng đã hoàn thành: 20 đơn vị
        Docket importDocket = new Docket();
        importDocket.setType(1);
        importDocket.setStatus(3);
        DocketVariant importVariant = new DocketVariant();
        importVariant.setDocket(importDocket);
        importVariant.setQuantity(20);
        transactions.add(importVariant);

        // Xuất hàng đã hoàn thành: 5 đơn vị
        Docket exportDocket = new Docket();
        exportDocket.setType(2);
        exportDocket.setStatus(3);
        DocketVariant exportVariant = new DocketVariant();
        exportVariant.setDocket(exportDocket);
        exportVariant.setQuantity(5);
        transactions.add(exportVariant);

        // Xuất hàng đang chờ: 8 đơn vị
        Docket pendingExportDocket = new Docket();
        pendingExportDocket.setType(2);
        pendingExportDocket.setStatus(1);
        DocketVariant pendingExportVariant = new DocketVariant();
        pendingExportVariant.setDocket(pendingExportDocket);
        pendingExportVariant.setQuantity(8);
        transactions.add(pendingExportVariant);

        // Nhập hàng đang xử lý: 10 đơn vị
        Docket pendingImportDocket = new Docket();
        pendingImportDocket.setType(1);
        pendingImportDocket.setStatus(2);
        DocketVariant pendingImportVariant = new DocketVariant();
        pendingImportVariant.setDocket(pendingImportDocket);
        pendingImportVariant.setQuantity(10);
        transactions.add(pendingImportVariant);

        // Act
        Map<String, Integer> result = InventoryUtils.calculateInventoryIndices(transactions);
        
        // Assert
        assertEquals(15, result.get("inventory")); // 20 - 5
        assertEquals(8, result.get("waitingForDelivery")); // 8 đang chờ xuất
        assertEquals(7, result.get("canBeSold")); // 15 - 8
        assertEquals(10, result.get("areComing")); // 10 đang về
    }

    /**
     * Test ID: 11
     * Test case: Tính toán chỉ số tồn kho với phiếu xuất vượt quá tồn kho
     * 
     * Mục tiêu:
     * - Kiểm tra hàm throw exception khi số lượng xuất vượt quá tồn kho
     * - Đảm bảo không cho phép xuất quá số lượng tồn kho
     * 
     * Dữ liệu test:
     * - Phiếu nhập: 10 đơn vị (đã hoàn thành)
     * - Phiếu xuất: 15 đơn vị (đã hoàn thành, vượt quá tồn kho)
     * 
     * Kết quả mong muốn:
     * - Throw IllegalArgumentException
     * - Message: "Số lượng xuất (15) vượt quá tồn kho hiện tại (10)"
     */
    @Test
    void calculateInventoryIndices_WithExcessiveExport_ShouldThrowException() {
        // Arrange
        List<DocketVariant> transactions = new ArrayList<>();
        
        // Nhập hàng: 10 đơn vị
        Docket importDocket = new Docket();
        importDocket.setType(1);
        importDocket.setStatus(3);
        DocketVariant importVariant = new DocketVariant();
        importVariant.setDocket(importDocket);
        importVariant.setQuantity(10);
        transactions.add(importVariant);

        // Xuất hàng: 15 đơn vị (vượt quá tồn kho)
        Docket exportDocket = new Docket();
        exportDocket.setType(2);
        exportDocket.setStatus(3);
        DocketVariant exportVariant = new DocketVariant();
        exportVariant.setDocket(exportDocket);
        exportVariant.setQuantity(15);
        transactions.add(exportVariant);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> InventoryUtils.calculateInventoryIndices(transactions)
        );
        assertEquals("Số lượng xuất (15) vượt quá tồn kho hiện tại (10)", exception.getMessage());
    }

    /**
     * Test ID: 12
     * Test case: Tính toán chỉ số tồn kho với phiếu xuất đang chờ vượt quá tồn kho
     * 
     * Mục tiêu:
     * - Kiểm tra hàm throw exception khi số lượng xuất đang chờ vượt quá tồn kho
     * - Đảm bảo không cho phép tạo phiếu xuất đang chờ vượt quá tồn kho
     * 
     * Dữ liệu test:
     * - Phiếu nhập: 10 đơn vị (đã hoàn thành)
     * - Phiếu xuất đang chờ: 15 đơn vị (vượt quá tồn kho)
     * 
     * Kết quả mong muốn:
     * - Throw IllegalArgumentException
     * - Message: "Số lượng xuất đang chờ (15) vượt quá tồn kho hiện tại (10)"
     */
    @Test
    void calculateInventoryIndices_WithExcessivePendingExport_ShouldThrowException() {
        // Arrange
        List<DocketVariant> transactions = new ArrayList<>();
        
        // Nhập hàng: 10 đơn vị
        Docket importDocket = new Docket();
        importDocket.setType(1);
        importDocket.setStatus(3);
        DocketVariant importVariant = new DocketVariant();
        importVariant.setDocket(importDocket);
        importVariant.setQuantity(10);
        transactions.add(importVariant);

        // Xuất hàng đang chờ: 15 đơn vị (vượt quá tồn kho)
        Docket pendingExportDocket = new Docket();
        pendingExportDocket.setType(2);
        pendingExportDocket.setStatus(1);
        DocketVariant pendingExportVariant = new DocketVariant();
        pendingExportVariant.setDocket(pendingExportDocket);
        pendingExportVariant.setQuantity(15);
        transactions.add(pendingExportVariant);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> InventoryUtils.calculateInventoryIndices(transactions)
        );
        assertEquals("Số lượng xuất đang chờ (15) vượt quá tồn kho hiện tại (10)", exception.getMessage());
    }
} 