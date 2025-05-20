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

public class SeleInventoryTest {
    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeMethod
    public void setUp() {
        // Cấu hình WebDriverManager để tự động tải ChromeDriver
        WebDriverManager.chromedriver().setup();

        // Khởi tạo ChromeDriver
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, 10);
        driver.manage().window().maximize();
    }

    @Test
    public void testViewInventoryTransactions() {
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

        // 4. Bấm nút giao dịch
        WebElement transactionButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='root']/div/div/main/div/div[2]/div/div/div/table/tbody/tr[1]/td[10]/a")));
        transactionButton.click();

        // 5. Kiểm tra bảng giao dịch hiển thị
        WebElement transactionTable = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[3]/div/div/div/div[1]")));
        Assert.assertTrue(transactionTable.isDisplayed(), "Bảng giao dịch không hiển thị");

        // 6. Kiểm tra các cột cần thiết trong bảng
        List<WebElement> tableHeaders = driver.findElements(By.xpath("/html/body/div[3]/div/div/div/div[1]/div[2]/table/thead/tr/th"));
        String[] expectedHeaders = {"Phiếu", "Ngày tạo", "Lý do", "Mã đơn nhập hàng", "Mã đơn hàng", "Số lượng", "SKU", "Kho", "Trạng thái"};

        // In ra số lượng header tìm thấy để debug
        System.out.println("Số lượng header tìm thấy: " + tableHeaders.size());
        for (WebElement header : tableHeaders) {
            System.out.println("Header text: " + header.getText());
        }

        for (int i = 0; i < expectedHeaders.length; i++) {
            Assert.assertTrue(tableHeaders.get(i).getText().contains(expectedHeaders[i]),
                    "Không tìm thấy cột " + expectedHeaders[i]);
        }
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}