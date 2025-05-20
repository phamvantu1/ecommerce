package com.electro.service.inventory;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PurchaseOrderInventoryTest {
    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeMethod
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, 10);
        driver.manage().window().maximize();
    }

    @Test
    public void testCreateInventoryFromUnapprovedPurchaseOrder() {
        // 1. Truy cập trang đăng nhập admin
        driver.get("http://localhost:3000/admin");

        // 2. Đăng nhập
        WebElement usernameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[1]/div/div/div/div/form/div[1]/div/input")));
        WebElement passwordInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/div/div/form/div[2]/div/div[1]/input"));
        WebElement loginButton = driver.findElement(By.xpath("/html/body/div[1]/div/div/div/div/form/button"));

        usernameInput.sendKeys("admin");
        passwordInput.sendKeys("123456");
        loginButton.click();

        // 3. Chọn menu tồn kho
        WebElement inventoryMenu = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='root']/div/div/nav/div/div/div/div[7]/a")));
        inventoryMenu.click();

        // 4. Chọn menu quản lý đơn mua hàng trong menu tồn kho
        WebElement purchaseOrderMenu = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/div/div/nav/div/div/div/div[7]/a[3]")));
        purchaseOrderMenu.click();

        // Đợi bảng dữ liệu hiển thị
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[1]/div/div/main/div/div[3]/div/div/div/table")));

        // 5. Tìm đơn mua hàng có trạng thái khác "Đã duyệt"
        String unapprovedOrderCode = null;
        int maxAttempts = 3;
        int currentAttempt = 0;

        while (unapprovedOrderCode == null && currentAttempt < maxAttempts) {
            try {
                // Tìm tất cả các dòng trong bảng
                List<WebElement> rows = driver.findElements(By.xpath("/html/body/div[1]/div/div/main/div/div[3]/div/div/div/table/tbody/tr"));
                System.out.println("Số dòng tìm thấy: " + rows.size()); // Debug log

                for (WebElement row : rows) {
                    try {
                        // Tìm trạng thái trong dòng hiện tại
                        WebElement statusElement = row.findElement(By.xpath(".//td[9]/div/span"));
                        String status = statusElement.getText();
                        System.out.println("Trạng thái tìm thấy: " + status); // Debug log

                        if (!status.equals("Đã duyệt")) {
                            // Tìm mã đơn mua hàng trong dòng hiện tại
                            WebElement orderCodeElement = row.findElement(By.xpath(".//td[4]/div/span"));
                            unapprovedOrderCode = orderCodeElement.getText();
                            System.out.println("Mã đơn mua hàng tìm thấy: " + unapprovedOrderCode); // Debug log
                            break;
                        }
                    } catch (Exception e) {
                        System.out.println("Lỗi khi xử lý dòng: " + e.getMessage()); // Debug log
                        continue;
                    }
                }
            } catch (Exception e) {
                System.out.println("Lỗi khi tìm bảng: " + e.getMessage()); // Debug log
            }
            
            if (unapprovedOrderCode == null) {
                currentAttempt++;
                try {
                    Thread.sleep(1000); // Đợi 1 giây trước khi thử lại
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        Assert.assertNotNull(unapprovedOrderCode, "Không tìm thấy đơn mua hàng nào có trạng thái khác 'Đã duyệt'");

        // 6. Chuyển đến trang quản lý phiếu nhập xuất kho
        WebElement inventoryReceiptMenu = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/div/div/nav/div/div/div/div[7]/a[5]")));
        inventoryReceiptMenu.click();

        // 7. Bấm nút tạo phiếu nhập
        WebElement createReceiptButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/div/div/main/div/div[1]/div[2]/a")));
        createReceiptButton.click();

        // 8. Chọn loại phiếu nhập (dropdown)
        WebElement receiptTypeSelect = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/div/div/main/div/div[3]/div[2]/form/div/div/div[1]/div[1]/div/div/div/input")));
        receiptTypeSelect.click();

        // Đợi dropdown hiển thị và chọn option "Nhập"
        try {
            Thread.sleep(1000); // Đợi dropdown hiển thị
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Tìm tất cả các div có class chứa 'mantine-Select-item'
        List<WebElement> allElements = driver.findElements(By.xpath("//*[contains(@class, 'mantine-Select-item')]"));
        System.out.println("Số lượng elements tìm thấy: " + allElements.size()); // Debug log

        for (WebElement element : allElements) {
            System.out.println("Element text: " + element.getText()); // Debug log
            if (element.getText().trim().equals("Nhập")) {
                element.click();
                break;
            }
        }

        // 9. Chọn loại đơn mua hàng (dropdown)
        WebElement purchaseOrderTypeSelect = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/div/div/main/div/div[3]/div[2]/form/div/div/div[1]/div[5]/div/div/div/input")));
        purchaseOrderTypeSelect.click();

        try {
            Thread.sleep(1000); // Đợi dropdown hiển thị
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Tìm tất cả các div có class chứa 'mantine-Select-item'
        List<WebElement> orderElements = driver.findElements(By.xpath("//*[contains(@class, 'mantine-Select-item')]"));
        boolean foundUnapprovedOrder = false;
        
        for (WebElement element : orderElements) {
            System.out.println("Order element text: " + element.getText()); // Debug log
            if (element.getText().contains(unapprovedOrderCode)) {
                foundUnapprovedOrder = true;
                break;
            }
        }
        
        // Test sẽ fail nếu tìm thấy đơn mua hàng chưa duyệt trong dropdown
        Assert.assertFalse(foundUnapprovedOrder, 
            "Đã tìm thấy đơn mua hàng chưa duyệt trong dropdown. Mã đơn: " + unapprovedOrderCode);
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
} 